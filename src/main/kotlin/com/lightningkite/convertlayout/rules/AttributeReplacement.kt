package com.lightningkite.convertlayout.rules

data class AttributeReplacement(
    val id: String,
    var valueType: ValueType = ValueType.String,
    var element: String? = null,
    var rules: Map<String, SubRule> = mapOf(),
    override val debug: Boolean = false
): ReplacementRule {

    data class SubRule(
        val append: List<Template> = listOf(),
        val attribute: Map<String, Template> = mapOf(),
        var css: Map<String, Template> = mapOf()
    )

    enum class ValueType {
        Font,
        Color,
        ColorResource,
        ColorStateResource,
        Vector,
        Bitmap,
        Shape,
        DrawableState,
        Layer,
        LayoutResource,
        Dimension,
        DimensionResource,
        Number,
        String,
        StringResource,
        Style,
    }

    override val priority: Int get() = (if(element != null) 1 else 0)
}
