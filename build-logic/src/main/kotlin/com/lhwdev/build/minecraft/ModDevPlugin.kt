@file:Suppress("UnstableApiUsage")

package com.lhwdev.build.minecraft

import net.neoforged.moddevgradle.legacyforge.dsl.LegacyForgeExtension
import net.neoforged.moddevgradle.legacyforge.internal.LegacyForgeModDevPlugin
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.kotlin.dsl.getByName
import javax.inject.Inject


open class ModDevPlugin @Inject constructor(objectFactory: ObjectFactory) : LegacyForgeModDevPlugin(objectFactory) {
	override fun apply(project: Project) {
		super.apply(project)
		
		val previous = project.extensions.getByName<LegacyForgeExtension>("legacyForge")
		val forge = project.extensions.create(
			"neoForge",
			OpenForgeExtension::class.java,
			previous.accessTransformers,
			previous.interfaceInjectionData,
		)
		forge.modDevPluginType = this::class.java
	}
}
