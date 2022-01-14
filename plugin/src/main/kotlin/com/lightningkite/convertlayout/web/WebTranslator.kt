package com.lightningkite.convertlayout.web

import com.lightningkite.convertlayout.android.AndroidLayoutFile
import com.lightningkite.convertlayout.android.AndroidResources
import com.lightningkite.convertlayout.rules.Replacements
import com.lightningkite.convertlayout.util.forEachBetween
import com.lightningkite.convertlayout.xml.*
import java.io.File

data class WebTranslator(
    val project: WebProject,
    val replacements: Replacements,
    val resources: AndroidResources
) {
    constructor(
        androidFolder: File,
        webFolder: File,
        webName: String,
        replacementFolders: Iterable<File>
    ):this(
        project = WebProject(webFolder, webName),
        replacements = Replacements().apply {
            replacementFolders
                .also { println("Searching $it for replacements...") }
                .asSequence()
                .flatMap { it.walkTopDown() }
                .filter { it.name.endsWith(".html.yaml") }
                .forEach { println("Loaded replacement file $it"); this += it }
        },
        resources = AndroidResources().apply {
            this.parse(androidFolder.resolve("src/main/res"))
        }
    )

    private fun make() = WebLayoutTranslatorForFile(project, replacements, resources)
    fun translate(layout: AndroidLayoutFile) {
        val instance = make()
        project.layoutsFolder
            .also { it.mkdirs() }
            .resolve(layout.name + ".html")
            .writeXml(instance.convertDocument(layout, layout.files.first().readXml()).also { it.documentElement.cleanBlanks() })
        project.layoutsFolder
            .also { it.mkdirs() }
            .resolve(layout.className + ".ts")
            .writeText(instance.tsFile(layout))
    }
    operator fun invoke() {
        importResources()
        for(layout in resources.layouts.values) {
            println("Translating ${layout.name}")
            translate(layout.layout.value)
        }
    }

    internal fun swaml() {
        println("---")
        for(k in replacements.elements.keys) {
            val e = replacements.getElement(k, mapOf()) ?: continue
            val template = (e.template ?: continue).toString()
                .trim()
            val element = try { template.readXml().documentElement } catch(e:Exception) {
                println("Failed to read $template")
                continue
            }
            val type = element.walkElements()
                .filter { it.hasAttribute("id") }
                .toList()
                .takeUnless { it.isEmpty() }
                ?.let {
                    buildString {
                        append('{')
                        it.forEachBetween(
                            forItem = { i ->
                                append(i["id"])
                                append(": ")
                                append(WebLayoutTranslatorForFile.elementMap[i.tagName])
                            },
                            between = { append(", ") }
                        )
                        append('}')
                    }
                } ?: WebLayoutTranslatorForFile.elementMap[element.tagName] ?: continue
            if(k.first().isUpperCase())
                println("- id: android.widget.$k")
            else
                println("- id: $k")
            println("  type: type")
            println("  template: $type")
            println()
        }
    }
}