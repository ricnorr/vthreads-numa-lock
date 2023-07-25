plugins {
    `java-library`
    `maven-publish`
    signing
    id("io.github.gradle-nexus.publish-plugin") version "1.3.0"
}

nexusPublishing {
    repositories {
        sonatype { // only for users registered in Sonatype after 24 Feb 2021
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
            username.set("ricnorr")
            password.set("KartoshkA2001()")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_19
    targetCompatibility = JavaVersion.VERSION_19
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "vthreads-numa-lock"
            groupId = "io.github.ricnorr"
            version = "0.0.5"
            from(components["java"])
            pom {
                packaging = "jar"
                name.set("Effective locks library for virtual threads on NUMA")
                url.set("https://github.com/ricnorr/")
                description.set("Effective locks library for virtual threads on NUMA")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                scm {
                    connection.set("scm:https://github.com/ricnorr/vthreads-numa-lock.git")
                    developerConnection.set("scm:git@github.com:ricnorr/vthreads-numa-lock.git")
                    url.set("https://github.com/ricnorr/vthreads-numa-lock")
                }

                developers {
                    developer {
                        id.set("ricnorr")
                        name.set("Nikolai Korobeinikov")
                        email.set("kolyan125125@yandex.ru")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            val releasesUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().endsWith("SNAPSHOT")) snapshotsUrl else releasesUrl
            credentials {
                username = project.properties["ossrhUsername"].toString()
                password = project.properties["ossrhPassword"].toString()
            }
        }
    }
}

signing {
    sign(publishing.publications["mavenJava"])
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

tasks.withType<Javadoc> {
    val javadocOptions = options as CoreJavadocOptions
    javadocOptions.addMultilineStringsOption("-add-exports").value = listOf(
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
version = "2.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("net.java.dev.jna:jna:5.12.1")
    implementation("com.github.oshi:oshi-dist:6.4.0")
}
