import java.io.FileInputStream
import java.util.Properties

plugins {
    kotlin("jvm") version "2.0.21"
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21"
    id("org.jetbrains.compose") version "1.7.0"
    application
}

// Load local properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

group = "com.jcraw"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(project(":llm-wrapper"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    testImplementation(kotlin("test"))
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

application {
    mainClass.set("com.jcraw.sophia.MainKt")
}

// Pass API key to application at runtime
tasks.named<JavaExec>("run") {
    val apiKey = localProperties.getProperty("openai.api.key") ?: System.getenv("OPENAI_API_KEY") ?: ""
    environment("OPENAI_API_KEY", apiKey)
}

// Debug task to check API key loading
tasks.register("debugApiKey") {
    doLast {
        val apiKey = localProperties.getProperty("openai.api.key") ?: System.getenv("OPENAI_API_KEY") ?: ""
        println("🔍 Debug API Key:")
        println("   From local.properties: ${localProperties.getProperty("openai.api.key")?.take(10)}...")
        println("   From environment: ${System.getenv("OPENAI_API_KEY")?.take(10) ?: "null"}")
        println("   Final value: ${apiKey.take(10)}... (length: ${apiKey.length})")
        println("   local.properties file exists: ${localPropertiesFile.exists()}")
    }
}

tasks.test {
    useJUnitPlatform()

    // Pass API key to tests
    val apiKey = localProperties.getProperty("openai.api.key") ?: System.getenv("OPENAI_API_KEY") ?: ""
    environment("OPENAI_API_KEY", apiKey)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}