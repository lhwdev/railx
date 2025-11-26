@file:Suppress("UnstableApiUsage")

package com.lhwdev.build.minecraft

import net.neoforged.moddevgradle.dsl.DataFileCollection
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeModdingSettings
import net.neoforged.moddevgradle.legacyforge.internal.LegacyForgeModDevPlugin
import org.gradle.api.Action
import org.gradle.api.Project
import javax.inject.Inject


abstract class OpenForgeExtension @Inject constructor(
	protected val project: Project,
	accessTransformers: DataFileCollection,
	interfaceInjectionData: DataFileCollection,
) : LegacyForgeExtension(project, accessTransformers, interfaceInjectionData) {
	lateinit var modDevPluginType: Class<out LegacyForgeModDevPlugin>
	
	override fun enable(customizer: Action<LegacyForgeModdingSettings>) {
		val modDevPlugin = project.plugins.getPlugin(modDevPluginType)
		
		val settings = project.objects.newInstance(LegacyForgeModdingSettings::class.java)
		customizer.execute(settings)
		
		modDevPlugin.enable(project, settings, this)
	}
}
