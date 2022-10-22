import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm") version "1.7.20"
    id("me.champeau.jmh") version "0.6.7"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

group = "me.ricnorr"
version = "1.0-SNAPSHOT"


repositories {
    mavenCentral()
}

dependencies {
    implementation("commons-cli:commons-cli:1.5.0")
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.35")
}

val threadP: String? by project
val includeP: String? by project
val profilerP: String? by project

jmh {
    includes.set(listOf(includeP ?: ".*Benchmark"))
    threads.set(Integer.parseInt(threadP ?: "16") )
    if (profilerP != null) {
        profilers.set(listOf(profilerP))
    }

    fork.set(1)
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}