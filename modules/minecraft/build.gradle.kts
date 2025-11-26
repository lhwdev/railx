import net.neoforged.moddevgradle.internal.ModDevArtifactsWorkflow

plugins {
	id("railx.neoforged.moddev")
}


neoForge {
	enable {
		forgeVersion = "${libs.versions.minecraft.get()}-${libs.versions.forge.get()}"
		enabledSourceSets = emptySet()
	}
	
	parchment {
		mappingsVersion = "2023.09.03"
		minecraftVersion = libs.versions.minecraft
	}
	
	validateAccessTransformers = true
	accessTransformers {
		from("src/main/resources/accesstransformer.cfg")
	}
}

@Suppress("UnstableApiUsage")
extensions.configure<ModDevArtifactsWorkflow>("__internal_modDevArtifactsWorkflow") {
	configurations.apiElements.get().extendsFrom(runtimeDependencies)
	configurations.runtimeElements.get().extendsFrom(compileDependencies)
}
