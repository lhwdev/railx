import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
	kotlin("jvm") version libs.versions.kotlin apply false
}

subprojects {
	group = "com.lhwdev.minecraft.railx"
	
	repositories {
		mavenCentral()
	}
	
	afterEvaluate {
		extensions.apply {
			val javaVersion = 21
			
			configureIf<JavaPluginExtension>("java") {
				toolchain.languageVersion = JavaLanguageVersion.of(javaVersion)
			}
			configureIf<KotlinJvmProjectExtension>("kotlin") {
				jvmToolchain(javaVersion)
			}
		}
	}
}

inline fun <reified T> ExtensionContainer.configureIf(name: String, crossinline block: T.() -> Unit) {
	val extension = findByName(name)
	if(extension is T) block(extension)
}
