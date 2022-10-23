import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    kotlin("jvm") version "1.7.20"
    id("me.champeau.jmh") version "0.6.7"
    id("io.github.reyerizo.gradle.jcstress") version "0.8.13"
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

val lockTypesP: String? by project

jmh {
    includes.set(listOf(project.properties["include"]?.toString() ?: ".*Benchmark"))
    val threadsCnt = Integer.parseInt(project.properties["thread"]?.toString() ?: "16")
    threads.set(threadsCnt)
    fork.set(Integer.parseInt(project.properties["fork"]?.toString() ?: "5"))
    project.properties["profiler"]?.toString()?.let {
        profilers.set(it.split(","))
    }
    resultsFile.set(project.file("${project.buildDir}/results/jmh/threads/${threadsCnt}/results.csv"))
    resultFormat.set("CSV")
}


tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}