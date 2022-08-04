import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.7.0"
    kotlin("plugin.serialization") version "1.7.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"

    java
}

repositories {
    mavenCentral()
    maven("https://jitpack.io")
}

dependencies {
    //compileOnly(kotlin("stdlib"))

    compileOnly("com.github.Minestom:Minestom:08f37400b0")
    compileOnly("com.github.EmortalMC:Immortal:69d17e00a0")
    compileOnly("com.github.EmortalMC:TNT:f0680e2013")

    compileOnly("com.github.EmortalMC:NBStom:18bc9744a7")
    //compileOnly("org.redisson:redisson:3.17.4")
    compileOnly("org.redisson:redisson:3.17.5")
    compileOnly("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.2")
    //implementation(files("libs/Blocky-1.0-SNAPSHOT.jar"))
}

tasks {
    processResources {
        filesMatching("extension.json") {
            expand(project.properties)
        }
    }

    named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
        archiveBaseName.set(project.name)
        mergeServiceFiles()
        minimize()
    }
    build { dependsOn(shadowJar) }

}

val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions.jvmTarget = JavaVersion.VERSION_17.toString()

compileKotlin.kotlinOptions {
    freeCompilerArgs = listOf("-Xinline-classes")
}
