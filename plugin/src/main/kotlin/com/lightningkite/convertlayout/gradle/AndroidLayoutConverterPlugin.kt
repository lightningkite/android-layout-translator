package com.lightningkite.convertlayout.gradle

import com.lightningkite.convertlayout.ios.IosTranslator
import com.lightningkite.convertlayout.util.camelCase
import com.lightningkite.convertlayout.web.WebTranslator
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import java.io.File

open class AndroidLayoutConverterPluginExtension {
    open var iosFolder: File? = null
    open var iosProjectName: String? = null
    open var iosModuleName: String? = null
    open var webFolder: File? = null
    open var webProjectName: String? = null
}

fun Project.androidLayoutConverter(configure: Action<AndroidLayoutConverterPluginExtension>) {
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("AndroidLayoutConverterPluginExtension", configure)
}

internal fun DependencyHandler.equivalents(dependencyNotation: Any): Dependency? {
    return this.add("equivalents", dependencyNotation)
}

class AndroidLayoutConverterPlugin: Plugin<Project> {

    override fun apply(target: Project) {
        val equivalentsSourceSet = target.objects.sourceDirectorySet("equivalents", "XML/XIB/HTML Equivalents")
        equivalentsSourceSet.srcDirs(target.projectDir.resolve("src/main/equivalents"))

        val equivalentsConfiguration = target.configurations.maybeCreate("equivalents").apply {
            description = "Equivalent declarations for translations"
            isCanBeResolved = true
            isCanBeConsumed = true
            isVisible = true
        }

        val ext = target.extensions.create("AndroidLayoutConverterPluginExtension", AndroidLayoutConverterPluginExtension::class.java)
        target.tasks.create("xmlToXib") {
            it.group = "ios"
            it.inputs.files(equivalentsConfiguration)
            it.doLast {
                val iosName = ext.iosProjectName ?: target.name.camelCase().capitalize()
                val iosModuleName = ext.iosModuleName ?: iosName
                val iosBase = ext.iosFolder ?: target.projectDir.resolve("../ios")
                val iosFolder = iosBase.resolve(iosName)
                val translator = IosTranslator(
                    androidFolder = target.projectDir,
                    iosFolder = iosFolder,
                    iosModuleName = iosModuleName,
                    iosName = iosName,
                    replacementFolders = equivalentsConfiguration.toList() + equivalentsSourceSet.toList()
                )
                translator()
            }
        }
        target.tasks.create("xmlToHtml") {
            it.group = "web"
            it.inputs.files(equivalentsConfiguration)
            it.doLast {
                val webName = ext.webProjectName ?: target.name.camelCase().capitalize()
                val webBase = ext.webFolder ?: target.projectDir.resolve("../web")
                val translator = WebTranslator(
                    androidFolder = target.projectDir,
                    webFolder = webBase,
                    webName = webName,
                    replacementFolders = equivalentsConfiguration.toList() + equivalentsSourceSet.toList()
                )
                translator()
            }
        }
    }
}