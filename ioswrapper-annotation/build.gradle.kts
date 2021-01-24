plugins {
    kotlin("multiplatform")
    id("java-library")
    id("maven-publish")
}

repositories {
    jcenter()
}

kotlin {

    jvm {
        mavenPublication {
            version = "0.1"
            artifactId = "suspend-wrapper"
            group = "com.futuremind"
        }
    }
    ios {
        mavenPublication {
            version = "0.1"
            artifactId = "suspend-wrapper"
            group = "com.futuremind"
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.4.2-native-mt")
            }
        }
    }
}

publishing {
//    publications {
//        create<MavenPublication>("maven") {
//            group = "com.futuremind"
//            version = "0.1"
//            artifactId = "suspend-wrapper"
////            from(components["java"])
//        }
//    }
}