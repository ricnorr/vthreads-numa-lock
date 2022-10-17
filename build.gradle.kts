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
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.35")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}