package com.lightningkite.convertlayout.web

import com.lightningkite.convertlayout.android.AndroidLayoutFile
import com.lightningkite.convertlayout.android.AndroidResources
import com.lightningkite.convertlayout.rules.Replacements
import com.lightningkite.convertlayout.xml.cleanBlanks
import com.lightningkite.convertlayout.xml.readXml
import com.lightningkite.convertlayout.xml.writeXml
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
                .asSequence()
                .flatMap { it.walkTopDown() }
                .filter { it.name.endsWith(".html.yaml") }
                .forEach { this += it }
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
            .resolve(layout.className + ".xib")
            .writeXml(instance.convertDocument(layout, layout.files.first().readXml()).also { it.documentElement.cleanBlanks() })
        project.layoutsFolder
            .also { it.mkdirs() }
            .resolve(layout.className + ".swift")
            .writeText(instance.tsFile(layout))
    }
    operator fun invoke() {
        importResources()
        for(layout in resources.layouts.values) {
            println("Translating ${layout.name}")
            translate(layout.layout.value)
        }
    }
}