package com.lightningkite.convertlayout.ios

import com.fasterxml.jackson.annotation.JsonIgnore
import com.lightningkite.convertlayout.util.camelCase
import com.lightningkite.convertlayout.xml.attributeMap
import com.lightningkite.convertlayout.xml.children
import com.lightningkite.convertlayout.xml.get
import com.lightningkite.convertlayout.xml.readXml
import org.w3c.dom.Element
import java.io.File

data class IosLayoutFile(
    val projectName: String,
    val name: String,
    val variants: Set<String>,
    val files: Set<File>,
    val bindings: Map<String, Hook>,
) {
    data class Hook(
        val name: String,
        val baseType: SwiftIdentifier,
        val optional: Boolean = false
    ) {
        fun declaration() = "@IBOutlet weak private var _$name: ${baseType.name}${if (optional) "?" else "!"}"
        fun access() = "public var $name: ${baseType.name}${if (optional) "?" else ""} { return _$name }"
    }

    @get:JsonIgnore
    val className: String
        get() = name.capitalize().camelCase() + "Binding"

    companion object {
        fun combine(iter: Sequence<IosLayoutFile>): IosLayoutFile =
            IosLayoutFile(
                projectName = iter.first().projectName,
                name = iter.first().name,
                variants = iter.flatMap { it.variants.asSequence() }.toSet(),
                files = iter.flatMap { it.files }.toSet(),
                bindings = run {
                    (iter.flatMap { it.bindings.asSequence() }.associate { it.toPair() }).mapValues { (key, value) ->
                        if (iter.all { it.bindings[key] != null }) value
                        else value.copy(optional = true)
                    }
                }
            )
    }

    fun emit(): String {
        return """
    |//
    |// ${className}.swift
    |// Created by Android Xml to iOS Xib Translator
    |//
    |
    |import XmlToXibRuntime
    |${bindings.values.mapNotNull { it.baseType.module }.filter { it != projectName }.toSet().joinToString("\n|") { "import $it" }}
    |
    |public class $className: XibView {
    |
    |    ${bindings.values.joinToString("\n|    ") { it.declaration() }}
    |    ${bindings.values.joinToString("\n|    ") { it.access() }}
    |
    |}
    """.trimMargin("|")
    }
}
