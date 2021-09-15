package com.lightningkite.convertlayout.rules

data class Template(val parts: List<TemplatePart>) {
    constructor(string: String):this(Unit.run {
        val parts = ArrayList<TemplatePart>()
        var position = 0
        var inside = false
        while(true) {
            val next = string.indexOf('~', position)
            if(next == -1) {
                parts.add(TemplatePart.Text(string.substring(position + 1)))
                break
            } else {
                val section = string.substring(position + 1, next)
                if(inside){
                    parts.add(TemplatePart.Variable(section))
                } else {
                    parts.add(TemplatePart.Text(section))
                }
                inside = !inside
                position = next + 1
            }
        }
        parts
    })
    fun write(getter: (String)->String): String {
        return parts.joinToString("") {
            when(it) {
                is TemplatePart.Text -> it.content
                is TemplatePart.Variable -> getter(it.key)
            }
        }
    }

    override fun toString(): String = write { "~$it~" }
}
sealed interface TemplatePart {
    data class Text(val content: String): TemplatePart
    data class Variable(val key: String): TemplatePart
}