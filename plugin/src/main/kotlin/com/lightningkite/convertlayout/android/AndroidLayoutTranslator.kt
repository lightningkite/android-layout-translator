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
                this["layout"]?.let { resources.read(it) as? AndroidLayoutResource }?.let {
                    it.layout.value.files.first().readXml().documentElement.allAttributes
//                        .also { println("Borrowed ${it.entries.joinToString() { it.key + ": " + it.value }} from source") }
                }
            )
        )

    fun HasGet.getPath(path: String): String = (this as Any).getPath(path)
    fun Element.getPath(path: String): String = (this as Any).getPath(path)
    private fun Any.getPath(path: String): String {
        var current: Any? = this
        for (part in path.split('.')) {
            while (current is Lazy<*>) {
                current = current.value
            }
            current = when (current) {
                is HasGet -> current[part]
                is Element -> when(part) {
                    "halfSize" -> (current.findSize()!! / 2).toString()
                    else -> current[part]?.let { resources.read(it) }
                }
                else -> return current.toString()
            }
        }
        while (current is Lazy<*>) {
            current = current.value
        }
        return current.toString()
    }
    private fun Element.findSize(): Double? {
        return this["android:layout_width"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number
            ?: this["android:layout_height"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number
            ?: (this.parentNode as? Element)?.findSize()?.let {
                it - (
                        this["android:padding"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_margin"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:paddingLeft"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_marginLeft"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:paddingRight"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_marginRight"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:paddingStart"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_marginStart"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:paddingEnd"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_marginEnd"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:paddingTop"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_marginTop"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:paddingBottom"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_marginBottom"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:paddingHorizontal"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_marginHorizontal"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:paddingVertical"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_marginVertical"]?.let { resources.read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: 0.0
                        )
            }
    }

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
            .write { getProjectWide(it) ?: sourceElement.getPath(it) })

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
        if (sourceElement.childElements.none()) return
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
                handleAttribute(rules, sourceElement, destElement, "android:" + key.substringAfter(':'), raw)
            } else {
                handleAttribute(rules, sourceElement, destElement, key, raw)
            }
        }
    }

    open fun handleAttribute(
        rules: List<ElementReplacement>,
        sourceElement: Element,
        destElement: Element,
        key: String,
        raw: String
    ) {
        val value = resources.read(raw)
        val attributeRule = rules.asSequence()
            .mapNotNull { replacements.getAttribute(it.caseIdentifier ?: it.id, key, AttributeReplacement.ValueType2[value::class], raw) }
            .firstOrNull() ?: return
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
                val resultCss =
                    broken + sub.css.mapValues { it.value.write { getProjectWide(it) ?: value.getPath(it) } }
                target.setAttribute("style", resultCss.entries.joinToString("; ") { "${it.key}: ${it.value}" })
            }
            for (toAppend in sub.append) {
                val content = toAppend.write { getProjectWide(it) ?: value.getPath(it) }
                if (content.startsWith('<'))
                    target.appendFragment(content)
                else
                    target.appendText(content)
            }
            for ((attKey, attTemplate) in sub.attribute) {
                target.setAttribute(attKey, attTemplate.write { getProjectWide(it) ?: value.getPath(it) })
            }
            sub.ifContains?.let { ifContains ->
                val raw = (value as AndroidString).value
                for (entry in ifContains) {
                    if (entry.key in raw.split('|')) {
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
