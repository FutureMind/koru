pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        jcenter() //TODO get rid of it when ready
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://plugins.gradle.org/m2/")
    }
}
rootProject.name = "Koru"

include(":koru")
include(":koru-processor")