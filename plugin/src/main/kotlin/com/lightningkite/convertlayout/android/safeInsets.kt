package com.lightningkite.convertlayout.android

import org.w3c.dom.Element


object SafePaddingFlags {
    const val NONE: Int = 0
    const val TOP: Int = 1
    const val RIGHT: Int = 2
    const val BOTTOM: Int = 4
    const val LEFT: Int = 8
    const val HORIZONTAL: Int = 10
    const val VERTICAL: Int = 5
    const val ALL: Int = 15
}

fun String.toSafePaddingFlags(): Int {
    var gravity = 0
    val parts = this.split('|')
    if(parts.contains("center"))
        gravity = gravity or SafePaddingFlags.ALL
    if(parts.contains("all"))
        gravity = gravity or SafePaddingFlags.ALL
    if(parts.contains("center_horizontal"))
        gravity = gravity or SafePaddingFlags.LEFT or SafePaddingFlags.RIGHT
    if(parts.contains("horizontal"))
        gravity = gravity or SafePaddingFlags.LEFT or SafePaddingFlags.RIGHT
    if(parts.contains("left"))
        gravity = gravity or SafePaddingFlags.LEFT
    if(parts.contains("right"))
        gravity = gravity or SafePaddingFlags.RIGHT
    if(parts.contains("center_vertical"))
        gravity = gravity or SafePaddingFlags.TOP or SafePaddingFlags.BOTTOM
    if(parts.contains("vertical"))
        gravity = gravity or SafePaddingFlags.TOP or SafePaddingFlags.BOTTOM
    if(parts.contains("top"))
        gravity = gravity or SafePaddingFlags.TOP
    if(parts.contains("bottom"))
        gravity = gravity or SafePaddingFlags.BOTTOM
    return gravity
}