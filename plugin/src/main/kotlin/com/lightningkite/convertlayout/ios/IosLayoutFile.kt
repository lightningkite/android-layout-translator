package com.lightningkite.convertlayout.ios

import com.fasterxml.jackson.annotation.JsonIgnore
import com.lightningkite.convertlayout.android.AndroidVariant
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
    val variants: Set<AndroidVariant>,
    val files: Set<File>,
    val bindings: Map<String, Hook>,
) {
    data class Hook(
        val name: String,
        val baseType: SwiftIdentifier,
        val optional: Boolean = false
    ) {
        fun declaration() = "@IBOutlet weak private var _$name: ${baseType.name}${if (optional) "?" else "!"}"
        fun access() =
            "public var ${name.safeSwiftViewIdentifier()}: ${baseType.name}${if (optional) "?" else ""} { return _$name }"
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
    |${
            bindings.values.mapNotNull { it.baseType.module }.filter { it != projectName }.toSet()
                .joinToString("\n|") { "import $it" }
        }
    |
    |public class $className: XibView {
    |
    |    ${bindings.values.joinToString("\n|    ") { it.declaration() }}
    |    ${bindings.values.joinToString("\n|    ") { it.access() }}
    |    
    |    public override func selectNibName() -> String {
    |       ${
            variants.sorted().filter { it.parts.size > 0 }.joinToString(" else ") {
                """if ${it.iosCondition} { 
                    |            return "${className}${it.suffix}"
                    |        }""".trimMargin("|")
            }
        }
    |        return "$className"
    |    }
    |
    |}
    """.trimMargin("|")
    }

    val AndroidVariant.iosCondition: String?
        get() = listOfNotNull(
            widerThan?.let { "UIScreen.main.bounds.width > $it" },
            tallerThan?.let { "UIScreen.main.bounds.height > $it" },
            landscape?.let { if (it) "UIScreen.main.bounds.width > UIScreen.main.bounds.height" else "UIScreen.main.bounds.width < UIScreen.main.bounds.height" }
        ).takeUnless { it.isEmpty() }?.joinToString(" && ")
}
