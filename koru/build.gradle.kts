import java.util.Properties

plugins {
    kotlin("multiplatform")
    id("java-library")
    id("maven-publish")
}

repositories {
    jcenter()
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
        }
    }

    repositories {
        maven {
            name = "bintray"
            setUrl("https://api.bintray.com/content/futuremind/koru/koru/$version/;publish=1;override=0")
            credentials {
                //TODO get from env vars in CI process
                val properties = Properties()
                properties.load(project.rootProject.file("local.properties").inputStream())
                username = properties["bintrayUsername"] as String
                password = properties["bintrayKey"] as String
            }
        }
    }
}