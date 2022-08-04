package com.lightningkite.convertlayout.web

import com.fasterxml.jackson.annotation.JsonIgnore
import com.lightningkite.convertlayout.android.AndroidVariant
import com.lightningkite.convertlayout.util.camelCase
import com.lightningkite.convertlayout.xml.attributeMap
import com.lightningkite.convertlayout.xml.children
import com.lightningkite.convertlayout.xml.get
import com.lightningkite.convertlayout.xml.readXml
import org.w3c.dom.Element
import java.io.File

data class WebLayoutFile(
    val packageName: String,
    val name: String,
    val variants: Set<AndroidVariant>,
    val files: Set<File>,
    val bindings: Map<String, Hook>,
    val sublayouts: Map<String, SubLayout>
) {
    data class SubLayout(
        val name: String,
        val layout: String,
        val optional: Boolean = false
    ) {
        fun binding() = name + ": " + layout + (if(optional) " | null" else "")
        fun import(): String = """import { ${layout} } from './${layout}' """
    }

    data class Hook(
        val name: String,
        val baseType: String,
        val additionalParts: Map<String, String> = mapOf(),
        val optional: Boolean = false
    ) {
        fun binding() = if(additionalParts.isEmpty()) {
            name + ": " + baseType + (if(optional) " | null" else "")
        } else {
            name + ": " + baseType + " & {" + additionalParts.entries.joinToString(", ") {
                it.key + ": " + it.value
            } + "}" + (if(optional) " | null" else "")
        }
    }

    @get:JsonIgnore
    val className: String
        get() = name.capitalize().camelCase() + "Binding"

    companion object {
        fun combine(iter: Sequence<WebLayoutFile>): WebLayoutFile =
            WebLayoutFile(
                packageName = iter.first().packageName,
                name = iter.first().name,
                variants = iter.flatMap { it.variants.asSequence() }.toSet(),
                files = iter.flatMap { it.files }.toSet(),
                bindings = run {
                    (iter.flatMap { it.bindings.asSequence() }.associate { it.toPair() }).mapValues { (key, value) ->
                        if (iter.all { it.bindings[key] != null }) value
                        else value.copy(optional = true)
                    }
                },
                sublayouts = run {
                    (iter.flatMap { it.sublayouts.asSequence() }.associate { it.toPair() }).mapValues { (key, value) ->
                        if (iter.all { it.sublayouts[key] != null }) value
                        else value.copy(optional = true)
                    }
                }
            )
    }

    fun emit(): String {
        return """
            |import {inflateHtmlFile} from "@lightningkite/android-xml-runtime";
            |${variants.joinToString("\n|"){ "import html${it.camelCaseSuffix} from './${name}${it.suffix}.html'" }}
            |${sublayouts.values.distinctBy { it.layout }.joinToString("\n|    ") { it.import() }}
            |
            |//! Declares ${packageName}.databinding.${className}
            |export interface ${className} {
            |    root: HTMLElement
            |    ${bindings.values.joinToString("\n|    ") { it.binding() }}
            |    ${sublayouts.values.joinToString("\n|    ") { it.binding() }}
            |}
            |
            |export namespace ${className} {
            |   const variants = [${variants.sorted().joinToString { it.web }}]
            |   export function inflate(): ${className} {
            |       return inflateHtmlFile(variants, [${
            bindings.values.filter { it.additionalParts.isEmpty() }.map { "\"" + it.name + "\"" }.joinToString()
        }], {${
            bindings.values.filter { it.additionalParts.isNotEmpty() }.map { it.name + ": [" + it.additionalParts.map { "\"" + it.key + "\"" }.joinToString() + "]" }
                .joinToString()
        }}, {${
            sublayouts.values.map { it.name + ": " + it.layout + ".inflate" }.joinToString()
        }}) as ${className}
            |   }
            |}
            |
        """.trimMargin("|")
    }

    val AndroidVariant.web: String get() = """
        {
            html: html${camelCaseSuffix},
            widerThan: ${widerThan?.toString() ?: "undefined"}
        }
    """.trimIndent()
}
