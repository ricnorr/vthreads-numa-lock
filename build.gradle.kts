import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.7.20"
    id("io.github.reyerizo.gradle.jcstress") version "0.8.13"
}

application {
    mainClass.set("ru.ricnorr.benchmarks.Main")
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
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.commons:commons-csv:1.9.0")
    implementation("org.ejml:ejml-all:0.41")
    implementation("net.java.dev.jna:jna:5.12.1")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}
