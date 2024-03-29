@Suppress("DSL_SCOPE_VIOLATION")
plugins {
    /*
        this could be a pure-jvm module, but there are some dependency issues
        https://stackoverflow.com/questions/65830632/cant-access-commonmain-multiplatform-classes-from-a-jvm-only-module
     */
    kotlin("multiplatform") version libs.versions.kotlin
    id("java-library")
    id("maven-publish")
    id("com.futuremind.koru.publish")
}

kotlin {

    //this is only used as kapt / ksp (annotation processor, so pure jvm)
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
                val kotlinpoetVersion = "1.12.0"
                implementation("com.squareup:kotlinpoet:$kotlinpoetVersion")
                implementation("com.squareup:kotlinpoet-metadata:$kotlinpoetVersion")
                implementation("com.squareup:kotlinpoet-ksp:$kotlinpoetVersion")

                implementation(libs.coroutines)
                implementation(libs.ksp)

            }
        }

        val jvmTest by getting {
            dependencies {
                val junitVersion = "5.8.2"
                val compileTestingVersion = "1.4.9"
                implementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
                runtimeOnly("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
                implementation("org.junit.jupiter:junit-jupiter-params:$junitVersion")
                implementation("com.github.tschuchortdev:kotlin-compile-testing:$compileTestingVersion")
                implementation("com.github.tschuchortdev:kotlin-compile-testing-ksp:$compileTestingVersion")
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
