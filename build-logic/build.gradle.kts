plugins {
	`kotlin-dsl`
}

repositories {
	gradlePluginPortal()
}

gradlePlugin {
	plugins {
		// See https://github.com/neoforged/ModDevGradle/blob/main/build.gradle
		register("moddev") {
			id = "railx.neoforged.moddev"
			implementationClass = "com.lhwdev.build.minecraft.ModDevPlugin"
			displayName = "NeoForge Mod Development Plugin"
			description = "This plugin helps you create Minecraft mods using the NeoForge platform"
		}
		
		register("repositories") {
			id = "railx.neoforged.moddev.repositories"
			implementationClass = "com.lhwdev.build.minecraft.RepositoriesPlugin"
			displayName = "NeoForge Mod Development Repositories Plugin"
			description =
				"This plugin adds the repositories needed for developing Minecraft mods. It is applied automatically by the moddev plugin, but can be applied manually in settings.gradle to make use of Gradle dependency management."
		}
		
		register("moddevAsDeps") {
			id = "railx.neoforged.moddev.asDeps"
			implementationClass = "com.lhwdev.build.minecraft.ModDevAsDepsPlugin"
			displayName = "NeoForge Mod Development Plugin Modified"
			description =
				"Assumes using :minecraft module; does not provide extra artifacts. Automatically add dependency to :minecraft project."
		}
	}
}

dependencies {
	implementation("net.neoforged:moddev-gradle:2.0.95")
	implementation("com.gradleup.shadow:shadow-gradle-plugin:9.2.2")
}
