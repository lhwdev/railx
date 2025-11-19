@file:Suppress("UnstableApiUsage")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "railx"


pluginManagement {
	includeBuild("build-logic")
	
	repositories {
		gradlePluginPortal()
		maven(url = "https://maven.neoforged.net/releases")
	}
}

plugins {
	id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
	id("railx.neoforged.moddev.repositories")
}


dependencyResolutionManagement {
	repositoriesMode = RepositoriesMode.PREFER_SETTINGS
	
	repositories {
		mavenCentral()
		
		exclusiveContent {
			forRepository { maven(url = "https://api.modrinth.com/maven") }
			filter { includeGroup("maven.modrinth") }
		}
		
		maven(url = "https://thedarkcolour.github.io/KotlinForForge/") {
			name = "Kotlin for Forge"
			content { includeGroup("thedarkcolour") }
		}
		
		maven(url = "https://maven.createmod.net/") {
			content {
				includeGroup("com.simibubi.create")
				includeGroup("dev.engine-room.flywheel")
				includeGroup("net.createmod.catnip")
				includeGroup("net.createmod.ponder")
			}
		}
		
		maven(url = "https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") {
			content { includeGroup("fuzs.forgeconfigapiport") }
		}
		
		maven(url = "https://mvn.devos.one/snapshots") {
			content {
				includeGroup("com.tterrag.registrate")
			}
		}
		
		maven(url = "https://squiddev.cc/maven/") {
			content {
				includeGroup("cc.tweaked")
				includeGroup("org.squiddev")
			}
		}
		
		maven(url = "https://maven.enginehub.org/repo/") {
			content { includeGroupAndSubgroups("com.sk89q") }
		}
		
		maven(url = "https://repo.spongepowered.org/repository/maven-public/") {
			name = "Sponge"
			content {
				includeGroup("org.spongepowered")
			}
		}
	}
}

val projects = listOf(
	":minecraft",
	":cc-asm", ":api",
	":forge"
)

projects.forEach { name ->
	include(name)
	val project = project(name)
	project.projectDir = file("modules${name.replace(":", "/")}")
}
