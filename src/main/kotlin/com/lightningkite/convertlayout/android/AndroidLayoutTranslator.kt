package com.lightningkite.convertlayout.android

import com.lightningkite.convertlayout.xml.*
import com.lightningkite.convertlayout.rules.*
import com.lightningkite.convertlayout.util.addLazy
import org.w3c.dom.Element

abstract class AndroidLayoutTranslator(val replacements: Replacements, val resources: AndroidResources) {
    val Element.allAttributes: Map<String, String> get() = attributeMap addLazy (this["style"]?.let { resources.read(it) as? AndroidStyle }?.map ?: mapOf())

    open fun convertElement(owner: Element, sourceElement: Element): Element {
        val allAttributes = sourceElement.allAttributes
        val firstRule = replacements.getElement(sourceElement.tagName, allAttributes)
            ?: throw IllegalArgumentException("No rule ${sourceElement.tagName} found")
        val rules = generateSequence(firstRule) {
            it.parent?.let { replacements.getElement(it, allAttributes) }
        }.toList()
        return convertElement(owner, rules, sourceElement, allAttributes)
    }

    open fun convertElement(
        destOwner: Element,
        rules: List<ElementReplacement>,
        sourceElement: Element,
        allAttributes: Map<String, String>
    ): Element {
        val newElement = destOwner.appendFragment(rules.asSequence().mapNotNull { it.template }.first().write { sourceElement.getPath(it) })

        // Handle children
        handleChildren(rules, newElement, sourceElement)

        // Handle attributes
        handleAttributes(allAttributes, sourceElement, newElement)
        return newElement
    }

    open fun handleChildren(
        rules: List<ElementReplacement>,
        destElement: Element,
        sourceElement: Element
    ) {
        if(sourceElement.childElements.none()) return
        rules.mapNotNull { it.insertChildrenAt }.firstOrNull()?.let { path ->
            val target =
                destElement.xpathElementOrCreate(path)
            val rule = rules.mapNotNull { it.childRule }.firstOrNull()
            handleChildren(rules, rule, sourceElement, destElement, target)
        }
    }

    open fun handleChildren(
        rules: List<ElementReplacement>,
        childAddRule: String?,
        sourceElement: Element,
        destElement: Element,
        target: Element
    ) {
        for (child in sourceElement.children.mapNotNull { it as? Element }) {
            convertElement(target, child)
        }
    }

    open fun handleAttributes(
        allAttributes: Map<String, String>,
        sourceElement: Element,
        destElement: Element
    ) {
        for ((key, raw) in allAttributes) {
            handleAttribute(sourceElement, destElement, key, raw)
        }
    }

    open fun handleAttribute(
        sourceElement: Element,
        destElement: Element,
        key: String,
        raw: String
    ) {
        val value = resources.read(raw)
        val attributeRule = replacements.getAttribute(sourceElement.tagName, key, value.type, raw) ?: return
        handleAttribute(attributeRule, destElement, value)
    }

    open fun handleAttribute(
        attributeRule: AttributeReplacement,
        destElement: Element,
        value: AndroidValue
    ) {
        fun subrule(path: String, sub: AttributeReplacement.SubRule) {
            val target = destElement.xpathElementOrCreate(path)
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
            sub.ifContains?.let { ifContains ->
                val raw = (value as AndroidStringValue).value
                for(entry in ifContains) {
                    if(entry.key in raw) {
                        subrule(path, entry.value)
                    }
                }
            }
        }
        for ((path, sub) in attributeRule.rules) {
            subrule(path, sub)
        }
    }
}
