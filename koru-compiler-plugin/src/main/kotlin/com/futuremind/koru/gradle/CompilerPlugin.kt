package com.futuremind.koru.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByType
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension


class CompilerPlugin : Plugin<Project> {

    override fun apply(project: Project) = with(project) {
        addKspPluginDependency()
        enableKspRunForCommonMainSourceSet()
        makeSureCompilationIsRunAfterKsp()
        addGeneratedFilesToAppleSourceSets()
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

    private fun Project.addGeneratedFilesToAppleSourceSets() = extensions
        .getByType<KotlinMultiplatformExtension>().sourceSets
        .matching {
            it.name.contains("ios")
                    || it.name.contains("macos")
                    || it.name.contains("watchos")
                    || it.name.contains("tvos")
        }
        .configureEach {
            kotlin.srcDir("${project.buildDir.absolutePath}/generated/ksp/metadata/commonMain/kotlin")
        }
}


