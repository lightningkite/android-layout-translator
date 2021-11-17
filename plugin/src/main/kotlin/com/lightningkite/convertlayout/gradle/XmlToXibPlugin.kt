package com.lightningkite.convertlayout.gradle

import com.lightningkite.convertlayout.ios.IosTranslator
import com.lightningkite.convertlayout.util.camelCase
import org.gradle.api.Action
import org.gradle.api.Plugin
import org.gradle.api.Project
import java.io.File
import java.util.*

open class XmlToXibPluginExtension {
    open var iosFolder: File? = null
    open var iosProjectName: String? = null
    open var iosModuleName: String? = null
}

fun Project.xmlToXib(configure: Action<XmlToXibPluginExtension>) {
    (this as org.gradle.api.plugins.ExtensionAware).extensions.configure("XmlToXibPluginExtension", configure)
}

class XmlToXibPlugin: Plugin<Project> {
    override fun apply(target: Project) {
        val ext = target.extensions.create("XmlToXibPluginExtension", XmlToXibPluginExtension::class.java)
        target.tasks.create("xmlToXib") {
            it.group = "ios"
            it.doLast {
                val iosName = ext.iosProjectName ?: target.name.camelCase().capitalize()
                val iosModuleName = ext.iosModuleName ?: iosName
                val iosBase = ext.iosFolder ?: target.projectDir.resolve("../ios")
                val iosFolder = iosBase.resolve(iosName)
                val dependencies = run {
                    val localProperties = Properties().apply {
                        val f = target.rootProject.file("local.properties")
                        if (f.exists()) {
                            load(f.inputStream())
                        }
                    }
                    val pathRegex = Regex(":path => '([^']+)'")
                    val home = System.getProperty("user.home")
                    val localPodSpecRefs = iosBase
                        .resolve("Podfile")
                        .takeIf { it.exists() }
                        ?.also { println("Found podfile: $it") }
                        ?.let { file ->
                            file
                                .readText()
                                .let { pathRegex.findAll(it) }
                                .also { println("Found podfile paths: ${it.joinToString { it.value }}") }
                                .map { it.groupValues[1] }
                                .map { it.replace("~", home) }
                                .map {
                                    if (it.startsWith('/'))
                                        File(it).parentFile
                                    else
                                        File(file.parentFile, it).parentFile
                                }
                        } ?: sequenceOf()
                    val allLocations = (localProperties.getProperty("xmltoxib.conversions") ?: "")
                        .splitToSequence(File.pathSeparatorChar)
                        .filter { it.isNotBlank() }
                        .map { File(it) }
                        .filter { it.exists() }
                        .plus(sequenceOf(iosBase))
                        .plus(sequenceOf(target.projectDir))
                        .plus(localPodSpecRefs)
                    println("Checking for iOS view definitions at: ${allLocations.joinToString("\n")}")
                    allLocations
                }
                val translator = IosTranslator(
                    androidFolder = target.projectDir,
                    iosFolder = iosFolder,
                    iosModuleName = iosModuleName,
                    iosName = iosName,
                    replacementFolders = dependencies.toList()
                )
                translator.resources.all.forEach { s, androidValue -> println("$s: $androidValue") }
                println("assetsFolder: ${translator.project.assetsFolder}")
                println("layoutsFolder: ${translator.project.layoutsFolder}")
                println("swiftResourcesFolder: ${translator.project.swiftResourcesFolder}")
                println("baseFolderForLocalizations: ${translator.project.baseFolderForLocalizations}")
                translator()
            }
        }
    }
}