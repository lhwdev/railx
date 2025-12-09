package com.lhwdev.build.minecraft

import org.gradle.api.Plugin
import org.gradle.api.Project


class ModDevPlatformPlugin : Plugin<Project> {
	override fun apply(project: Project) {
		project.extensions.create("modDevPlatform", ModDevPlatformExtension::class.java)
	}
}
