package com.lightningkite.convertlayout.rules

data class ElementReplacement(
    val id: String,
    val caseIdentifier: String? = null,
    var attributes: Map<String, String> = mapOf(),
    var parent: String? = "View",
    var template: Template? = null,
    var childRule: String? = null,
    var autoWrapFor: List<String>? = null,
    var wrappedAttributes: List<String>? = null,
    var insertChildrenAt: String? = null,
    var uniqueElementType: String? = null,
    override val debug: Boolean = false
): ReplacementRule {
    override val priority: Int get() = attributes.size
}

