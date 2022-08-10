package com.futuremind.koru.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import java.io.File


class CompilerPlugin : Plugin<Project> {

    override fun apply(project: Project) = with(project) {
        val extension: KoruPluginExtension = extensions.create("koru")
        requireKspPluginDependency()
        enableKspRunForCommonMainSourceSet()
        makeSureCompilationIsRunAfterKsp()
        afterEvaluate {
            requireSourceSetsNamesSet(extension.nativeSourceSetNames)
            addGeneratedFilesToSourceSets(extension.nativeSourceSetNames)
        }
    }

    private fun Project.requireKspPluginDependency() {
        requireNotNull(pluginManager.findPlugin("com.google.devtools.ksp")) {
            "You need to provide ksp plugin, e.g. plugins { id(\"com.google.devtools.ksp\") version \"1.7.0-1.0.6\" }"
        }
    }

    private fun Project.enableKspRunForCommonMainSourceSet() = dependencies {
        //todo don't hardcode version
        add("kspCommonMainMetadata", "com.futuremind:koru-processor:0.11.1")
    }

    private fun Project.makeSureCompilationIsRunAfterKsp() = tasks
        .matching {
            it.name.startsWith("compileKotlinIos")
                    || it.name.startsWith("compileKotlinMacos")
                    || it.name.startsWith("compileKotlinWatchos")
                    || it.name.startsWith("compileKotlinTvos")
        }
        .configureEach {
            dependsOn("kspCommonMainKotlinMetadata")
        }

    private fun requireSourceSetsNamesSet(nativeSourceSetNames: List<String>) {
        require(nativeSourceSetNames.isNotEmpty()) {
            "You need to provide the name of your main native source set in your build.gradle, e.g. koru.nativeSourceSetNames = listOf(\"iosMain\")"
        }
    }

    private fun Project.addGeneratedFilesToSourceSets(sourceSetNames: List<String>) {
        val anyMatch = extensions
            .getByType<KotlinMultiplatformExtension>().sourceSets
            .any { sourceSetNames.contains(it.name) }
        if (!anyMatch) throw IllegalStateException("None of the provided source set names were matched: $sourceSetNames. You need to provide the name of your main native source set in your build.gradle, e.g. koru.nativeSourceSetNames = listOf(\"iosMain\")")

        extensions
            .getByType<KotlinMultiplatformExtension>().sourceSets
            .matching { sourceSetNames.contains(it.name) }
            .configureEach {
                kotlin.srcDir("${project.buildDir.absolutePath}${File.separator}generated${File.separator}ksp${File.separator}metadata${File.separator}commonMain${File.separator}kotlin")
            }
    }
}

open class KoruPluginExtension {
    var nativeSourceSetNames: List<String> = listOf()
}


