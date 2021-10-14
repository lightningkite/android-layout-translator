package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.xml.childElements
import com.lightningkite.convertlayout.xml.get
import com.lightningkite.convertlayout.xml.getOrPut
import org.w3c.dom.Element
import java.util.*
import kotlin.random.Random

private val idCharOptions = "0123456789QWERTYUIOPASDFGHJKLZXCVBNMqwertyuiopasdfghjklzxcvbnm"
private fun generateId(random: Random): String = buildString {
    repeat(3) { append(idCharOptions.random(random)) }
    append('-')
    repeat(2) { append(idCharOptions.random(random)) }
    append('-')
    repeat(3) { append(idCharOptions.random(random)) }
}
private fun generateId(type: String, random: Random): String = buildString {
    append(type)
    repeat(6) { append(idCharOptions.random(random)) }
}

private val cached = WeakHashMap<Element, Long>()
private fun getElementHash(
    element: Element
): Long = cached.getOrPut(element) {
    element["id"]?.let { return it.hashCode().toLong() }
    val parent = (element.parentNode as? Element) ?: return 7L
    return 31L * getElementHash(parent) + parent.childElements.indexOf(element)
}

fun Element.id(): String {
    return getOrPut("id") { generateId(tagName, Random(getElementHash(this))) }
}