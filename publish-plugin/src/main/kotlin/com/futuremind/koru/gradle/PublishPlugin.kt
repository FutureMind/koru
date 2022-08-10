package com.futuremind.koru.gradle

import org.gradle.api.*
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.plugins.PublishingPlugin
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.*
import org.gradle.plugins.signing.SigningExtension
import org.gradle.plugins.signing.SigningPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin
import java.io.File
import java.io.FileInputStream
import java.util.*
import org.jetbrains.dokka.gradle.DokkaTask


class PublishPlugin : Plugin<Project> {

    override fun apply(project: Project) {

        val extension: PublishPluginExtension = project.extensions.create("koruPublishing")

        project.pluginManager.apply(PublishingPlugin::class)
        project.pluginManager.apply(SigningPlugin::class)
        project.pluginManager.apply(DokkaPlugin::class)

        project.afterEvaluate {

            val pomName = extension.pomName ?: throwIllegalConfig("pomName")
            val pomDescription = extension.pomDescription ?: throwIllegalConfig("pomDescription")

            val javadocJar = project.createJavaDoc()
            project.configureMavenPublication(javadocJar, pomName, pomDescription)
            project.configureArtifactSigning()
        }

    }

    private fun Project.createJavaDoc(): Jar {
        project.tasks.getByName<DokkaTask>("dokkaJavadoc") {
            outputDirectory.set(project.rootProject.file("${project.buildDir}/dokka"))
            dokkaSourceSets {
                configureEach {
                    suppress.set(true)
                }
                try {
                    //we only want the docs for maven central artifacts and javadoc doesn't
                    //really make sense for multiplatform projects
                    val commonMain by getting {
                        suppress.set(false)
                        platform.set(org.jetbrains.dokka.Platform.jvm)
                    }
                } catch (e: UnknownDomainObjectException) {
                    logger.warn("Warning: ${e.message}")
                }
            }
        }
        val koruJavadocJar by project.tasks.creating(Jar::class) {
            val dokkaTask = project.tasks.getByName<DokkaTask>("dokkaJavadoc")
            from(dokkaTask.outputDirectory)
            dependsOn(dokkaTask)
            dependsOn("build")
            archiveClassifier.value("javadoc")
        }
        return koruJavadocJar
    }

    private fun Project.configureMavenPublication(
        javadocJar: Jar,
        pomName: String,
        pomDescription: String
    ) {
        project.extensions.configure(PublishingExtension::class) {

            val localProperties = loadLocalProperties(rootProject)

            publications.all {
                version = rootProject.version
                group = rootProject.group
            }

            publications.withType(MavenPublication::class) {
                pom {
                    name.set(pomName)
                    description.set(pomDescription)
                    url.set("https://github.com/FutureMind/koru")
                    licenses {
                        license {
                            name.set("The MIT License")
                            url.set("https://opensource.org/licenses/MIT")
                        }
                    }
                    developers {
                        developer {
                            id.set("mklimczak")
                            name.set("Michał Klimczak")
                            email.set("m.klimczak@futuremind.com")
                        }
                    }
                    scm {
                        url.set("https://github.com/FutureMind/koru")
                    }
                }

                artifact(javadocJar)
            }

            repositories {
                maven {
                    name = "Sonatype_OSS"
                    url = uri(
                        if (version.toString().endsWith("SNAPSHOT")) {
                            "https://oss.sonatype.org/content/repositories/snapshots"
                        } else {
                            "https://oss.sonatype.org/service/local/staging/deploy/maven2"
                        }
                    )
                    credentials {
                        username = localProperties["sonatypeUsername"] as String?
                        password = localProperties["sonatypePassword"] as String?
                    }
                }
            }

        }
    }

    private fun loadLocalProperties(rootProject: Project) = Properties().apply {
        load(FileInputStream(File(rootProject.rootDir, "local.properties")))
    }

    /**
    Sign all artifacts
    Requires following properties to be set in ~/.gradle/gradle.properties
    (don't put credentials in project's gradle.properties!)
    signing.keyId=
    signing.password=
    signing.secretKeyRingFile=
     */
    private fun Project.configureArtifactSigning() {
        project.extensions.getByType<SigningExtension>().run {
            try {
                sign(project.extensions.getByType<PublishingExtension>().publications)
            } catch (e: InvalidUserDataException) {
                logger.warn("Could not create signing task (it's probably fine, it might have been added by gradle-portal-plugin: ${e.message}")
            }
        }
    }

    private fun throwIllegalConfig(emptyParam: String): Nothing =
        throw IllegalStateException("You need to provide $emptyParam in koruPublishing plugin")
}

open class PublishPluginExtension {
    var pomName: String? = null
    var pomDescription: String? = null
}
