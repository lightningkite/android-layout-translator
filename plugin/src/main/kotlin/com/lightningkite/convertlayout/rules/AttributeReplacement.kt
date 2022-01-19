package com.lightningkite.convertlayout.rules

import com.lightningkite.convertlayout.android.*
import com.lightningkite.convertlayout.xml.appendFragment
import com.lightningkite.convertlayout.xml.appendText
import com.lightningkite.convertlayout.xml.xpathElementOrCreate
import org.w3c.dom.Element
import kotlin.reflect.KClass
import kotlin.reflect.full.isSuperclassOf

data class AttributeReplacement(
    val id: String,
    var valueType: ValueType2 = ValueType2.Value,
    var element: String = "View",
    var parentElement: String? = null,
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
        var css: Map<String, Template?> = mapOf(),
        val classes: List<Template> = listOf()
    ) {
        operator fun invoke(
            value: AndroidValue,
            getter: (String) -> String,
            action: (SubRule) -> Unit
        ) {
            action(this)
            ifContains?.let { ifContains ->
                val raw = (value as? AndroidString)?.value ?: value.toString()
                var hit = false
                for (entry in ifContains) {
                    if (entry.key.contains("=")) {
                        val eqKey = entry.key.substringBefore("=")
                        val eqValue = entry.key.substringAfter("=")
                        val attrValue = getter(eqKey)
                        if (eqValue in attrValue.split('|')) {
                            entry.value(value, getter, action)
                            hit = true
                        }
                    } else {
                        if (entry.key in raw.split('|')) {
                            entry.value(value, getter, action)
                            hit = true
                        }
                    }
                }
                if (!hit) ifContains["else"]?.let { it(value, getter, action) }
            }
        }
    }

    enum class XibRuleType { SubNode, Attribute, UserDefined, StateSubNode, StateAttribute }

    enum class ValueType2(val kotlinClass: KClass<*>) {
        FontLiteral(AndroidFontLiteral::class),
        FontSet(AndroidFontSet::class),
        ColorLiteral(AndroidColorLiteral::class),
        ColorResource(AndroidColorResource::class),
        ColorStateResource(AndroidColorStateResource::class),
        NamedColor(AndroidNamedColor::class),
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
