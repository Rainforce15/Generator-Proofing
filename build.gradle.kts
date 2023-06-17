plugins {
	id("java")
	id("org.jetbrains.kotlin.jvm") version "1.8.21"
	id("org.jetbrains.intellij") version "1.13.3"
}

group = "com.aeolid"
version = "1.3.1"

repositories {
	mavenCentral()
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
	version.set("2023.1")
	type.set("IC") // Target IDE Platform

	plugins.set(listOf("com.intellij.java"))
}

tasks {
	// Set the JVM compatibility versions
	withType<JavaCompile> {
		sourceCompatibility = "17"
		targetCompatibility = "17"
	}
	withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
		kotlinOptions.jvmTarget = "17"
	}

	patchPluginXml {
		version.set("${project.version}")
		sinceBuild.set("231")
		untilBuild.set("242.*")
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
