package com.lightningkite.convertlayout.web

import com.lightningkite.convertlayout.xml.get
import com.lightningkite.convertlayout.xml.set
import org.w3c.dom.Element

//data class Style(
//    val selector: String,
//    val values: Map<String, String>,
//    val inherits:
//)

class CssStyle(val on: Element) {
    val map = on["style"]?.split(";")?.associateTo(LinkedHashMap()) { s -> s.substringBefore(':').trim() to s.substringAfter(':').trim() } ?: LinkedHashMap()
    init {
        map.remove("")
    }
    operator fun get(key: String) : String? = map[key]
    operator fun set(key: String, value: String) {
        map[key] = value
        on["style"] = map.entries.joinToString(";") { "${it.key}: ${it.value}" }
    }
    fun remove(key: String) {
        map.remove(key)
        on["style"] = map.entries.joinToString(";") { "${it.key}: ${it.value}" }
    }
    fun multi(action: (MutableMap<String, String>)->Unit) {
        action(map)
        on["style"] = map.entries.joinToString(";") { "${it.key}: ${it.value}" }
    }
}
val Element.css: CssStyle get() = CssStyle(this)