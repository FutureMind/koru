buildscript {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.30")
        classpath("com.android.tools.build:gradle:4.1.2")
        classpath("de.mannodermaus.gradle.plugins:android-junit5:1.7.0.0")
    }
}


group = "com.futuremind"
version = "0.3.7"