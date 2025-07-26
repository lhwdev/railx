import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

plugins {
	kotlin("jvm") version "2.0.0" apply false
}

subprojects {
	repositories {
		mavenCentral()
	}
	
	extensions.apply {
		configureIf<JavaPluginExtension>("java") {
			toolchain.languageVersion = JavaLanguageVersion.of(21)
		}
		configureIf<KotlinJvmProjectExtension>("kotlin") {
			jvmToolchain(21)
		}
	}
}

inline fun <reified T> ExtensionContainer.configureIf(name: String, crossinline block: T.() -> Unit) {
	val extension = findByName(name)
	if(extension is T) block(extension)
}