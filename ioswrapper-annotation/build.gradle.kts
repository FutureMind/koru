plugins {
    kotlin("multiplatform")
    id("java-library")
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
                api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2-native-mt")
            }
        }
    }
}