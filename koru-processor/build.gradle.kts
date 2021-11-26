plugins {
    /*
        this could be a pure-jvm module, but there are some dependency issues
        https://stackoverflow.com/questions/65830632/cant-access-commonmain-multiplatform-classes-from-a-jvm-only-module
     */
    val kotlinVersion = "1.5.31"
    kotlin("multiplatform") version kotlinVersion
    id("java-library")
    id("maven-publish")
    id("com.futuremind.koru.publish")
}

kotlin {

    //this is only used as kapt (annotation processor, so pure jvm)
    jvm {
        val main by compilations.getting {
            kotlinOptions {
                jvmTarget = "1.8"
            }
        }
    }

    sourceSets {

        val jvmMain by getting {
            dependencies {

                implementation(project(":koru"))

                //code generation
                val kotlinpoetVersion = "1.10.2"
                implementation("com.squareup:kotlinpoet:$kotlinpoetVersion")
                implementation("com.squareup:kotlinpoet-metadata:$kotlinpoetVersion")

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
            }
        }

        val jvmTest by getting {
            dependencies {
                implementation("org.junit.jupiter:junit-jupiter-api:5.8.1")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.1")
                implementation("com.github.tschuchortdev:kotlin-compile-testing:1.4.6")
                implementation("io.kotest:kotest-assertions-core:4.6.3")
            }
        }

    }
}

koruPublishing {
    pomName = "Koru - Processor"
    pomDescription = "Wrappers for suspend functions / Flow in Kotlin Native - annotation processor."
}

//otherwise junit5 tests cannot be run from jvmTest
tasks.withType<Test> {
    useJUnitPlatform()
}
