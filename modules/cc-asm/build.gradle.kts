plugins {
	kotlin("jvm")
}

dependencies {
	implementation("org.ow2.asm:asm:9.+")
	implementation("org.ow2.asm:asm-util:9.+")
	
	implementation(kotlin("reflect"))
	
	compileOnly("cc.tweaked:cc-tweaked-1.20.1-core-api:1.116.2")
	compileOnly("cc.tweaked:cc-tweaked-1.20.1-forge-api:1.116.2")
	runtimeOnly("cc.tweaked:cc-tweaked-1.20.1-forge:1.116.2")
	compileOnly("cc.tweaked:cobalt:0.9.7")
}
