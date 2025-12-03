plugins {
	kotlin("jvm")
	id("railx.neoforged.moddev.asDeps")
}

neoForge {
	enable {}
}

dependencies {
	implementation(projects.minecraft)
}

