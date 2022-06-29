package com.futuremind.koru.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension


class CompilerPlugin : Plugin<Project> {

    override fun apply(project: Project) = with(project) {
        val extension: KoruPluginExtension = extensions.create("koru")
        addKspPluginDependency()
        enableKspRunForCommonMainSourceSet()
        makeSureCompilationIsRunAfterKsp()
        afterEvaluate {
            require(extension.nativeSourceSetNames.isNotEmpty()) {
                "You need to provide the name of your main native source set in your build.gradle, e.g. koru.nativeSourceSetNames = listOf(\"iosMain\")"
            }
            addGeneratedFilesToSourceSets(extension.nativeSourceSetNames)
        }
    }

    private fun Project.addKspPluginDependency() = pluginManager.apply("com.google.devtools.ksp")

    private fun Project.enableKspRunForCommonMainSourceSet() = dependencies {
        //todo don't hardcode version
        add("kspCommonMainMetadata", "com.futuremind:koru-processor:0.11.0")
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

    private fun Project.addGeneratedFilesToSourceSets(sourceSetNames: List<String>) = extensions
        .getByType<KotlinMultiplatformExtension>().sourceSets
        .matching {
            sourceSetNames.contains(it.name)
        }
        .configureEach {
            kotlin.srcDir("${project.buildDir.absolutePath}/generated/ksp/metadata/commonMain/kotlin")
        }
}

open class KoruPluginExtension {
    var nativeSourceSetNames: List<String> = listOf()
}


