pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://plugins.gradle.org/m2/")
    }
}
rootProject.name = "Koru"

includeBuild("gradlePlugins")
include(":koru")
include(":koru-processor")
