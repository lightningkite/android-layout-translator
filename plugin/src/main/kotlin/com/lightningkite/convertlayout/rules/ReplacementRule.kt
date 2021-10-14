package com.lightningkite.convertlayout.rules

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type"
)
@JsonSubTypes(
    JsonSubTypes.Type(
        ElementReplacement::class,
        name = "element"
    ),
    JsonSubTypes.Type(
        AttributeReplacement::class,
        name = "attribute"
    )
)
interface ReplacementRule: Comparable<ReplacementRule> {
    val debug: Boolean
    val priority: Int
    override fun compareTo(other: ReplacementRule): Int {
        val diff = other.priority - this.priority
        if(diff != 0) return diff
        if(this == other) return 0
        val diff2 = System.identityHashCode(other) - System.identityHashCode(this)
        if(diff2 != 0) return diff2
        return 1
    }
}