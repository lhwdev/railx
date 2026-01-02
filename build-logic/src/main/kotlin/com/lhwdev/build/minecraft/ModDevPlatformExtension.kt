package com.lhwdev.build.minecraft

import com.github.jengelman.gradle.plugins.shadow.tasks.DependencyFilter
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.neoforged.moddevgradle.dsl.InternalModelHelper
import net.neoforged.moddevgradle.internal.utils.ExtensionUtils
import net.neoforged.moddevgradle.legacyforge.dsl.ObfuscationExtension
import net.neoforged.moddevgradle.legacyforge.internal.MinecraftMappings
import net.neoforged.moddevgradle.legacyforge.tasks.RemapOperation
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.*
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.*
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.gradle.internal.extensions.stdlib.capitalized
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.register
import org.gradle.process.ExecOperations
import java.io.Serializable
import java.util.*
import javax.inject.Inject


@Suppress("UnstableApiUsage")
open class ModDevPlatformExtension @Inject constructor(private val project: Project) {
	companion object {
		const val Name: String = "modDevPlatform"
	}
	
	val prepareRunTask: TaskProvider<DefaultTask>
	
	val modDevRuntimeStandalone: NamedDomainObjectProvider<SourceSet>
	val modDevRuntimeStandaloneJar: TaskProvider<Jar>
	val modDevRuntime: NamedDomainObjectProvider<SourceSet>
	val modDevRuntimeMods: NamedDomainObjectProvider<Configuration>
	
	val projectDependencies: FileCollection
	private var writeProjectDependencies: TaskProvider<WriteProjectDependencies>? = null
	
	// private var mainJarTaskName = "jar"
	
	
	init {
		val configurations = project.configurations
		val tasks = project.tasks
		val sourceSets = ExtensionUtils.getSourceSets(project)
		
		prepareRunTask = tasks.register<DefaultTask>("prepareRun") {}
		project.afterEvaluate {
			val neoForge = project.extensions.getByName<OpenForgeExtension>("neoForge")
			neoForge.runs.configureEach {
				tasks.named(InternalModelHelper.nameOfRun(this, "prepare", "run")) {
					dependsOn(prepareRunTask)
				}
			}
		}
		
		modDevRuntimeMods = configurations.register("modDevRuntimeMods") {
			isCanBeConsumed = false
			isCanBeResolved = false
		}
		
		val mainTask = sourceSets.named("main")
		modDevRuntimeStandalone = sourceSets.register("modDevRuntimeStandalone") {
			val main = mainTask.get()
			
			configurations.named(compileOnlyConfigurationName) {
				extendsFrom(configurations.getByName(main.compileClasspathConfigurationName))
			}
			
			configurations.named(runtimeOnlyConfigurationName) {
				extendsFrom(configurations.getByName(main.runtimeClasspathConfigurationName))
			}
		}
		
		modDevRuntimeStandaloneJar = tasks.register(modDevRuntimeStandalone.get().jarTaskName, Jar::class.java) {
			description = "Assembles a jar archive containing the classes of the '${modDevRuntimeStandalone.name}'."
			group = BasePlugin.BUILD_GROUP
			
			from(modDevRuntimeStandalone.get().output)
			destinationDirectory.set(project.layout.buildDirectory.dir("moddevStandalone"))
			archiveBaseName.set("railx-standalone")
		}
		
		prepareRunTask.configure { dependsOn(modDevRuntimeStandaloneJar) }
		
		modDevRuntime = sourceSets.register("modDevRuntime") {
			val main = mainTask.get()
			
			configurations.named(compileOnlyConfigurationName) {
				extendsFrom(configurations.getByName(main.compileClasspathConfigurationName))
				
				dependencies.add(project.dependencyFactory.create(main.output))
				val standalone = project.dependencyFactory.create(modDevRuntimeStandalone.get().compileClasspath)
				dependencies.add(standalone)
			}
			
			configurations.named(runtimeOnlyConfigurationName) {
				extendsFrom(configurations.getByName(main.runtimeClasspathConfigurationName))
				extendsFrom(modDevRuntimeMods.get())
				
				dependencies.add(project.dependencyFactory.create(main.output))
				val standalone = modDevRuntimeStandaloneJar.map { it.outputs.files }
					.let { project.files(it) }
				dependencies.add(project.dependencyFactory.create(standalone))
			}
		}
		
		projectDependencies = configurations
			.named(modDevRuntime.get().runtimeClasspathConfigurationName)
			.map { c -> c.incoming.artifactView { componentFilter { it is ProjectComponentIdentifier } }.files }
			.let { project.files(it) }
	}
	
	fun writeProjectDependencies(): TaskProvider<WriteProjectDependencies> {
		writeProjectDependencies?.let { return it }
		
		return project.tasks.register("writeProjectDependencies", WriteProjectDependencies::class.java) {
			files.from(projectDependencies)
		}
			.also { writeProjectDependencies = it }
	}
	
	val projectDependenciesFile: Provider<RegularFile>
		get() = (writeProjectDependencies
			?: throw IllegalStateException("get after calling writeProjectDependencies()"))
			.flatMap { it.output }
	
	fun configureShadow(task: TaskProvider<ShadowJar>) {
		// mainJarTaskName = task.name
		task.configure { dependencyFilter.set(FileDependencyFilter(project)) }
	}
	
	fun createRuntimeModConfiguration(parent: Configuration): Configuration {
		val obfuscation = project.extensions.getByName<ObfuscationExtension>("obfuscation")
		val namedMappings = ObfuscationExtension::class.java.getDeclaredField("namedMappings")
			.also { it.isAccessible = true }
			.get(obfuscation) as MinecraftMappings
		
		val remapJarFiles = project.tasks.register<RemapJars>("${parent.name}RemapJarFiles") {
			obfuscation.configureSrgToNamedOperation(remapOperation)
			libraries.from(modDevRuntime.get().compileClasspath)
			include("*.jar")
			into(project.layout.buildDirectory.dir("moddevRuntime/remapped"))
			duplicatesStrategy = DuplicatesStrategy.INCLUDE
		}
		
		val remappingConfig = project.configurations.dependencyScope("mod${parent.name.capitalized()}") {
			description = "Configuration for runtime mod dependencies of ${parent.name} that needs to be remapped"
			isTransitive = false
			withDependencies {
				val iterator = iterator()
				while(iterator.hasNext()) when(val dependency = iterator.next()) {
					is ExternalModuleDependency -> project.dependencies.constraints {
						add(parent.name, "${dependency.group}:${dependency.name}:${dependency.version}") {
							attributes { attribute(MinecraftMappings.ATTRIBUTE, namedMappings) }
						}
					}
					
					
					is ProjectDependency -> project.dependencies.constraints {
						add(parent.name, dependency) {
							attributes { attribute(MinecraftMappings.ATTRIBUTE, namedMappings) }
						}
					}
					
					is FileCollectionDependency -> {
						iterator.remove()
						remapJarFiles.get().from(dependency.files)
					}
				}
			}
		}
		
		parent.extendsFrom(remappingConfig.get())
		
		project.dependencies.add(parent.name, remapJarFiles.map { it.outputs.files.asFileTree })
		prepareRunTask.configure { dependsOn(remapJarFiles) }
		
		return remappingConfig.get()
	}
}


abstract class WriteProjectDependencies : DefaultTask() {
	@get:InputFiles
	abstract val files: ConfigurableFileCollection
	
	@get:OutputFile
	val output: RegularFileProperty = project.objects.fileProperty()
		.convention(project.layout.buildDirectory.file("moddev/project_dependencies.txt"))
	
	
	@TaskAction
	fun write() {
		val writer = output.get().asFile.bufferedWriter()
		try {
			// sorted for consistent result
			for(file in TreeSet(files.files)) {
				writer.write(file.absolutePath)
				writer.write("\n")
			}
			writer.flush()
		} finally {
			writer.close()
		}
	}
}

private class FileDependencyFilter(@Transient private val project: Project) : DependencyFilter, Serializable {
	override fun resolve(configuration: Configuration): FileCollection =
		project.extensions.getByName<ModDevPlatformExtension>(ModDevPlatformExtension.Name)
			.projectDependencies
	
	override fun resolve(configurations: Collection<Configuration>): FileCollection =
		configurations.map { resolve(it) }
			.reduceOrNull { acc, fileCollection -> acc + fileCollection }
			?: project.files()
	
	override fun dependency(dependencyNotation: Any): Spec<ResolvedDependency> = error("no op")
	override fun exclude(spec: Spec<ResolvedDependency>) = error("no op")
	override fun include(spec: Spec<ResolvedDependency>) = error("no op")
	override fun project(notation: Any): Spec<ResolvedDependency> = error("no op")
}


private abstract class RemapJars @Inject constructor(private val execOperations: ExecOperations) : Copy() {
	@get:Nested
	abstract val remapOperation: RemapOperation
	
	@get:InputFiles
	abstract val libraries: ConfigurableFileCollection
	
	
	override fun createCopyAction(): CopyAction = CopyAction { stream ->
		val fileResolver = fileLookup.getFileResolver(destinationDir)
		var didWork = false
		stream.process { details ->
			val target = fileResolver.resolve(details.relativePath.pathString)
			if(target.exists()) { // renameIfCaseChanged
				val canonicalized = target.canonicalFile
				if(target.name != canonicalized.name) canonicalized.renameTo(target)
			}
			
			remapOperation.execute(execOperations, details.file, target, libraries)
			didWork = true
		}
		WorkResults.didWork(didWork)
	}
}
