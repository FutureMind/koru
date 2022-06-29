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
    /*
     * Using 1.5.31 for ksp, because of https://kotlinlang.slack.com/archives/C19FD9681/p1653583730830919
     * Basically Gradle 7.4.2 uses 2.1.7 of kotlin-dsl, which depends on 1.5.31 kotlin
     * (is compatible with 1.6.21, but not 1.7.0)
     */
    compileOnly("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:1.5.31-1.0.1")
    runtimeOnly(libs.kspPlugin)
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