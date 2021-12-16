package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.android.*

fun Appendable.setToColor(color: AndroidColor?, controlView: String = "view", write: (color: String, state: String) -> Unit): Boolean {
    if(color == null) return false
    when(color){
        is AndroidColorLiteral -> write("UIColor(argb: 0x${color.value.android.drop(1)})", ".normal")
        is AndroidColorResource -> write("R.color.${color.name}", ".normal")
        is AndroidColorStateResource -> {
            appendLine("applyColor($controlView, R.color.${color.name}) { (c, s) in")
            write("c", "s")
            appendLine("}")
        }
    }
    return true
}

val AndroidColor.swift: String get() = when(this) {
    is AndroidColorLiteral -> "UIColor(red: ${value.redFloat}, green: ${value.greenFloat}, blue: ${value.blueFloat}, alpha: ${value.alphaFloat})"
    is AndroidColorResource -> "R.color.${name}"
    is AndroidColorStateResource -> "R.color.${name}"
}
//val AndroidDrawableResource.swiftDrawable: String get() = "R.drawable.${name.safeSwiftIdentifier()}"
//fun AndroidDrawableResource.swiftLayer(forView: String): String = "$swiftDrawable.makeLayer($forView)"
//val AndroidDrawableResource.swiftImage: String get() = "UIImage(named: \"$name\") ?? ${swiftLayer("view")}.toImage()"
val AndroidDimension.swift: String get() = when(this) {
    is AndroidNumber -> value.toString()
    is AndroidDimensionLiteral -> measurement.number.toString()
    is AndroidDimensionResource -> "R.dimen.$name"
}

fun String.safeSwiftIdentifier(): String = when(this){
    "associatedtype",
    "class",
    "deinit",
    "enum",
    "extension",
    "fileprivate",
    "func",
    "import",
    "init",
    "inout",
    "internal",
    "let",
    "open",
    "operator",
    "private",
    "protocol",
    "public",
    "rethrows",
    "static",
    "struct",
    "subscript",
    "typealias",
    "var",
    "break",
    "case",
    "continue",
    "default",
    "defer",
    "do",
    "else",
    "fallthrough",
    "for",
    "guard",
    "if",
    "in",
    "repeat",
    "return",
    "switch",
    "where",
    "while",
    "as",
    "Any",
    "catch",
    "false",
    "is",
    "nil",
    "super",
    "self",
    "Self",
    "throw",
    "throws",
    "true",
    "try" -> "`$this`"
    "description" -> "myDescription"
    else -> this
}
fun String.safeSwiftViewIdentifier(): String = when(this){
    "next" -> "nextView"
    else -> this.safeSwiftIdentifier()
}