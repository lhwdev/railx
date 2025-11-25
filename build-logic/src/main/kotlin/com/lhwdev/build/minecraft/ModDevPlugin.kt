@file:Suppress("UnstableApiUsage")

package com.lhwdev.build.minecraft

import net.neoforged.minecraftdependencies.MinecraftDependenciesPlugin
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.internal.DataFileCollections
import net.neoforged.moddevgradle.internal.ModDevPlugin
import net.neoforged.moddevgradle.internal.jarjar.JarJarPlugin
import net.neoforged.nfrtgradle.NeoFormRuntimePlugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.slf4j.LoggerFactory


open class ModDevPlugin : ModDevPlugin() {
	companion object {
		internal val LOG = LoggerFactory.getLogger(com.lhwdev.build.minecraft.ModDevPlugin::class.java)
	}
	
	override fun apply(project: Project) {
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
}
