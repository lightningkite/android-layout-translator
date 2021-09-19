package com.lightningkite.convertlayout.rules

data class AttributeReplacement(
    val id: String,
    var valueType: GeneralValueType = GeneralValueType.String,
    var subtype: ValueType? = null,
    var element: String? = null,
    var rules: Map<String, SubRule> = mapOf(),
    var xib: Map<String, XibRuleType> = mapOf(),
    var code: Template? = null,
    var equalTo: String? = null,
    override val debug: Boolean = false
): ReplacementRule {

    data class SubRule(
        var ifContains: Map<String, SubRule>? = null,
        val append: List<Template> = listOf(),
        val attribute: Map<String, Template> = mapOf(),
        var css: Map<String, Template> = mapOf()
    )

    enum class XibRuleType { SubNode, Attribute, UserDefined, State }

    enum class GeneralValueType {
        Font,
        Color,
        Drawable,
        Layout,
        Dimension,
        Number,
        String,
        Style,
    }

    enum class ValueType(val general: GeneralValueType) {
        Font(GeneralValueType.Font),
        FontSet(GeneralValueType.Font),
        Color(GeneralValueType.Color),
        ColorResource(GeneralValueType.Color),
        ColorStateResource(GeneralValueType.Color),
        Vector(GeneralValueType.Drawable),
        Bitmap(GeneralValueType.Drawable),
        Shape(GeneralValueType.Drawable),
        DrawableState(GeneralValueType.Drawable),
        Layer(GeneralValueType.Drawable),
        LayoutResource(GeneralValueType.Layout),
        Dimension(GeneralValueType.Dimension),
        DimensionResource(GeneralValueType.Dimension),
        Number(GeneralValueType.Number),
        String(GeneralValueType.String),
        StringResource(GeneralValueType.String),
        Style(GeneralValueType.Style),
    }

    override val priority: Int get() = (if(equalTo != null) 4 else 0) + (if(element != null) 1 else 0) + (if(subtype != null) 1 else 0)
}
