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
        return other.priority - this.priority
    }
}