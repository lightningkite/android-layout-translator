package com.lightningkite.convertlayout.android

import com.lightningkite.convertlayout.xml.*
import com.lightningkite.convertlayout.rules.*
import com.lightningkite.convertlayout.util.DeferMap
import org.w3c.dom.Element

abstract class AndroidLayoutTranslator(val replacements: Replacements, val resources: AndroidResources) {
    val Element.allAttributes: Map<String, String>
        get() = DeferMap(
            listOfNotNull(
                attributeMap,
                this["style"]?.let { resources.read(it) as? AndroidStyle }?.chainedMap,
                (resources.read("@style/AppTheme") as? AndroidStyle)?.chainedMap,
                this["layout"]?.let { resources.read(it) as? AndroidLayoutResource }?.let {
                    it.layout.value.files.first().readXml().documentElement.allAttributes
//                        .also { println("Borrowed ${it.entries.joinToString() { it.key + ": " + it.value }} from source") }
                }
            )
        )

    open fun convertElement(owner: Element, sourceElement: Element): Element {
        val allAttributes = sourceElement.allAttributes
        val firstRule = replacements.getElement(sourceElement.tagName, allAttributes)
            ?: throw IllegalArgumentException("No rule ${sourceElement.tagName} found")
        val rules = generateSequence(firstRule) {
            it.parent?.let { replacements.elementsByIdentifier[it] }
        }.toList()
        return convertElement(owner, rules, sourceElement, allAttributes)
    }

    abstract fun getProjectWide(key: String): String?

    open fun convertElement(
        destOwner: Element,
        rules: List<ElementReplacement>,
        sourceElement: Element,
        allAttributes: Map<String, String>
    ): Element {
        val newElement = destOwner.appendFragment(rules.asSequence().mapNotNull { it.template }.first()
            .write { getProjectWide(it) ?: with(resources) { sourceElement.getPath(it) } })

        // Handle children
        handleChildren(rules, newElement, sourceElement)

        // Handle attributes
        handleAttributes(rules, allAttributes, sourceElement, newElement)
        return newElement
    }

    open fun handleChildren(
        rules: List<ElementReplacement>,
        destElement: Element,
        sourceElement: Element
    ) {
//        if (sourceElement.childElements.none()) return
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
        rules: List<ElementReplacement>,
        allAttributes: Map<String, String>,
        sourceElement: Element,
        destElement: Element
    ) {
        for ((key, raw) in allAttributes) {
            if(key.startsWith("tools:") && !allAttributes.containsKey("android:" + key.substringAfter(':'))) {
                handleAttribute(rules, sourceElement, destElement, allAttributes, "android:" + key.substringAfter(':'), raw)
            } else {
                handleAttribute(rules, sourceElement, destElement, allAttributes, key, raw)
            }
        }
    }

    open fun handleAttribute(
        rules: List<ElementReplacement>,
        sourceElement: Element,
        destElement: Element,
        allAttributes: Map<String, String>,
        key: String,
        raw: String
    ) {
        val value = resources.read(raw)
        val attributeRule = rules.asSequence()
            .mapNotNull { replacements.getAttribute(it.caseIdentifier ?: it.id, key, AttributeReplacement.ValueType2[value::class], raw) }
            .firstOrNull() ?: return
        handleAttribute(allAttributes, attributeRule, destElement, value)
    }

    open fun handleAttribute(
        allAttributes: Map<String, String>,
        attributeRule: AttributeReplacement,
        destElement: Element,
        value: AndroidValue
    ) = with(resources) {
        for((path, sub) in attributeRule.rules) {
            sub(
                allAttributes = allAttributes,
                value = value,
                resources = resources,
                action = {
                    val target = destElement.xpathElementOrCreate(path)
                    if(!replacements.canBeInStyleSheet(attributeRule.id)) {
                        if (sub.css.isNotEmpty()) {
                            val broken = target.getAttribute("style").split(';')
                                .associate { it.substringBefore(':').trim() to it.substringAfter(':').trim() }
                            val resultCss =
                                broken + sub.css.mapValues { it.value?.write { getProjectWide(it) ?: value.getPath(it) } }
                            target.setAttribute("style", resultCss.entries.joinToString("; ") { "${it.key}: ${it.value}" })
                        }
                        if(sub.classes.isNotEmpty()) {
                            target["class"] = ((target["class"]?.split(' ') ?: listOf()) + sub.classes).joinToString(" ")
                        }
                    }
                    for (toAppend in sub.append) {
                        val content = toAppend.write { getProjectWide(it) ?: value.getPath(it) }
                        if (content.startsWith('<'))
                            target.appendFragment(content)
                        else
                            target.appendText(content)
                    }
                    for ((attKey, attTemplate) in sub.attribute) {
                        attTemplate?.write { getProjectWide(it) ?: value.getPath(it) }?.let {
                            target.setAttribute(attKey, it)
                        } ?: target.removeAttribute(attKey)
                    }
                }
            )
        }
    }
}
