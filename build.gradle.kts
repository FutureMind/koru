buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.5.30")
        classpath("com.android.tools.build:gradle:7.0.2")
        classpath("de.mannodermaus.gradle.plugins:android-junit5:1.7.0.0")
    }
}

allprojects {
    repositories {
        mavenCentral()
    }
}

group = "com.futuremind"
version = "0.8.0"
