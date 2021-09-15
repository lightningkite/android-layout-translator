package com.lightningkite.convertlayout.rules

data class ElementReplacement(
    val id: String,
    var classMappings: Map<String, String> = mapOf(),
    var attributes: Map<String, String> = mapOf(),
    var parent: String? = null,
    var template: Template? = null,
    var childRule: String? = null,
    var autoWrapFor: List<String>? = null,
    var insertChildrenAt: String? = null,
    override val debug: Boolean = false
): ReplacementRule {
    override val priority: Int get() = attributes.size
}

