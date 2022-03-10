package com.lightningkite.convertlayout.web

import com.lightningkite.convertlayout.android.AndroidLayoutFile
import com.lightningkite.convertlayout.android.AndroidResources
import com.lightningkite.convertlayout.rules.Replacements
import com.lightningkite.convertlayout.util.forEachBetween
import com.lightningkite.convertlayout.util.walkZip
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
                .flatMap { it.walkZip() }
                .filter { it.name.endsWith(".html.yaml") }
                .forEach { this += it }
        },
        resources = AndroidResources().apply {
            this.parse(androidFolder.resolve("src/main/res"))
        }
    )

    fun translate(layout: AndroidLayoutFile) {
        layout.variants.entries.map {
            val translator = WebLayoutTranslatorForFile(project, replacements, resources)
            val htmlFile = project.layoutsFolder
                .also { it.mkdirs() }
                .resolve(layout.name + (if(it.key.isBlank()) "" else "-${it.key}") + ".html")
            translator
                .convertDocument(layout, it.value.readXml())
                .also { it.documentElement.cleanBlanks() }
                .let { htmlFile.writeXml(it) }
            translator.tsFile(layout.name, htmlFile, it.key.takeUnless { it.isBlank() })
        }.asSequence().let { WebLayoutFile.combine(it) }.let {
            project.layoutsFolder
                .also { it.mkdirs() }
                .resolve(layout.className + ".ts")
                .writeText(it.emit())
        }
    }
    operator fun invoke() {
        importResources()
        for(layout in resources.layouts.values) {
            println("Translating ${layout.name}")
            translate(layout.layout.value)
        }
    }

}