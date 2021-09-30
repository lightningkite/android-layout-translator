package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.xml.*
import org.w3c.dom.Element
import java.util.*
import kotlin.collections.getOrPut


enum class ConstraintAttribute {
    top,
    right,
    trailing,
    bottom,
    left,
    leading,
    width,
    height,
    centerX,
    centerY;
    companion object {
        operator fun get(vertical: Boolean, end: Boolean, locale: Boolean = true) = if(vertical)
            if(end) bottom else top
        else if(locale)
            if(end) trailing else leading
        else
            if(end) right else left
        fun center(vertical: Boolean) = if(vertical) centerY else centerX
        fun size(vertical: Boolean) = if(vertical) height else width
    }

}
val ConstraintAttribute.starting: Boolean get() = when(this) {
    ConstraintAttribute.top -> true
    ConstraintAttribute.right -> false
    ConstraintAttribute.trailing -> false
    ConstraintAttribute.bottom -> false
    ConstraintAttribute.left -> true
    ConstraintAttribute.leading -> true
    ConstraintAttribute.width -> false
    ConstraintAttribute.height -> false
    ConstraintAttribute.centerX -> false
    ConstraintAttribute.centerY -> false
}

enum class ConstraintRelation {
    equal,
    greaterThanOrEqual,
    lessThanOrEqual,
}

val ConstraintRelation.reversed: ConstraintRelation get() = when(this){
    ConstraintRelation.equal -> ConstraintRelation.equal
    ConstraintRelation.greaterThanOrEqual -> ConstraintRelation.lessThanOrEqual
    ConstraintRelation.lessThanOrEqual -> ConstraintRelation.greaterThanOrEqual
}

operator fun SystemEdges.get(key: ConstraintAttribute): Boolean {
    return when(key) {
        ConstraintAttribute.top -> this.top
        ConstraintAttribute.right -> this.right
        ConstraintAttribute.trailing -> this.end
        ConstraintAttribute.bottom -> this.bottom
        ConstraintAttribute.left -> this.left
        ConstraintAttribute.leading -> this.start
        ConstraintAttribute.width -> false
        ConstraintAttribute.height -> false
        ConstraintAttribute.centerX -> false
        ConstraintAttribute.centerY -> false
    }
}
private val elementSystemEdges = WeakHashMap<Element, SystemEdges>()
var Element.systemEdges: SystemEdges?
    get() = elementSystemEdges[this]
    set(value){
        elementSystemEdges[this] = value
    }
private val elementDirectSystemEdges = WeakHashMap<Element, SystemEdges>()
var Element.directSystemEdges: SystemEdges?
    get() = elementDirectSystemEdges[this]
    set(value){
        elementDirectSystemEdges[this] = value
    }

val Element.safeAreaGuide: Element get() {
    return getOrAppendChildWithKey("viewLayoutGuide", "safeArea").also {
        it["id"] = this.id() + "SafeAreaGuide"
    }
}

private fun Element.addConstraint(
    firstItem: Element?,
    firstAttribute: ConstraintAttribute,
    secondItem: Element?,
    secondAttribute: ConstraintAttribute?,
    constant: Double,
    multiplier: Double,
    relation: ConstraintRelation = ConstraintRelation.equal,
    priority: Int = 999
) {
    this.getOrAppendChild("constraints")
        .appendElement("constraint") {
            firstItem?.let { this["firstItem"] = it.id() }
            this["firstAttribute"] = firstAttribute.toString()
            secondItem?.let { this["secondItem"] = it.id() }
            secondAttribute?.let { this["secondAttribute"] = it.toString() }
            this["constant"] = constant.toString()
            this["multiplier"] = multiplier.toString()
            this["relation"] = relation.toString()
            this["priority"] = priority.toString()
            id()
        }
}

data class ElementAnchor(val element: Element, val attribute: ConstraintAttribute)
val Element.anchorTop: ElementAnchor get() = ElementAnchor(this, ConstraintAttribute.top)
val Element.anchorTrailing: ElementAnchor get() = ElementAnchor(this, ConstraintAttribute.trailing)
val Element.anchorBottom: ElementAnchor get() = ElementAnchor(this, ConstraintAttribute.bottom)
val Element.anchorLeading: ElementAnchor get() = ElementAnchor(this, ConstraintAttribute.leading)
val Element.anchorWidth: ElementAnchor get() = ElementAnchor(this, ConstraintAttribute.width)
val Element.anchorHeight: ElementAnchor get() = ElementAnchor(this, ConstraintAttribute.height)
val Element.anchorCenterX: ElementAnchor get() = ElementAnchor(this, ConstraintAttribute.centerX)
val Element.anchorCenterY: ElementAnchor get() = ElementAnchor(this, ConstraintAttribute.centerY)

fun Element.anchorStart(vertical: Boolean): ElementAnchor = if(vertical) anchorTop else anchorLeading
fun Element.anchorCenter(vertical: Boolean): ElementAnchor = if(vertical) anchorCenterY else anchorCenterX
fun Element.anchorEnd(vertical: Boolean): ElementAnchor = if(vertical) anchorBottom else anchorTrailing
fun Element.anchorSize(vertical: Boolean): ElementAnchor = if(vertical) anchorHeight else anchorWidth

private fun Element.commonAncestor(with: Element): Element? {
    val depthA = generateSequence(this) { it.parentNode as? Element }.count()
    val depthB = generateSequence(with) { it.parentNode as? Element }.count()
    var a = this
    var b = with
    if(depthA > depthB) {
        repeat(depthA - depthB) { a = a.parentNode as? Element ?: return null }
    } else {
        repeat(depthB - depthA) { b = b.parentNode as? Element ?: return null }
    }
    while(a !== b) {
        a = a.parentNode as? Element ?: return null
        b = b.parentNode as? Element ?: return null
    }
    return a
}
fun ElementAnchor.constraint(
    other: ElementAnchor,
    relationship: ConstraintRelation = ConstraintRelation.equal,
    constant: Double = 0.0,
    multiplier: Double = 1.0,
    priority: Int = 1000
) {
    val commonView = (this.element.commonAncestor(other.element) ?: throw IllegalStateException("No common ancestor between $element and ${other.element}")).let {
        if(it.tagName == "subviews") it.parentNode as Element
        else it
    }
    commonView.addConstraint(
        firstItem = if(other.element == commonView) null else other.element,
        firstAttribute = other.attribute,
        secondItem = this.element,
        secondAttribute = this.attribute,
//        firstItem = if(this.element == commonView) null else this.element,
//        firstAttribute = this.attribute,
//        secondItem = other.element,
//        secondAttribute = other.attribute,
        constant = constant,
        multiplier = multiplier,
        relation = relationship,
        priority = priority
    )
}
fun Element.constraintChildMatch(
    child: Element,
    attribute: ConstraintAttribute,
    relationship: ConstraintRelation = ConstraintRelation.equal,
    constant: Double = 0.0,
    multiplier: Double = 1.0,
    priority: Int = 1000
) {
    val parentAnchorElement = when {
        this.directSystemEdges?.get(attribute) == true -> this.safeAreaGuide
        else -> this
    }
    val parentAnchor = ElementAnchor(
        parentAnchorElement,
        attribute
    )
    val childAnchor = ElementAnchor(
        child,
        attribute
    )
    if(attribute.starting) {
        parentAnchor.constraint(childAnchor, relationship, constant, multiplier, priority)
    } else {
        childAnchor.constraint(parentAnchor, relationship, constant, multiplier, priority)
    }
}
fun Element.constraintChildMatchTop(child: Element, relationship: ConstraintRelation = ConstraintRelation.equal, constant: Double = 0.0, multiplier: Double = 1.0, priority: Int = 1000) = constraintChildMatch(child, ConstraintAttribute.top, relationship, constant, multiplier, priority)
fun Element.constraintChildMatchRight(child: Element, relationship: ConstraintRelation = ConstraintRelation.equal, constant: Double = 0.0, multiplier: Double = 1.0, priority: Int = 1000) = constraintChildMatch(child, ConstraintAttribute.right, relationship, constant, multiplier, priority)
fun Element.constraintChildMatchTrailing(child: Element, relationship: ConstraintRelation = ConstraintRelation.equal, constant: Double = 0.0, multiplier: Double = 1.0, priority: Int = 1000) = constraintChildMatch(child, ConstraintAttribute.trailing, relationship, constant, multiplier, priority)
fun Element.constraintChildMatchBottom(child: Element, relationship: ConstraintRelation = ConstraintRelation.equal, constant: Double = 0.0, multiplier: Double = 1.0, priority: Int = 1000) = constraintChildMatch(child, ConstraintAttribute.bottom, relationship, constant, multiplier, priority)
fun Element.constraintChildMatchLeft(child: Element, relationship: ConstraintRelation = ConstraintRelation.equal, constant: Double = 0.0, multiplier: Double = 1.0, priority: Int = 1000) = constraintChildMatch(child, ConstraintAttribute.left, relationship, constant, multiplier, priority)
fun Element.constraintChildMatchLeading(child: Element, relationship: ConstraintRelation = ConstraintRelation.equal, constant: Double = 0.0, multiplier: Double = 1.0, priority: Int = 1000) = constraintChildMatch(child, ConstraintAttribute.leading, relationship, constant, multiplier, priority)
fun Element.constraintChildMatchWidth(child: Element, relationship: ConstraintRelation = ConstraintRelation.equal, constant: Double = 0.0, multiplier: Double = 1.0, priority: Int = 1000) = constraintChildMatch(child, ConstraintAttribute.width, relationship, constant, multiplier, priority)
fun Element.constraintChildMatchHeight(child: Element, relationship: ConstraintRelation = ConstraintRelation.equal, constant: Double = 0.0, multiplier: Double = 1.0, priority: Int = 1000) = constraintChildMatch(child, ConstraintAttribute.height, relationship, constant, multiplier, priority)
fun Element.constraintChildMatchCenterX(child: Element, relationship: ConstraintRelation = ConstraintRelation.equal, constant: Double = 0.0, multiplier: Double = 1.0, priority: Int = 1000) = constraintChildMatch(child, ConstraintAttribute.centerX, relationship, constant, multiplier, priority)
fun Element.constraintChildMatchCenterY(child: Element, relationship: ConstraintRelation = ConstraintRelation.equal, constant: Double = 0.0, multiplier: Double = 1.0, priority: Int = 1000) = constraintChildMatch(child, ConstraintAttribute.centerY, relationship, constant, multiplier, priority)
infix fun ElementAnchor.matches(other: ElementAnchor) = this.constraint(other)
fun ElementAnchor.setTo(constant: Double) {
    this.element.addConstraint(
        firstItem = null,
        firstAttribute = this.attribute,
        secondItem = null,
        secondAttribute = null,
        constant = constant,
        multiplier = 1.0,
        relation = ConstraintRelation.equal
    )
}

const val framingHugPriority = 300
const val framingCompressionPriority = 700
fun Element.constraintChildFrameAxis(
    child: Element,
    vertical: Boolean,
    align: Align,
    alignLocaleDependent: Boolean = true,
    insets: Insets,
    contentHuggingPriority: Int = framingHugPriority,
    contentCompressionResistancePriority: Int = framingCompressionPriority
) {
    when(align) {
        Align.START -> {
            constraintChildMatch(
                child,
                attribute = ConstraintAttribute[vertical, false, alignLocaleDependent],
                constant = insets[vertical, false, alignLocaleDependent]
            )
            constraintChildMatch(
                child,
                attribute = ConstraintAttribute[vertical, true, insets.localeDependent],
                constant = insets[vertical, true, insets.localeDependent],
                relationship = ConstraintRelation.greaterThanOrEqual,
                priority = contentCompressionResistancePriority
            )
            constraintChildMatch(
                child,
                attribute = ConstraintAttribute[vertical, true, insets.localeDependent],
                constant = insets[vertical, true, insets.localeDependent],
                priority = contentHuggingPriority
            )
        }
        Align.CENTER -> {
            constraintChildMatch(
                child,
                attribute = ConstraintAttribute[vertical, false, insets.localeDependent],
                constant = insets[vertical, false, insets.localeDependent],
                priority = contentHuggingPriority
            )
            constraintChildMatch(
                child,
                attribute = ConstraintAttribute[vertical, false, insets.localeDependent],
                constant = insets[vertical, false, insets.localeDependent],
                relationship = ConstraintRelation.greaterThanOrEqual,
                priority = contentCompressionResistancePriority
            )
            constraintChildMatch(
                child,
                attribute = ConstraintAttribute.center(vertical)
            )
        }
        Align.END -> {
            constraintChildMatch(
                child,
                attribute = ConstraintAttribute[vertical, false, insets.localeDependent],
                constant = insets[vertical, false, insets.localeDependent],
                relationship = ConstraintRelation.greaterThanOrEqual,
                priority = contentCompressionResistancePriority
            )
            constraintChildMatch(
                child,
                attribute = ConstraintAttribute[vertical, false, insets.localeDependent],
                constant = insets[vertical, false, insets.localeDependent],
                priority = contentHuggingPriority
            )
            constraintChildMatch(
                child,
                attribute = ConstraintAttribute[vertical, true, alignLocaleDependent],
                constant = insets[vertical, true, alignLocaleDependent]
            )
        }
    }
}
fun Element.constraintChildBiggestOfChildrenAxis(
    child: Element,
    vertical: Boolean,
    insets: Insets,
    contentHuggingPriority: Int = framingHugPriority,
    contentCompressionResistancePriority: Int = framingCompressionPriority
) {
    constraintChildMatch(
        child,
        attribute = ConstraintAttribute.size(vertical),
        constant = insets.size(vertical),
        relationship = ConstraintRelation.greaterThanOrEqual,
        priority = contentCompressionResistancePriority
    )
    constraintChildMatch(
        child,
        attribute = ConstraintAttribute.size(vertical),
        constant = insets.size(vertical),
        priority = contentHuggingPriority
    )
}
fun Element.constraintChildFrame(
    child: Element,
    gravity: Gravity,
    insets: Insets,
    contentHuggingPriority: Int = framingHugPriority,
    contentCompressionResistancePriority: Int = framingCompressionPriority
) {
    constraintChildFrameAxis(child, true, gravity.vertical, gravity.localeDependent, insets, contentHuggingPriority, contentCompressionResistancePriority)
    constraintChildFrameAxis(child, false, gravity.horizontal, gravity.localeDependent, insets, contentHuggingPriority, contentCompressionResistancePriority)
}
fun Element.constraintChildMatchEdgesAxis(child: Element, vertical: Boolean, insets: Insets, priority: Int = 1000) {
    constraintChildMatch(child, ConstraintAttribute[vertical, false], constant = insets[vertical, false], priority = priority)
    constraintChildMatch(child, ConstraintAttribute[vertical, true], constant = insets[vertical, true], priority = priority)
}
fun Element.constraintChildMatchEdges(child: Element, insets: Insets, priority: Int = 1000) {
    constraintChildMatchEdgesAxis(child, true, insets, priority)
    constraintChildMatchEdgesAxis(child, false, insets, priority)
}
