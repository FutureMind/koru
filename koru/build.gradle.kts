plugins {
    kotlin("multiplatform")
    id("java-library")
    id("maven-publish")
    id("com.futuremind.koru.publish")
}

kotlin {

    jvm()

    iosArm64()
    iosX64()
    macosX64()
    macosArm64()
    watchosArm32()
    watchosArm64()
    watchosX86()
    watchosX64()
    tvosArm64()
    tvosX64()

    iosSimulatorArm64()
    watchosSimulatorArm64()
    tvosSimulatorArm64()

    sourceSets {

        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2-native-mt")
            }
        }

        val appleMain by creating {
            this.dependsOn(commonMain)
        }

        val appleTest by creating {
            this.dependsOn(appleMain)
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }

        val iosArm64Main by getting { this.dependsOn(appleMain) }
        val iosX64Main by getting { this.dependsOn(appleMain) }
        val macosX64Main by getting { this.dependsOn(appleMain) }
        val macosArm64Main by getting { this.dependsOn(appleMain) }
        val watchosArm32Main by getting { this.dependsOn(appleMain) }
        val watchosArm64Main by getting { this.dependsOn(appleMain) }
        val watchosX86Main by getting { this.dependsOn(appleMain) }
        val watchosX64Main by getting { this.dependsOn(appleMain) }
        val tvosArm64Main by getting { this.dependsOn(appleMain) }
        val tvosX64Main by getting { this.dependsOn(appleMain) }
        val iosSimulatorArm64Main by getting { this.dependsOn(appleMain) }
        val watchosSimulatorArm64Main by getting { this.dependsOn(appleMain) }
        val tvosSimulatorArm64Main by getting { this.dependsOn(appleMain) }

        val iosArm64Test by getting { this.dependsOn(appleTest) }
        val iosSimulatorArm64Test by getting { this.dependsOn(appleTest) }
        val iosX64Test by getting { this.dependsOn(appleTest) }

    }
}

koruPublishing {
    pomName = "Koru"
    pomDescription = "Wrappers for suspend functions / Flow in Kotlin Native"
}
