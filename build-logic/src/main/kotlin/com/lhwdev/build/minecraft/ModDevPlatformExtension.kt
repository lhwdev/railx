package com.lhwdev.build.minecraft

import com.github.jengelman.gradle.plugins.shadow.tasks.DependencyFilter
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import net.neoforged.moddevgradle.dsl.InternalModelHelper
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.internal.utils.ExtensionUtils
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import org.gradle.api.DefaultTask
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.Provider
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.attributes
import org.gradle.kotlin.dsl.getByName
import java.io.Serializable
import java.util.*
import javax.inject.Inject


@Suppress("UnstableApiUsage")
open class ModDevPlatformExtension @Inject constructor(private val project: Project) {
	companion object {
		const val Name: String = "modDevPlatform"
	}
	
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
			
			manifest.attributes(
				"FMLModType" to "LIBRARY",
			)
		}
		
		tasks.named("classes") { dependsOn(modDevRuntimeStandaloneJar) }
		
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
			}
		}
		
		// add to legacyClasspath
		project.afterEvaluate {
			configurations.named("additionalRuntimeClasspath") {
				val standaloneJar = modDevRuntimeStandaloneJar.map { it.outputs.files }
				dependencies.add(project.dependencyFactory.create(project.files(standaloneJar)))
			}
			
			// IDK why task dependency is not added from configuration
			val neoForge = project.extensions.getByName<LegacyForgeExtension>("legacyForge")
			neoForge.runs.configureEach {
				tasks.named(InternalModelHelper.nameOfRun(this, "write", "legacyClasspath")) {
					dependsOn(modDevRuntimeStandaloneJar)
				}
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
