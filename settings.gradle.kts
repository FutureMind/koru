rootProject.name = "Koru"

gradle.allprojects {
    group = "com.futuremind"
    version = "0.12.0"
}

includeBuild("publish-plugin")
include("koru")
include("koru-compiler-plugin")
include("koru-processor")

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
    versionCatalogs {
        create("libs") {
            from(files("libs.versions.toml"))
        }
    }
    repositories {
        mavenCentral()
    }
}