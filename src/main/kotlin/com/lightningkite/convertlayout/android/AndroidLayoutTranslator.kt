package com.lightningkite.convertlayout.android

import com.lightningkite.convertlayout.xml.*
import com.lightningkite.convertlayout.rules.*
import org.w3c.dom.Element

abstract class AndroidLayoutTranslator(val replacements: Replacements, val resources: AndroidResources) {
    open fun convertElement(owner: Element, element: Element): Element {
        val allAttributes =
            element.attributeMap + (element["style"]?.let { resources.read(it) as? AndroidStyle }?.map ?: mapOf())
        val firstRule = replacements.getElement(element.tagName, allAttributes)
            ?: throw IllegalArgumentException("No rule ${element.tagName} found")
        val rules = generateSequence(firstRule) {
            it.parent?.let { replacements.getElement(it, allAttributes) }
        }.toList()
        val newElement = owner.appendFragment(rules.asSequence().mapNotNull { it.template }.first().write {element.getPath(it)})

        // Handle children
        rules.mapNotNull { it.insertChildrenAt }.firstOrNull()?.let { path ->
            val target =
                newElement.xpathElement(path) ?: throw IllegalArgumentException("No element found for path '$path'")
            val rule = rules.mapNotNull { it.childRule }.firstOrNull()
            handleChildren(rules, rule, element, newElement, target)
        }

        // Handle attributes
        for ((key, raw) in allAttributes) {
            handleAttribute(element, newElement, key, raw)
        }
        return newElement
    }

    open fun handleChildren(
        rules: List<ElementReplacement>,
        childAddRule: String?,
        sourceElement: Element,
        resultElement: Element,
        target: Element
    ) {
        for (child in sourceElement.children.mapNotNull { it as? Element }) {
            convertElement(target, child)
        }
    }

    open fun handleAttribute(
        element: Element,
        newElement: Element,
        key: String,
        raw: String
    ) {
        val value = resources.read(raw)
        val attributeRule = replacements.getAttribute(element.tagName, key, value.type) ?: return
        for ((path, sub) in attributeRule.rules) {
            val target = newElement.xpathElement(path)!!
            if (sub.css.isNotEmpty()) {
                val broken = target.getAttribute("style").split(';')
                    .associate { it.substringBefore(':').trim() to it.substringAfter(':').trim() }
                val resultCss = broken + sub.css.mapValues { it.value.write { value.getPath(it) } }
                target.setAttribute("style", resultCss.entries.joinToString("; ") { "${it.key}: ${it.value}" })
            }
            for (toAppend in sub.append) {
                val content = toAppend.write { value.getPath(it) }
                if (content.startsWith('<'))
                    target.appendFragment(content)
                else
                    target.appendText(content)
            }
            for ((attKey, attTemplate) in sub.attribute) {
                target.setAttribute(attKey, attTemplate.write { value.getPath(it) })
            }
        }
    }
}
