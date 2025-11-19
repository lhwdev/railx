package com.lhwdev.build.minecraft

import net.neoforged.moddevgradle.dsl.DataFileCollection
import net.neoforged.moddevgradle.dsl.ModdingVersionSettings
import net.neoforged.moddevgradle.dsl.NeoForgeExtension
import net.neoforged.moddevgradle.internal.ModDevPlugin
import org.gradle.api.Action
import org.gradle.api.Project
import javax.inject.Inject


abstract class OpenNeoForgeExtension @Inject constructor(
	protected val project: Project,
	accessTransformers: DataFileCollection,
	interfaceInjectionData: DataFileCollection,
) : NeoForgeExtension(project, accessTransformers, interfaceInjectionData) {
	lateinit var modDevPluginType: Class<out ModDevPlugin>
	
	override fun enable(customizer: Action<ModdingVersionSettings>) {
		val modDevPlugin = project.plugins.getPlugin(modDevPluginType)
		
		val settings = project.objects.newInstance(ModdingVersionSettings::class.java)
		customizer.execute(settings)
		
		modDevPlugin.enable(project, settings, this)
	}
}
