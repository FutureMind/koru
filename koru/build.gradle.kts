plugins {
    kotlin("multiplatform")
    id("java-library")
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka") version "1.4.32"
}

repositories {
    mavenCentral()
}

kotlin {

    jvm()

    iosArm64()
    iosX64()
    macosX64()
    macosArm64()
    watchosArm32()
    watchosArm64()
    watchosX86()
    watchosX64()
    tvosArm64()
    tvosX64()

    iosSimulatorArm64()
    watchosSimulatorArm64()
    tvosSimulatorArm64()

    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
            }
        }

        val appleMain by creating {
            this.dependsOn(commonMain)
        }

        val appleTest by creating {
            this.dependsOn(appleMain)
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val iosArm64Main by getting { this.dependsOn(appleMain) }
        val iosX64Main by getting { this.dependsOn(appleMain) }
        val macosX64Main by getting { this.dependsOn(appleMain) }
        val macosArm64Main by getting { this.dependsOn(appleMain) }
        val watchosArm32Main by getting { this.dependsOn(appleMain) }
        val watchosArm64Main by getting { this.dependsOn(appleMain) }
        val watchosX86Main by getting { this.dependsOn(appleMain) }
        val watchosX64Main by getting { this.dependsOn(appleMain) }
        val tvosArm64Main by getting { this.dependsOn(appleMain) }
        val tvosX64Main by getting { this.dependsOn(appleMain) }
        val iosSimulatorArm64Main by getting { this.dependsOn(appleMain) }
        val watchosSimulatorArm64Main by getting { this.dependsOn(appleMain) }
        val tvosSimulatorArm64Main by getting { this.dependsOn(appleMain) }

        val iosArm64Test by getting { this.dependsOn(appleTest) }
        val iosSimulatorArm64Test by getting { this.dependsOn(appleTest) }
        val iosX64Test by getting { this.dependsOn(appleTest) }

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
                username = project.properties["sonatypeUsername"] as String?
                password = project.properties["sonatypePassword"] as String?
            }
        }
    }
}

// Create javadocs with dokka and attach to maven publication
tasks {
    dokkaJavadoc {
        outputDirectory.set(project.rootProject.file("$buildDir/dokka"))
        dokkaSourceSets {
            configureEach {
                suppress.set(true)
            }
            val commonMain by getting {
                suppress.set(false)
                platform.set(org.jetbrains.dokka.Platform.jvm)
            }
        }
    }
}
val javadocJar by tasks.creating(Jar::class) {
    val dokkaTask = tasks.getByName<org.jetbrains.dokka.gradle.DokkaTask>("dokkaJavadoc")
    from(dokkaTask.outputDirectory)
    dependsOn(dokkaTask)
    dependsOn("build")
    archiveClassifier.value("javadoc")
}
publishing {
    publications.withType<MavenPublication>().all {
        artifact(javadocJar)
    }
}

/*
Sign all artifacts
Requires following properties to be set in gradle.properties
signing.keyId=
signing.password=
signing.secretKeyRingFile=
 */
publishing {
    publications.withType<MavenPublication>().all {
        signing {
            sign(this@all)
        }
    }
}