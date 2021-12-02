package com.lightningkite.convertlayout.rules

import com.lightningkite.convertlayout.android.*
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

data class AttributeReplacement(
    val id: String,
    var valueType: ValueType2 = ValueType2.Value,
    var element: String = "View",
    var rules: Map<String, SubRule> = mapOf(),
    var xib: Map<String, XibRuleType> = mapOf(),
    var code: Template? = null,
    var equalTo: String? = null,
    override val debug: Boolean = false
) : ReplacementRule {

    data class SubRule(
        var ifContains: Map<String, SubRule>? = null,
        val append: List<Template> = listOf(),
        val attribute: Map<String, Template?> = mapOf(),
        var css: Map<String, Template?> = mapOf()
    )

    enum class XibRuleType { SubNode, Attribute, UserDefined, StateSubNode, StateAttribute }

    enum class ValueType2(val kotlinClass: KClass<*>) {
        FontLiteral(AndroidFontLiteral::class),
        FontSet(AndroidFontSet::class),
        ColorLiteral(AndroidColorLiteral::class),
        ColorResource(AndroidColorResource::class),
        ColorStateResource(AndroidColorStateResource::class),
        Vector(AndroidVector::class),
        Bitmap(AndroidBitmap::class),
        Shape(AndroidShape::class),
        DrawableState(AndroidDrawableState::class),
        Layer(AndroidLayer::class),
        LayoutResource(AndroidLayoutResource::class),
        DimensionLiteral(AndroidDimensionLiteral::class),
        DimensionResource(AndroidDimensionResource::class),
        Number(AndroidNumber::class),
        StringLiteral(AndroidStringLiteral::class),
        StringResource(AndroidStringResource::class),
        Style(AndroidStyle::class),
        Value(AndroidValue::class),
        Dimension(AndroidDimension::class),
        Drawable(AndroidDrawable::class),
        NamedDrawable(AndroidNamedDrawable::class),
        NamedDrawableWithSize(AndroidNamedDrawableWithSize::class),
        XmlDrawable(AndroidXmlDrawable::class),
        DrawableXml(AndroidDrawableXml::class),
        Color(AndroidColor::class),
        String(AndroidString::class),
        Font(AndroidFont::class);

        val parentTypes: Set<ValueType2> by lazy {
            values()
                .asSequence()
                .filter { it.kotlinClass.isSuperclassOf(kotlinClass) }
                .toSet()
        }
        val depth: Int by lazy {
            val allInterfaces = HashSet<Class<*>>()
            fun traverse(clazz: Class<*>) {
                allInterfaces.add(clazz)
                clazz.interfaces.forEach { traverse(it) }
            }
            traverse(kotlinClass.java)
            allInterfaces.size
        }

        operator fun contains(other: ValueType2): Boolean = this in other.parentTypes

        companion object {
            val map = values().associateBy { it.kotlinClass }
            operator fun get(type: KClass<*>): ValueType2 = map[type]!!
        }
    }

    override val priority: Int get() = (if (equalTo != null) 20 else 0) + valueType.depth
}
