@file:Suppress("UnstableApiUsage")

package com.lhwdev.build.minecraft

import net.neoforged.moddevgradle.dsl.ModDevExtension
import net.neoforged.moddevgradle.dsl.ModdingVersionSettings
import net.neoforged.moddevgradle.internal.Branding
import net.neoforged.moddevgradle.internal.ModDevArtifactsWorkflow
import net.neoforged.moddevgradle.internal.ModDevRunWorkflow
import org.gradle.api.InvalidUserCodeException
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project


private const val MinecraftProject = ":minecraft"

open class ModDevAsDepsPlugin : ModDevPlugin() {
	override fun apply(project: Project) {
		project.evaluationDependsOn(MinecraftProject)
		project.dependencies {
			add("implementation", project(MinecraftProject))
		}
		
		super.apply(project)
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
