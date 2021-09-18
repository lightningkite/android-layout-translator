package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.xml.*
import org.w3c.dom.Element
import kotlin.collections.getOrPut


enum class ConstraintAttribute {
    top,
    trailing,
    bottom,
    leading,
    width,
    height,
    centerX,
    centerY
}

enum class ConstraintRelation {
    equal,
    greaterThanOrEqual,
    lessThanOrEqual,
}

fun Element.addConstraint(
    firstItem: Element?,
    firstAttribute: ConstraintAttribute,
    secondItem: Element?,
    secondAttribute: ConstraintAttribute?,
    constant: Double,
    multiplier: Double,
    relation: ConstraintRelation = ConstraintRelation.equal
) {
    this.getOrAppendChild("constraints")
        .appendElement("constraint") {
            firstItem?.let { this["firstItem"] = it.getOrPut("id") { generateId() } }
            this["firstAttribute"] = firstAttribute.toString()
            secondItem?.let { this["secondItem"] = it.getOrPut("id") { generateId() } }
            secondAttribute?.let { this["secondAttribute"] = it.toString() }
            this["constant"] = constant.toString()
            this["multiplier"] = multiplier.toString()
            this["relation"] = relation.toString()
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
fun ElementAnchor.constraint(other: ElementAnchor, relationship: ConstraintRelation = ConstraintRelation.equal, constant: Double = 0.0, multiplier: Double = 1.0) {
    val commonView = this.element.commonAncestor(other.element)!!.let {
        if(it.tagName == "subviews") it.parentNode as Element
        else it
    }
    commonView.addConstraint(
        firstItem = if(this.element == commonView) null else this.element,
        firstAttribute = this.attribute,
        secondItem = other.element,
        secondAttribute = other.attribute,
        constant = constant,
        multiplier = multiplier,
        relation = relationship
    )
}
infix fun ElementAnchor.matches(other: ElementAnchor) {
    val commonView = this.element.commonAncestor(other.element)!!.let {
        if(it.tagName == "subviews") it.parentNode as Element
        else it
    }
    commonView.addConstraint(
        firstItem = if(this.element == commonView) null else this.element,
        firstAttribute = this.attribute,
        secondItem = other.element,
        secondAttribute = other.attribute,
        constant = 0.0,
        multiplier = 1.0,
        relation = ConstraintRelation.equal
    )
}
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