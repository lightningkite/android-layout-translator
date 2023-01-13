package com.lightningkite.convertlayout.ios


enum class Align {
    START, CENTER, END
}
fun Align.orStretch(): AlignOrStretch = when(this) {
    Align.START -> AlignOrStretch.START
    Align.CENTER -> AlignOrStretch.CENTER
    Align.END -> AlignOrStretch.END
}
fun AlignOrStretch.align(): Align = when(this) {
    AlignOrStretch.START -> Align.START
    AlignOrStretch.CENTER -> Align.CENTER
    AlignOrStretch.END -> Align.END
    else -> Align.START
}
enum class AlignOrStretch {
    START, CENTER, END, STRETCH
}

data class Gravity(
    val horizontal: Align = Align.START,
    val vertical: Align = Align.START,
    val localeDependent: Boolean = true
) {
    operator fun get(checkVertical: Boolean): Align = if(checkVertical) vertical else horizontal
}

fun String.toGravity(): Gravity {
    var horz: Align = Align.START
    var vert: Align = Align.START
    var locale = true
    for (part in this.split('|')) {
        when (part) {
            "left" -> horz = Align.START
            "start" -> { locale = true; horz = Align.START }
            "right" -> horz = Align.END
            "end" -> { locale = true; horz =Align.END }
            "center_horizontal"-> horz = Align.CENTER
            "top" -> vert = Align.START
            "bottom" -> vert = Align.END
            "center_vertical" -> vert = Align.CENTER
            "center", "all" -> {
                horz = Align.CENTER
                vert = Align.CENTER
            }
        }
    }
    return Gravity(horz, vert, locale)
}


data class SystemEdges(
    var top: Boolean = false,
    var bottom: Boolean = false,
    var left: Boolean = false,
    var right: Boolean = false,
    var start: Boolean = false,
    var end: Boolean = false
) {
    operator fun plusAssign(other: SystemEdges) {
        top = top || other.top
        bottom = bottom || other.bottom
        left = left || other.left
        right = right || other.right
        start = start || other.start
        end = end || other.end
    }

    operator fun times(other: SystemEdges): SystemEdges = SystemEdges(
        top = this.top && other.top,
        bottom = this.bottom && other.bottom,
        left = this.left && other.left,
        right = this.right && other.right,
        start = this.start && other.start,
        end = this.end && other.end,
    )

    companion object {
        val default = SystemEdges()
    }
}
fun String.toSystemEdges(): SystemEdges {
    val result = SystemEdges()
    for (part in this.split('|')) {
        when (part) {
            "left" -> result.left = true
            "start" -> result.start = true
            "right" -> result.right = true
            "end" -> result.end = true
            "center_horizontal"-> {
                result.start = true
                result.end = true
                result.left = true
                result.right = true
            }
            "top" -> result.top = true
            "bottom" -> result.bottom = true
            "center_vertical" -> {
                result.top = true
                result.bottom = true
            }
            "center", "all" -> {
                result.start = true
                result.end = true
                result.left = true
                result.right = true
                result.top = true
                result.bottom = true
            }
        }
    }
    return result
}

