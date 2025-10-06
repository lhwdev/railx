rootProject.name = "railx"

pluginManagement {
	repositories {
		gradlePluginPortal()
		maven(url = "https://maven.neoforged.net/releases")
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

val projects = listOf(":forge", ":cc-asm", ":test")

projects.forEach { name ->
	include(name)
	project(name).projectDir = file("modules${name.replace(":", "/")}")
}
