package com.futuremind.koru.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.dependencies


class CompilerPlugin : Plugin<Project> {

    override fun apply(project: Project) = with(project) {
        requireKspPluginDependency()
        afterEvaluate {
            enableKspRunForAppleSourceSet()
        }
    }

    private fun Project.requireKspPluginDependency() {
        requireNotNull(pluginManager.findPlugin("com.google.devtools.ksp")) {
            "You need to provide ksp plugin, e.g. plugins { id(\"com.google.devtools.ksp\") version \"1.7.0-1.0.6\" }"
        }
    }

    private fun Project.enableKspRunForAppleSourceSet() = dependencies {
        val kspAppleRegex = "ksp[Ios|Tvos|Macos|Watchos]+(?!.*Test$)".toRegex()
        configurations
            .filter { it.name.contains(kspAppleRegex) }
            .forEach {
                //todo don't hardcode version
                add(it.name, "com.futuremind:koru-processor:0.13.0")
            }
    }
}