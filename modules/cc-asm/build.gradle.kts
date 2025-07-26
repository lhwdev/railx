plugins {
	kotlin("jvm")
}

dependencies {
	implementation("org.ow2.asm:asm:9.+")
	
	implementation("org.jetbrains.kotlin:kotlin-reflect:2.0.0")
	
	// 1.21.1-1.115.1 does not exist in squiddev maven
	implementation(files("../../libs/cc-tweaked-1.21.1-forge-1.115.1.jar"))
	compileOnly(files("../../libs/cobalt-0.9.5.jar"))
	// compileOnly("cc.tweaked:cc-tweaked-1.21-core-api:1.115.1")
	// compileOnly("cc.tweaked:cc-tweaked-1.21-forge-api:1.115.1")
	// runtimeOnly("cc.tweaked:cc-tweaked-1.21-forge:1.115.1")
	// compileOnly("org.squiddev:Cobalt:0.9.6")
}
