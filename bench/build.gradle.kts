plugins {
    application
    `java-library`
    kotlin("jvm") version "1.7.20"
    id("io.github.reyerizo.gradle.jcstress") version "0.8.13"
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

buildscript {
    repositories {
        mavenCentral()
    }

    dependencies {
        classpath("org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.18.5")
    }
}

apply(plugin = "kotlinx-atomicfu")

application {
    @Suppress("DEPRECATION")
    mainClassName = "io.github.ricnorr.benchmarks.Main"
}

tasks.jar {
    manifest.attributes["Main-Class"] = "io.github.ricnorr.benchmarks.Main"
}

java {
    sourceCompatibility = JavaVersion.VERSION_19
    targetCompatibility = JavaVersion.VERSION_19
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs = listOf(
        "--add-opens",
        "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-exports",
        "java.base/jdk.internal.util=ALL-UNNAMED",
        "--enable-preview",
        "--add-exports",
        "java.base/jdk.internal.vm.annotation=ALL-UNNAMED",
    )
}

tasks.withType<JavaExec>().configureEach {
    jvmArgs = listOf(
        "--enable-preview",
        "-XX:+UseNUMA",
        "-XX:+UseParallelGC",
        "-XX:-RestrictContended",
        "--add-opens",
        "java.base/java.lang=ALL-UNNAMED",
        "-Djna.library.path=libs/",
    )
}

group = "io.github.ricnorr"
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
    testImplementation("org.jetbrains.kotlinx:lincheck:2.16")
    testImplementation("org.junit.jupiter:junit-jupiter:5.7.1")
    implementation("com.github.oshi:oshi-dist:6.4.0")
    implementation("org.openjdk.jmh:jmh-core:1.35")
    testImplementation("org.testng:testng:7.1.0")
    annotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.35")
    implementation("org.openjdk.jol:jol-core:0.9")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.8.9")
    implementation("io.github.ricnorr:vthreads-numa-lock:0.0.3")
}

tasks.jar {
    exclude("io.github.ricnorr.benchmarks")
}

tasks.test {
    maxHeapSize = "2g"
    jvmArgs(
        "--add-opens",
        "java.base/jdk.internal.misc=ALL-UNNAMED",
        "--add-exports",
        "java.base/jdk.internal.loader=ALL-UNNAMED",
        "--add-exports",
        "java.base/jdk.internal.util=ALL-UNNAMED",
        "--add-opens",
        "java.base/java.lang=ALL-UNNAMED",
        "-XX:-RestrictContended",
    )
}
