package com.lightningkite.convertlayout.web

import com.lightningkite.convertlayout.android.*
import com.lightningkite.convertlayout.rules.AttributeReplacement
import com.lightningkite.convertlayout.rules.ElementReplacement
import com.lightningkite.convertlayout.rules.Replacements
import com.lightningkite.convertlayout.util.camelCase
import com.lightningkite.convertlayout.xml.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.min

internal class WebLayoutTranslatorForFile(
    val project: WebProject,
    replacements: Replacements,
    resources: AndroidResources
) : AndroidLayoutTranslator(replacements, resources) {

    override fun getProjectWide(key: String): String? = when (key) {
        "projectName" -> project.name
        else -> null
    }

    fun convertDocument(layout: AndroidLayoutFile, readXml: Document): Document {
        TODO("Not yet implemented")
    }

    fun tsFile(
        layout: AndroidLayoutFile
    ): String = TODO()

}
