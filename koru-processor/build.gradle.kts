import java.util.Properties

plugins {
    /*
        this could be a pure-jvm module, but there are some dependency issues
        https://stackoverflow.com/questions/65830632/cant-access-commonmain-multiplatform-classes-from-a-jvm-only-module
     */
    kotlin("multiplatform")
    id("java-library")
    id("maven-publish")
}

repositories {
    jcenter()
}

kotlin {

    jvm() //this is only used as kapt (annotation processor, so pure jvm)

    sourceSets {

        val jvmMain by getting {
            dependencies {

                implementation(project(":koru"))

                //code generation
                val kotlinpoetVersion = "1.7.2"
                implementation("com.squareup:kotlinpoet:$kotlinpoetVersion")
                implementation("com.squareup:kotlinpoet-metadata:$kotlinpoetVersion")
                implementation("com.squareup:kotlinpoet-metadata-specs:$kotlinpoetVersion")
                implementation("com.squareup:kotlinpoet-classinspector-elements:$kotlinpoetVersion")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2-native-mt")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-api:5.7.0")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.7.0")
                implementation("com.github.tschuchortdev:kotlin-compile-testing:1.3.5")
                implementation("io.kotest:kotest-assertions-core:4.4.0.RC2")
            }
        }

    }
}

publishing {

    publications.all {
        version = rootProject.version
        group = rootProject.group
    }

    repositories {
        maven {
            name = "bintray"
            setUrl("https://api.bintray.com/content/futuremind/koru/koru-processor/$version/;publish=1;override=0")
            credentials {
                //TODO get from env vars in CI process
                val properties = Properties()
                properties.load(project.rootProject.file("local.properties").inputStream())
                username = properties["bintrayUsername"] as String
                password = properties["bintrayKey"] as String
            }
        }
    }

}

tasks.withType<Test> {
    useJUnitPlatform() //otherwise junit5 tests cannot be run from jvmTest
}