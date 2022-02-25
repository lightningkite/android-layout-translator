package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.android.AndroidLayoutFile
import com.lightningkite.convertlayout.android.AndroidResources
import com.lightningkite.convertlayout.rules.Replacements
import com.lightningkite.convertlayout.web.WebLayoutFile
import com.lightningkite.convertlayout.web.WebLayoutTranslatorForFile
import com.lightningkite.convertlayout.xml.cleanBlanks
import com.lightningkite.convertlayout.xml.readXml
import com.lightningkite.convertlayout.xml.writeXml
import java.io.File

data class IosTranslator(
    val project: IosProject,
    val replacements: Replacements,
    val resources: AndroidResources
) {
    constructor(
        androidFolder: File,
        iosFolder: File,
        iosName: String,
        iosModuleName: String = iosName,
        replacementFolders: Iterable<File>
    ):this(
        project = IosProject(iosFolder, iosName, iosModuleName),
        replacements = Replacements().apply {
            replacementFolders
                .asSequence()
                .flatMap { it.walkTopDown() }
                .filter { it.name.endsWith(".xib.yaml") }
                .forEach { this += it }
        },
        resources = AndroidResources().apply {
            this.parse(androidFolder.resolve("src/main/res"))
        }
    )

    private fun make() = IosLayoutTranslatorForFile(project, replacements, resources)
    fun translate(layout: AndroidLayoutFile) {
        layout.variants.entries.map {
            val translator = IosLayoutTranslatorForFile(project, replacements, resources)
            val htmlFile = project.layoutsFolder
                .also { it.mkdirs() }
                .resolve(layout.className + (if(it.key.isBlank()) "" else "-${it.key}") + ".xib")
            translator
                .convertDocument(layout, it.value.readXml())
                .also { it.documentElement.cleanBlanks() }
                .let { htmlFile.writeXml(it) }
            translator.swiftFile(layout.name, htmlFile, it.key.takeUnless { it.isBlank() })
        }.asSequence().let { IosLayoutFile.combine(it) }.let {
            project.layoutsFolder
                .also { it.mkdirs() }
                .resolve(layout.className + ".swift")
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