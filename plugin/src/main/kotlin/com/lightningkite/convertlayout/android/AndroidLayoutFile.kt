package com.lightningkite.convertlayout.android

import com.fasterxml.jackson.annotation.JsonIgnore
import com.lightningkite.convertlayout.util.camelCase
import com.lightningkite.convertlayout.xml.attributeMap
import com.lightningkite.convertlayout.xml.children
import com.lightningkite.convertlayout.xml.get
import com.lightningkite.convertlayout.xml.readXml
import org.w3c.dom.Element
import java.io.File

data class AndroidLayoutFile(
    val name: String,
    val variants: Map<String, File>
) {
    @get:JsonIgnore val className: String get() = name.capitalize().camelCase() + "Binding"
    companion object {
        fun parseAll(folder: File, resources: AndroidResources): Map<String, AndroidLayoutFile> {
            return folder.listFiles()!!.asSequence().filter { it.name.startsWith("layout") }
                .flatMap { it.listFiles()!!.asSequence() }
                .map { it.name }
                .distinct()
                .map { parseSet(folder, it, resources) }
                .associateBy { it.name }
        }

        fun parseSet(folder: File, filename: String, resources: AndroidResources): AndroidLayoutFile {
            return AndroidLayoutFile(filename.removeSuffix(".xml"), folder.listFiles()!!.asSequence().filter { it.name.startsWith("layout") }
                .map { it.resolve(filename) }
                .filter { it.exists() }
                .associate { it.parentFile.name.substringAfter("layout-", "") to it })
        }
    }

}
