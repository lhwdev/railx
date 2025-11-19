@file:Suppress("UnstableApiUsage")

package com.lhwdev.build.minecraft

import net.neoforged.minecraftdependencies.MinecraftDependenciesPlugin
import net.neoforged.moddevgradle.dsl.ModDevExtension
import net.neoforged.moddevgradle.dsl.ModdingVersionSettings
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.internal.*
import net.neoforged.moddevgradle.internal.jarjar.JarJarPlugin
import net.neoforged.nfrtgradle.NeoFormRuntimePlugin
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project
import org.slf4j.LoggerFactory


private const val MinecraftProject = ":minecraft"

open class ModDevAsDepsPlugin : ModDevPlugin() {
	companion object {
		private val LOG = LoggerFactory.getLogger(ModDevPlugin::class.java)
	}
	
	override fun apply(project: Project) {
		project.evaluationDependsOn(MinecraftProject)
		project.dependencies {
			add("implementation", project(MinecraftProject))
		}
		
		project.plugins.apply(JavaLibraryPlugin::class.java)
		project.plugins.apply(NeoFormRuntimePlugin::class.java)
		project.plugins.apply(MinecraftDependenciesPlugin::class.java)
		project.plugins.apply(JarJarPlugin::class.java)
		
		// Do not apply the repositories automatically if they have been applied at the settings-level.
		// It's still possible to apply them manually, though.
		if(!project.gradle.plugins.hasPlugin(RepositoriesPlugin::class.java)) {
			project.plugins.apply(RepositoriesPlugin::class.java)
		} else {
			LOG.info("Not enabling NeoForged repositories since they were applied at the settings level")
		}
		
		val dataFileCollections = DataFileCollections.create(project)
		val neoForge = project.extensions.create(
			NeoForgeExtension.NAME,
			OpenNeoForgeExtension::class.java,
			dataFileCollections.accessTransformers().extension(),
			dataFileCollections.interfaceInjectionData().extension(),
		)
		neoForge.modDevPluginType = this::class.java
	}
	
	override fun enable(project: Project, settings: ModdingVersionSettings, extension: ModDevExtension) {
		val neoForgeVersion = settings.version
		val neoFormVersion = settings.neoFormVersion
		if(neoForgeVersion != null || neoFormVersion != null) {
			throw InvalidUserCodeException("You must not specify at least a NeoForge or a NeoForm version for as deps plugin")
		}
		
		val minecraftProject = project.project(MinecraftProject)
		var artifacts = minecraftProject.extensions.getByName("__internal_modDevArtifactsWorkflow")
			as ModDevArtifactsWorkflow
		artifacts = ModDevArtifactsWorkflow(
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
		ModDevRunWorkflow.create(project, Branding.MDG, artifacts, extension.runs)
	}
}
