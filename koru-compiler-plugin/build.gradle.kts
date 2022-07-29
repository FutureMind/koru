plugins {
    `kotlin-dsl`
    id("maven-publish")
    id("com.gradle.plugin-publish") version "1.0.0"
    id("com.futuremind.koru.publish")
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("gradle-plugin"))
}

gradlePlugin {
    plugins {
        create("compilerPlugin") {
            id = "com.futuremind.koru"
            displayName = "Koru - Gradle Plugin"
            description = "Wrappers for suspend functions / Flow in Kotlin Native - gradle plugin"
            implementationClass = "com.futuremind.koru.gradle.CompilerPlugin"
        }
    }
}

pluginBundle {
    website = "https://github.com/FutureMind/koru"
    vcsUrl = "https://github.com/FutureMind/koru"
    tags = listOf("coroutines", "kmm", "kmp", "kotlin", "multiplatform", "native", "swift", "suspend", "flow", "kotlin-multiplatform", "kotlin-native")
}

koruPublishing {
    pomName = "Koru - Gradle Plugin"
    pomDescription = "Wrappers for suspend functions / Flow in Kotlin Native - gradle plugin."
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(11))
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
    }
}