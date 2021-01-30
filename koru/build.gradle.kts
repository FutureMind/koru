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