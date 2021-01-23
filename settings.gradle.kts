pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        jcenter()
        mavenCentral()
        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://plugins.gradle.org/m2/")
    }
}
rootProject.name = "KMM Suspend Wrapper"

include(":ioswrapper-annotation")
include(":ioswrapper-processor")

