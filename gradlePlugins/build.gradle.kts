plugins {
    `kotlin-dsl`
    id("maven-publish")
    id("signing")
    id("org.jetbrains.dokka") version "1.6.10"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.dokka:dokka-gradle-plugin:1.6.10")
}

gradlePlugin {
    plugins {
        create("publish") {
            id = "com.futuremind.koru.publish"
            implementationClass = "com.futuremind.koru.gradle.PublishPlugin"
        }
    }
}
