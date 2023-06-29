plugins {
	id("java")
	id("org.jetbrains.kotlin.jvm") version "1.8.21"
	id("org.jetbrains.intellij") version "1.13.3"
}

group = "com.aeolid"
version = "1.2.5"

repositories {
	mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
	version.set("2021.1")
	type.set("IC") // Target IDE Platform

	plugins.set(listOf("com.intellij.java"))
}

tasks {
	// Set the JVM compatibility versions
	withType<JavaCompile> {
		sourceCompatibility = "11"
		targetCompatibility = "11"
	}
	withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
		kotlinOptions.jvmTarget = "11"
	}

	patchPluginXml {
		version.set("${project.version}")
		sinceBuild.set("211")
		untilBuild.set("223.*")
	}

	signPlugin {
		certificateChainFile.set(file("../jbMarketData/chain.crt"))
		privateKeyFile.set(file("../jbMarketData/private.pem"))
		password.set(providers.environmentVariable("PRIVATE_KEY_PASSWORD"))
	}

	publishPlugin {
		token.set(providers.environmentVariable("PUBLISH_TOKEN"))
	}
}
