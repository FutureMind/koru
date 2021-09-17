plugins {
    /*
        this could be a pure-jvm module, but there are some dependency issues
        https://stackoverflow.com/questions/65830632/cant-access-commonmain-multiplatform-classes-from-a-jvm-only-module
     */
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

    //this is only used as kapt (annotation processor, so pure jvm)
    jvm {
        val main by compilations.getting {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }
    sourceSets {

        val jvmMain by getting {
            dependencies {

                implementation(project(":koru"))

                //code generation
                val kotlinpoetVersion = "1.9.0"
                implementation("com.squareup:kotlinpoet:$kotlinpoetVersion")
                implementation("com.squareup:kotlinpoet-metadata:$kotlinpoetVersion")
                implementation("com.squareup:kotlinpoet-metadata-specs:$kotlinpoetVersion")
                implementation("com.squareup:kotlinpoet-classinspector-elements:$kotlinpoetVersion")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
                implementation("com.github.tschuchortdev:kotlin-compile-testing:1.3.5")
                implementation("io.kotest:kotest-assertions-core:4.4.0.RC2")
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
            name.set("Koru - Processor")
            description.set("Wrappers for suspend functions / Flow in Kotlin Native - annotation processor.")
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

//otherwise junit5 tests cannot be run from jvmTest
tasks.withType<Test> {
    useJUnitPlatform()
}

// Create javadocs and attach to maven publication
tasks {
    dokkaJavadoc {
        outputDirectory.set(project.rootProject.file("$buildDir/dokka"))
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
