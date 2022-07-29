/*
 * Using 1.5.31 in this plugin, because of https://kotlinlang.slack.com/archives/C19FD9681/p1653583730830919
 * Basically Gradle 7.4.2 uses 2.1.7 of kotlin-dsl, which depends on 1.5.31 kotlin
 * (is compatible with 1.6.21, but not 1.7.0)
 */
@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    `kotlin-dsl`
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka") version "1.5.31"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.5.31")
}

gradlePlugin {
    plugins {
        create("publish") {
            id = "com.futuremind.koru.publish"
            implementationClass = "com.futuremind.koru.gradle.PublishPlugin"
        }
    }
}
