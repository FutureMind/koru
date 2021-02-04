import java.util.Properties

plugins {
    kotlin("multiplatform")
    id("java-library")
    id("maven-publish")
    id("org.jetbrains.dokka") version "1.4.20"
}

repositories {
    mavenCentral()
    maven(url="https://dl.bintray.com/kotlin/dokka") //TODO 1.4.20 not in mavenCentral yet
    jcenter()
    /*
    com.soywiz.korlibs.korte:korte-jvm:1.10.3
    org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2.
        Required by: project :koru > org.jetbrains.dokka:javadoc-plugin:1.4.20
     */
}

kotlin {

    jvm()
    ios()

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2-native-mt")
            }
        }
    }
}

publishing {

    publications.all {
        version = rootProject.version
        group = rootProject.group
    }

    publications.withType<MavenPublication> {
        pom {
            name.set("Koru")
            description.set("Wrappers for suspend functions / Flow in Kotlin Native")
            url.set("https://github.com/FutureMind/koru")
            licenses {
                license {
                    name.set("The MIT License")
                    url.set("https://opensource.org/licenses/MIT")
                }
            }
            developers {
                developer {
                    id.set("mklimczak")
                    name.set("Michał Klimczak")
                    email.set("m.klimczak@futuremind.com")
                }
            }
            scm {
                url.set("https://github.com/FutureMind/koru")
            }
        }
    }

    repositories {
        maven {
            name = "Sonatype_OSS"
            url = uri(
                if (version.toString().endsWith("SNAPSHOT")) {
                    "https://oss.sonatype.org/content/repositories/snapshots"
                } else {
                    "https://oss.sonatype.org/service/local/staging/deploy/maven2"
                }
            )
            credentials {
                val properties = Properties()
                properties.load(project.rootProject.file("local.properties").inputStream())
                username = properties["sonatypeUsername"] as String
                password = properties["sonatypePassword"] as String
            }
        }
    }
}

// Create javadocs and attach to maven publication (required by mavenCentral)
//tasks {
//    dokkaJavadoc {
//        outputDirectory.set(project.rootProject.file("$buildDir/dokka"))
//        dokkaSourceSets {
//            named("commonMain") {
//                sourceRoots.from(kotlin.sourceSets.getByName("commonMain").kotlin.srcDirs)
//            }
//        }
//    }
//}
//val javadocJar by tasks.creating(Jar::class) {
//    val dokkaTask = tasks.getByName<org.jetbrains.dokka.gradle.DokkaTask>("dokkaJavadoc")
//    from(dokkaTask.outputDirectory)
//    dependsOn(dokkaTask)
//    dependsOn("build")
//    archiveClassifier.value("javadoc")
//}
//publishing {
//    publications.withType<MavenPublication>().all {
//        artifact(javadocJar)
//    }
//}