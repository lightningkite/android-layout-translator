package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.android.AndroidLayoutFile
import com.lightningkite.convertlayout.android.AndroidResources
import com.lightningkite.convertlayout.rules.Replacements
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
        val instance = make()
        project.layoutsFolder
            .also { it.mkdirs() }
            .resolve(layout.className + ".xib")
            .writeXml(instance.convertDocument(layout, layout.files.first().readXml()).also { it.documentElement.cleanBlanks() })
        project.layoutsFolder
            .also { it.mkdirs() }
            .resolve(layout.className + ".swift")
            .writeText(instance.swiftFile(layout))
    }
    operator fun invoke() {
        importResources(resources)
        for(layout in resources.layouts.values) {
            println("Translating ${layout.name}")
            translate(layout.layout.value)
        }
    }
}