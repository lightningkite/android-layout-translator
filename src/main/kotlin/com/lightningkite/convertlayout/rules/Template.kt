package com.lightningkite.convertlayout.rules

import com.fasterxml.jackson.annotation.JsonCreator

data class Template(val parts: List<TemplatePart>) {
    @JsonCreator constructor(string: String):this(Unit.run {
        val substring = string.trim()
        val parts = ArrayList<TemplatePart>()
        var position = 0
        var inside = false
        while(true) {
            val next = substring.indexOf('~', position)
            if(next == -1) {
                parts.add(TemplatePart.Text(substring.substring(position)))
                break
            } else {
                val section = substring.substring(position, next)
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