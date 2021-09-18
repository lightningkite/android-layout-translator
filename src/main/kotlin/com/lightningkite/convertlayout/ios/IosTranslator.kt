package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.android.AndroidLayoutFile
import com.lightningkite.convertlayout.android.AndroidResources
import com.lightningkite.convertlayout.rules.Replacements
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
        replacementFolders: Iterable<File>
    ):this(
        project = IosProject(iosFolder, iosName),
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
    fun importOtherResources() {
        project.importResources(resources)
    }
    fun translate(layout: AndroidLayoutFile) {
        val instance = make()
        project.layoutsFolder
            .also { it.mkdirs() }
            .resolve(layout.name + "Xml.xib")
            .writeXml(instance.convertDocument(layout, layout.files.first().readXml()))
        project.layoutsFolder
            .also { it.mkdirs() }
            .resolve(layout.name + "Xml.swift")
            .writeText(instance.swiftFile(layout))
    }
    operator fun invoke() {
        importOtherResources()
        for(layout in resources.layouts.values) {
            translate(layout.layout.value)
        }
    }
}