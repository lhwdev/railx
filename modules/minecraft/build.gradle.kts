import net.neoforged.moddevgradle.internal.ModDevArtifactsWorkflow

plugins {
	id("railx.neoforged.moddev")
}


neoForge {
	enable {
		version = libs.versions.neoForge.get()
		enabledSourceSets = emptySet()
	}
	
	parchment {
		mappingsVersion = "2024.11.17"
		minecraftVersion = libs.versions.minecraft
	}
	
	validateAccessTransformers = true
	accessTransformers {
		file("src/main/resources/META-INF/accesstransformer.cfg")
	}
}

@Suppress("UnstableApiUsage")
extensions.configure<ModDevArtifactsWorkflow>("__internal_modDevArtifactsWorkflow") {
	configurations.apiElements.get().extendsFrom(runtimeDependencies)
	configurations.runtimeElements.get().extendsFrom(compileDependencies)
}
