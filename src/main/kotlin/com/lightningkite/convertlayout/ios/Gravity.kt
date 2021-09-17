package com.lightningkite.convertlayout.ios


enum class Align {
    START, CENTER, END
}
fun Align.orStretch(): AlignOrStretch = when(this) {
    Align.START -> AlignOrStretch.START
    Align.CENTER -> AlignOrStretch.CENTER
    Align.END -> AlignOrStretch.END
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
            "center" -> {
                horz = Align.CENTER
                vert = Align.CENTER
            }
        }
    }
    return Gravity(horz, vert, locale)
}