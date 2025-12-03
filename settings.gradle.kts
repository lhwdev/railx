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
			forRepository {
				maven(url = "https://api.modrinth.com/maven") { name = "Modrinth Maven" }
			}
			filter { includeGroup("maven.modrinth") }
		}
		
		maven(url = "https://thedarkcolour.github.io/KotlinForForge/") {
			name = "Kotlin for Forge Maven"
			content { includeGroup("thedarkcolour") }
		}
		
		maven(url = "https://maven.createmod.net/") {
			name = "Create Maven"
			content {
				includeGroup("com.simibubi.create")
				includeGroup("dev.engine-room.flywheel")
				includeGroup("net.createmod.catnip")
				includeGroup("net.createmod.ponder")
			}
		}
		
		maven(url = "https://raw.githubusercontent.com/Fuzss/modresources/main/maven/") {
			name = "Fuzss Maven for ForgeConfigApiPort"
			content { includeGroup("fuzs.forgeconfigapiport") }
		}
		
		maven(url = "https://mvn.devos.one/snapshots") {
			name = "Maven for Registrate"
			content {
				includeGroup("com.tterrag.registrate")
			}
		}
		
		maven(url = "https://maven.squiddev.cc") {
			name = "SquidDev Maven for CC: Tweaked"
			content {
				includeGroup("cc.tweaked")
			}
		}
		
		maven(url = "https://maven.enginehub.org/repo/") {
			name = "EngineHub Maven for WorldEdit"
			content { includeGroupAndSubgroups("com.sk89q") }
		}
		
		maven(url = "https://repo.spongepowered.org/repository/maven-public/") {
			name = "Sponge Maven"
			content {
				includeGroup("org.spongepowered")
			}
		}
	}
}

val projects = listOf(
	":minecraft",
	":utils",
	":cc-asm", ":api",
	":forge"
)

projects.forEach { name ->
	include(name)
	val project = project(name)
	project.projectDir = file("modules${name.replace(":", "/")}")
}
