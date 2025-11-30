@file:Suppress("UnstableApiUsage")

package com.lhwdev.build.minecraft

import net.neoforged.moddevgradle.dsl.DataFileCollection
import net.neoforged.moddevgradle.internal.utils.ExtensionUtils
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeModdingSettings
import net.neoforged.moddevgradle.legacyforge.internal.LegacyForgeModDevPlugin
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectProvider
import org.gradle.api.Project
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.SourceSet
import org.gradle.kotlin.dsl.extendsFrom
import org.gradle.kotlin.dsl.invoke
import javax.inject.Inject
import kotlin.text.get


abstract class OpenForgeExtension @Inject constructor(
	protected val project: Project,
	accessTransformers: DataFileCollection,
	interfaceInjectionData: DataFileCollection,
) : LegacyForgeExtension(project, accessTransformers, interfaceInjectionData) {
	internal lateinit var modDevPluginType: Class<out LegacyForgeModDevPlugin>
	
	override fun enable(customizer: Action<LegacyForgeModdingSettings>) {
		val modDevPlugin = project.plugins.getPlugin(modDevPluginType)
		
		val settings = project.objects.newInstance(LegacyForgeModdingSettings::class.java)
		customizer.execute(settings)
		
		modDevPlugin.enable(project, settings, this)
	}
}
