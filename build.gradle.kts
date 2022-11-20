import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    application
    kotlin("jvm") version "1.7.20"
    id("me.champeau.jmh") version "0.6.7"
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
    jmhAnnotationProcessor("org.openjdk.jmh:jmh-generator-annprocess:1.35")
    implementation("com.googlecode.json-simple:json-simple:1.1.1")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.commons:commons-csv:1.9.0")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
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
    (project.properties["fileName"]?.toString() ?: "").let {
        resultsFile.set(project.file("${project.buildDir}/results/jmh/threads/${it}results_${threadsCnt}.csv"))
    }
    resultFormat.set("CSV")

    val parametersMap = mutableMapOf<String, ListProperty<String>>()
    (project.properties["inSectionMatrixSize"]?.toString() ?: "1000").let {
        val inSectionMatrixSizeProperties = project.objects.listProperty(String::class.java)
        inSectionMatrixSizeProperties.addAll(it.split(","))
        parametersMap["inSectionMatrixSize"] = inSectionMatrixSizeProperties
    }
    (project.properties["afterSectionMatrixSize"]?.toString() ?: "1000").let {
        val afterSectionMatrixSizeProperties = project.objects.listProperty(String::class.java)
        afterSectionMatrixSizeProperties.addAll(it.split(","))
        parametersMap["afterSectionMatrixSize"] = afterSectionMatrixSizeProperties
    }
    project.properties["lockType"]?.toString()?.let {
        val lockTypeProperties = project.objects.listProperty(String::class.java)
        lockTypeProperties.addAll(it.split(","))
        parametersMap["lockType"] = lockTypeProperties
    }
    benchmarkParameters.set(parametersMap)
}
