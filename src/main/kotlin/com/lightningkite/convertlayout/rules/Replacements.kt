package com.lightningkite.convertlayout.rules

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File
import java.util.*
import kotlin.collections.HashMap
import kotlin.collections.HashSet

class Replacements() {

    companion object {
        val mapper = YAMLMapper().registerKotlinModule()
    }

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
            when(value) {
                "any", "set" -> otherValue != null
                "unset" -> otherValue == null
                else -> otherValue == value
            }
        }
    }

    fun getAttribute(
        elementName: String,
        attributeName: String,
        attributeType: AttributeReplacement.ValueType
    ): AttributeReplacement? = attributes[attributeName]?.firstOrNull {
        val res = (it.valueType == attributeType)
                && (it.element == null || it.element == elementName)
        if(it.debug) {
            println("Checking against rule $it")
            println("    ${it.valueType} == ${attributeType}")
            println("    ${it.element} == ${elementName}")
            println("    ${res}")
        }
        res
    }


    operator fun plusAssign(item: ReplacementRule) {
        if(item.debug){
            println("Debugging rule $item")
        }
        when (item) {
            is ElementReplacement -> elements.getOrPut(item.id) { TreeSet() }.add(item)
            is AttributeReplacement -> attributes.getOrPut(item.id) { TreeSet() }.add(item)
        }
    }

    operator fun plusAssign(yaml: String) {
        mapper.readValue<List<ReplacementRule>>(yaml).forEach {
            this += it
        }
    }

    operator fun plusAssign(yaml: File) {
        mapper.readValue<List<ReplacementRule>>(yaml).forEach {
            this += it
        }
    }
}

