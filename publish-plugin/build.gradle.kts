@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    `kotlin-dsl`
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka") version libs.versions.kotlin
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.dokka)
}

gradlePlugin {
    plugins {
        create("publish") {
            id = "com.futuremind.koru.publish"
            implementationClass = "com.futuremind.koru.gradle.PublishPlugin"
        }
    }
}
