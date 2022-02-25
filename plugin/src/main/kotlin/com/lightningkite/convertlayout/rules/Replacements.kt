package com.lightningkite.convertlayout.rules

import com.fasterxml.jackson.databind.MapperFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.util.*
import kotlin.collections.HashMap

class Replacements() {

    companion object {
        val mapper = YAMLMapper().registerKotlinModule().enable(MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS)
    }

    val elementsByIdentifier: HashMap<String, ElementReplacement> = HashMap()
    val elements: HashMap<String, TreeSet<ElementReplacement>> = HashMap()
    val attributes: HashMap<String, TreeSet<AttributeReplacement>> = HashMap()

    fun getElement(
        elementName: String,
        attributes: Map<String, String>
    ): ElementReplacement? = elements[elementName]?.firstOrNull {
        if(it.debug) {
            println("Checking against rule $it")
        }
        it.attributes.entries.all { (key, value) ->
            val otherValue = attributes[key]
            value.split("&").all { part ->
                when {
                    part.startsWith("contains:") -> otherValue != null && otherValue.contains(part.substringAfter(':').trim())
                    part.startsWith("doesNotContain:") -> otherValue == null || !otherValue.contains(part.substringAfter(':').trim())
                    part.startsWith("not:") -> otherValue == null || otherValue != part.substringAfter(':').trim()
                    else -> when(part) {
                        "any", "set" -> otherValue != null
                        "unset" -> otherValue == null
                        else -> otherValue == value
                    }
                }
            }
        }
    }

    fun getAttribute(
        elementName: String,
        parentElementName: String,
        attributeName: String,
        attributeType: AttributeReplacement.ValueType2,
        rawValue: String
    ): AttributeReplacement? = attributes[attributeName]?.firstOrNull {
        val res = (attributeType in it.valueType)
                && (it.element == elementName)
                && (it.equalTo == null || it.equalTo == rawValue)
        if(it.debug) {
            println("Checking against rule $it")
            println("    ${attributeType} in ${it.valueType}")
            println("    ${it.element} == ${elementName}")
            println("    ${it.equalTo} == null or ${rawValue}")
            println("    ${it.parentElement} == null or ${parentElementName}")
            println("    ${res}")
        }
        res
    }

    fun getAttributeOptionsForStyle(
        attributeName: String,
        attributeType: AttributeReplacement.ValueType2,
        rawValue: String
    ): Sequence<AttributeReplacement> = attributes[attributeName]?.asSequence()?.filter {
        val res = (attributeType in it.valueType)
                && (it.equalTo == null || it.equalTo == rawValue)
        if(it.debug) {
            println("Checking against rule $it")
            println("    ${attributeType} in ${it.valueType}")
            println("    ${it.equalTo} == null or ${rawValue}")
            println("    ${res}")
        }
        res
    } ?: sequenceOf()

    private val canBeInStylesheetCache = hashMapOf<String, Boolean>("android:background" to false)
    fun canBeInStyleSheet(attributeName: String): Boolean = canBeInStylesheetCache.getOrPut(attributeName) {
        val matching = attributes[attributeName] ?: return@getOrPut false

        // all simple element accesses that can be reproduced in CSS
        if(!matching.all { it.rules.keys.all { it.all { it.isLetterOrDigit() || it == '/' } } }) return@getOrPut false
        // No ifContains rules that are dependent on knowing other values
        if(!matching.all { it.rules.values.all { it.ifContains?.keys?.none { it.contains('=') } != false } }) return@getOrPut false

        return@getOrPut true
    }

    operator fun plusAssign(item: ReplacementRule) {
        if(item.debug){
            println("Debugging rule $item")
        }
        when (item) {
            is ElementReplacement -> {
                elements.getOrPut(item.id) { TreeSet() }.add(item)
                elementsByIdentifier[item.caseIdentifier ?: item.id] = item
            }
            is AttributeReplacement -> attributes.getOrPut(item.id) { TreeSet() }.add(item)
        }
    }

    operator fun plusAssign(yaml: String) {
        mapper.readValue<List<ReplacementRule>>(yaml).filterNotNull().forEach {
            this += it
        }
    }

    operator fun plusAssign(yaml: File) {
        mapper.readValue<List<ReplacementRule>>(yaml).filterNotNull().forEach {
            this += it
        }
    }
}

