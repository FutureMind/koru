rootProject.name = "Koru"
gradle.rootProject {
    group = "com.futuremind"
    version = "0.10.0"
}


includeBuild("gradlePlugins")
include(":koru")
include(":koru-processor")

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://plugins.gradle.org/m2/")
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}