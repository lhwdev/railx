plugins {
	kotlin("jvm")
	id("railx.neoforged.moddev.asDeps")
}

neoForge {
	enable {}
}

dependencies {
	val v = libs.versions
	
	implementation(projects.minecraft)
	implementation(projects.forge)
	implementation("com.simibubi.create:create-${v.minecraft.get()}:${v.create.get()}") {
		isTransitive = false
	}
	
	implementation(kotlin("reflect"))
}
