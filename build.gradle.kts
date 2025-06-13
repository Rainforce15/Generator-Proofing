import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
	kotlin("jvm") version "2.1.10"
	id("org.jetbrains.intellij") version "1.13.3"
}

group = "com.aeolid"
version = "1.4.2"

repositories {
	mavenCentral()
}

intellij {
	version.set("2025.1")
	type.set("IC")
	plugins.set(listOf("com.intellij.java"))
}

tasks {
	withType<JavaCompile> {
		sourceCompatibility = JvmTarget.JVM_17.target
		targetCompatibility = JvmTarget.JVM_17.target
	}

	withType<KotlinCompile> {
		compilerOptions {
			jvmTarget.set(JvmTarget.JVM_17)
		}
	}

	patchPluginXml {
		version.set("${project.version}")
		sinceBuild.set("251")
		untilBuild.set("273.*")
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
