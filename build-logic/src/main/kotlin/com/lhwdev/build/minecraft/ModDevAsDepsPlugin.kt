@file:Suppress("UnstableApiUsage")

package com.lhwdev.build.minecraft

import net.neoforged.moddevgradle.internal.Branding
import net.neoforged.moddevgradle.internal.ModDevArtifactsWorkflow
import net.neoforged.moddevgradle.internal.ModDevRunWorkflow
import net.neoforged.moddevgradle.internal.utils.ExtensionUtils
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeModdingSettings
import net.neoforged.moddevgradle.legacyforge.dsl.MixinExtension
import net.neoforged.moddevgradle.legacyforge.dsl.ObfuscationExtension
import net.neoforged.moddevgradle.legacyforge.internal.LegacyForgeModDevPlugin
import net.neoforged.moddevgradle.legacyforge.internal.MinecraftMappings
import net.neoforged.moddevgradle.legacyforge.tasks.RemapOperation
import org.gradle.api.Project
import org.gradle.api.artifacts.transform.*
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.file.RegularFile
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.*
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.getByName
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.process.ExecOperations
import java.lang.invoke.MethodHandles
import javax.inject.Inject


private const val MinecraftProject = ":minecraft"

open class ModDevAsDepsPlugin @Inject constructor(objectFactory: ObjectFactory) : ModDevPlugin(objectFactory) {
	override fun apply(project: Project) {
		project.evaluationDependsOn(MinecraftProject)
		super.apply(project)
	}
	
	override fun enable(project: Project, settings: LegacyForgeModdingSettings, extension: LegacyForgeExtension) {
		val minecraftProject = project.project(MinecraftProject)
		val configurations = project.configurations
		val artifacts = minecraftProject.extensions
			.getByName<ModDevArtifactsWorkflow>("__internal_modDevArtifactsWorkflow")
		for(sourceSets in settings.enabledSourceSets) {
			configurations.getByName(sourceSets.runtimeClasspathConfigurationName)
				.extendsFrom(artifacts.runtimeDependencies)
			configurations.getByName(sourceSets.compileClasspathConfigurationName)
				.extendsFrom(artifacts.compileDependencies)
		}
		
		val artifactsForRun = ModDevArtifactsWorkflow(
			artifacts.project,
			artifacts.dependencies,
			artifacts.versionCapabilities,
			artifacts.createArtifacts,
			artifacts.minecraftClassesDependency,
			artifacts.downloadAssets,
			artifacts.runtimeDependencies,
			artifacts.compileDependencies,
			project.layout.buildDirectory.dir("moddev"),
			artifacts.artifactsBuildDir,
		)
		
		ModDevRunWorkflow.create(project, Branding.MDG, artifactsForRun, extension.runs)
		
		val mixin = ExtensionUtils.getExtension(project, "mixin", MixinExtension::class.java)
			as MixinExtension
		
		val obf = ExtensionUtils.getExtension(project, "obfuscation", ObfuscationExtension::class.java)
			as ObfuscationExtension
		
		val namedToIntermediate = artifacts.getAdditionalMinecraftArtifact("namedToIntermediaryMapping")
		obf.namedToSrgMappings.set(namedToIntermediate)
		
		val intermediateToNamed = artifacts.getAdditionalMinecraftArtifact("intermediaryToNamedMapping")
		
		val mappingsCsv = artifacts.getAdditionalMinecraftArtifact("csvMapping")
		obf.srgToNamedMappings.set(mappingsCsv)
		
		extension.runs.configureEach {
			jvmArguments.addAll(
				"--add-exports",
				"cpw.mods.bootstraplauncher/cpw.mods.bootstraplauncher=ALL-UNNAMED"
			)
			systemProperties.put("mixin.env.remapRefMap", "true")
			systemProperties.put(
				"mixin.env.refMapRemappingFile",
				intermediateToNamed.map { f -> f.asFile.absolutePath }
			)
			programArguments.addAll(mixin.configs.map { cfgs -> cfgs.flatMap { listOf("--mixin.config", it) } })
		}
		val reobfJar = obf.reobfuscate(
			project.tasks.named<Jar>("jar"),
			project.extensions.getByType<SourceSetContainer>().getByName("main"),
		)
		project.tasks.named("assemble") { dependsOn(reobfJar) }
		
		// artifacts.runtimeDependencies.dependencies.add(project.dependencyFactory.create(project.files(mappingsCsv)))
		
		val remapDeps = project.configurations.create("remappingDependencies") {
			description = "An internal configuration that contains the Minecraft dependencies, used for remapping mods"
			isCanBeConsumed = false
			isCanBeDeclared = false
			isCanBeResolved = true
			extendsFrom(artifacts.runtimeDependencies)
		}
		
		project.dependencies.registerTransform(RemappingTransform::class.java) {
			parameters {
				obf.configureSrgToNamedOperation(remapOperation)
				minecraftDependencies.from(remapDeps)
			}
			
			from.attribute(
				MinecraftMappings.ATTRIBUTE,
				SrgMappings.invokeExact(this@ModDevAsDepsPlugin as LegacyForgeModDevPlugin) as MinecraftMappings
			)
			from.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
			
			to.attribute(
				MinecraftMappings.ATTRIBUTE,
				NamedMappings.invokeExact(this@ModDevAsDepsPlugin as LegacyForgeModDevPlugin) as MinecraftMappings
			)
			to.attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "jar")
		}
	}
	
	companion object {
		private val lookup = MethodHandles.lookup()
		private val SrgMappings = LegacyForgeModDevPlugin::class.java.getDeclaredField("srgMappings")
			.also { it.isAccessible = true }
			.let { lookup.unreflectGetter(it) }
		private val NamedMappings = LegacyForgeModDevPlugin::class.java.getDeclaredField("namedMappings")
			.also { it.isAccessible = true }
			.let { lookup.unreflectGetter(it) }
	}
}


internal abstract class RemappingTransform @Inject constructor() : TransformAction<RemappingTransform.Parameters> {
	abstract val inputArtifact: Provider<FileSystemLocation>
		@PathSensitive(PathSensitivity.NONE) @InputArtifact get
	
	abstract val dependencies: FileCollection
		@InputArtifactDependencies @CompileClasspath get
	
	protected abstract val execOperations: ExecOperations
		@Inject get
	
	override fun transform(outputs: TransformOutputs) {
		val inputFile = this.inputArtifact.get().asFile
		if(inputFile.exists()) {
			val mappedFile = outputs.file(inputFile.name)
			
			this.parameters.remapOperation.execute(
				this.execOperations,
				inputFile,
				mappedFile,
				this.dependencies + this.parameters.minecraftDependencies,
			)
		}
	}
	
	interface Parameters : TransformParameters {
		val remapOperation: RemapOperation
			@Nested get
		
		val minecraftDependencies: ConfigurableFileCollection
			@PathSensitive(PathSensitivity.NONE) @InputFiles get
	}
}

private fun ModDevArtifactsWorkflow.getAdditionalMinecraftArtifact(id: String): Provider<RegularFile> =
	project.layout.file(createArtifacts.flatMap { it.additionalResults.getting(id) })
