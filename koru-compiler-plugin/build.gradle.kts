plugins {
    `kotlin-dsl`
    id("maven-publish")
    id("com.futuremind.koru.publish")
}

//TODO
group = "com.futuremind"
version = "0.11.0"

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
            implementationClass = "com.futuremind.koru.gradle.CompilerPlugin"
        }
    }
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