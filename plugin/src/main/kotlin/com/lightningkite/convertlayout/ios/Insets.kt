package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.android.AndroidDimension
import com.lightningkite.convertlayout.android.AndroidResources

data class Insets(
    var left: Double,
    var top: Double,
    var right: Double,
    var bottom: Double
) {
    val localeDependent: Boolean get() = true

    companion object {
        val zero = Insets(0.0, 0.0, 0.0, 0.0)
    }
    fun start(vertical: Boolean) = if(vertical) top else left
    fun end(vertical: Boolean) = if(vertical) bottom else right
    operator fun plus(other: Insets): Insets = Insets(
        left = left + other.left,
        top = top + other.top,
        right = right + other.right,
        bottom = bottom + other.bottom,
    )
    operator fun plusAssign(other: Insets): Unit {
        left += other.left
        top += other.top
        right += other.right
        bottom += other.bottom
    }
    operator fun minus(other: Insets): Insets = Insets(
        left = left - other.left,
        top = top - other.top,
        right = right - other.right,
        bottom = bottom - other.bottom,
    )
    operator fun minusAssign(other: Insets): Unit {
        left -= other.left
        top -= other.top
        right -= other.right
        bottom -= other.bottom
    }
    operator fun get(vertical: Boolean, end: Boolean, locale: Boolean = true) = if(vertical)
        if(end) bottom else top
    else
        if(end) right else left

    fun size(vertical: Boolean): Double = if(vertical) top + bottom else left + right
}
fun Map<String, String>.insets(prefix: String, resources: AndroidResources): Insets {
    fun num(key: String): Double? = this[key]?.let { resources.read(it) as AndroidDimension }?.measurement?.number
    val default = num(prefix)
    val horz = num("${prefix}Horizontal")
    val vert = num("${prefix}Vertical")
    return Insets(
        left = num("${prefix}Start") ?: num("${prefix}Left") ?: horz ?: default ?: 0.0,
        top = num("${prefix}Top") ?: vert ?: default ?: 0.0,
        right = num("${prefix}End") ?: num("${prefix}Right") ?: horz ?: default ?: 0.0,
        bottom = num("${prefix}Bottom") ?: vert ?: default ?: 0.0,
    )
}
