package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.android.*
import com.lightningkite.convertlayout.rules.AttributeReplacement
import com.lightningkite.convertlayout.rules.ElementReplacement
import com.lightningkite.convertlayout.rules.Replacements
import com.lightningkite.convertlayout.util.camelCase
import com.lightningkite.convertlayout.web.WebLayoutFile
import com.lightningkite.convertlayout.web.WebLayoutTranslatorForFile
import com.lightningkite.convertlayout.xml.*
import org.w3c.dom.Document
import org.w3c.dom.Element
import java.io.File
import kotlin.collections.HashMap
import kotlin.collections.HashSet
import kotlin.math.min

internal class IosLayoutTranslatorForFile(
    val project: IosProject,
    replacements: Replacements,
    resources: AndroidResources
) : AndroidLayoutTranslator(replacements, resources) {

    companion object {
        val compoundDrawables: Set<String> = setOf(
            "android:drawableLeft",
            "android:drawableRight",
            "android:drawableStart",
            "android:drawableEnd",
            "android:drawableTop",
            "android:drawableBottom",
        )
        val defaultWrappedAttributes: Set<String> = setOf(
            "android:visibility",
            "android:minWidth",
            "android:minHeight",
            "android:elevation"
        )
    }

    override val useTools: Boolean
        get() = true

    private fun Element.childrenWhoWrap(isVertical: Boolean): Int {
        return childElements
            .filter { it.allAttributes["android:layout_${if(isVertical) "height" else "vertical"}"] == "wrap_content" }
            .map { it.childrenWhoWrap(isVertical) }
            .maxOrNull()
            ?.plus(1)
            ?: 0
    }
    fun Element.wrapPower(isVertical: Boolean): Int {
        return this["tools:wrapPower"]?.toIntOrNull() ?: (998 - childrenWhoWrap(isVertical) * 2)
    }

    var outlets: MutableMap<String, SwiftIdentifier> = LinkedHashMap()
    val usedResources: MutableSet<AndroidValue> = LinkedHashSet()
    val iosCode = StringBuilder()

    fun assignIds(destElement: Element) {
        destElement.id()
        destElement.getChild("subviews")?.childElements?.forEach { assignIds(it) }
        destElement.getChild("constraints")?.childElements?.forEach { it.id() }
    }

    override fun getProjectWide(key: String): String? = when (key) {
        "projectName" -> project.name
        "moduleName" -> project.moduleName
        else -> null
    }

    override fun convertElement(
        destOwner: Element,
        rules: List<ElementReplacement>,
        sourceElement: Element,
        allAttributes: Map<String, String>
    ): Element {
        val autowrap = rules.asSequence()
            .flatMap { it.autoWrapFor?.asSequence() ?: sequenceOf() }
            .toSet()
        val directSystemEdges = sourceElement.allAttributes["app:safeInsets"]?.toSystemEdges() ?: sourceElement.allAttributes["app:safeInsetsSizing"]?.toSystemEdges()

        if (allAttributes.keys.none { it in autowrap }) {
            val newElement =
                destOwner.appendFragment(
                    rules.asSequence().mapNotNull { it.template }.first()
                        .write { getProjectWide(it) ?: with(resources) { sourceElement.getPath(it) } })
            directSystemEdges?.let { newElement.directSystemEdges = it }
            allAttributes["android:id"]
                ?.substringAfter('/')
                ?.camelCase()
                ?.also { outlets[it] = newElement.swiftIdentifier() }
                ?.let { newElement["id"] = it }
            assignIds(newElement)
            if (sourceElement.convertibleChildElements.none()) {
                if (allAttributes["android:layout_width"] == "wrap_content") {
                    newElement["horizontalHuggingPriority"] = "1000"
                    newElement["horizontalCompressionResistancePriority"] = "1000"
                } else if (newElement.tagName == "label") {
                    newElement["numberOfLines"] = "0"
                } else if (rules.any { it.id == "LabeledToggle" }) {
                    newElement.childElements
                        .find { it.tagName == "subviews" }
                        ?.childElements
                        ?.find { it.tagName == "label" }
                        ?.set("numberOfLines", "0")
                }
                if (allAttributes["android:layout_height"] == "wrap_content") {
                    newElement["verticalHuggingPriority"] = "1000"
                    newElement["verticalCompressionResistancePriority"] = "1000"
                }
            }

            // Handle children
            handleChildren(rules, newElement, sourceElement)

            // Handle attributes
            handleAttributes(rules, allAttributes, sourceElement, newElement)

            handleSetSize(sourceElement, allAttributes, newElement)

            assignIds(newElement)
            return newElement
        } else {
            val wrappedAttributes = rules.asSequence()
                .flatMap { it.wrappedAttributes?.asSequence() ?: sequenceOf() }
                .toSet().plus(autowrap).plus(defaultWrappedAttributes)
            val innerAttributes = HashMap<String, String>()
            val outerAttributes = HashMap<String, String>()
            for ((key, value) in allAttributes) {
                if (key in wrappedAttributes) outerAttributes[key] = value
                else innerAttributes[key] = value
            }

            val outerElement = destOwner.appendElement("view")
            outerElement["translatesAutoresizingMaskIntoConstraints"] = "NO"
            outerElement["customModule"] = "XmlToXibRuntime"
            outerElement["customClass"] = "ContainerView"
            val innerElement =
                outerElement.getOrAppendChild("subviews")
                    .appendFragment(rules.asSequence().mapNotNull { it.template }.first()
                        .write { getProjectWide(it) ?: with(resources) { sourceElement.getPath(it) } })
            directSystemEdges?.let { outerElement.directSystemEdges = it }
            assignIds(innerElement)
            allAttributes["android:id"]
                ?.substringAfter('/')
                ?.camelCase()
                ?.also { outlets[it] = innerElement.swiftIdentifier() }
                ?.let { innerElement["id"] = it }
            assignIds(outerElement)

            if (sourceElement.convertibleChildElements.none()) {
                if (allAttributes["android:layout_width"] == "wrap_content") {
                    innerElement["horizontalHuggingPriority"] = "1000"
                    innerElement["horizontalCompressionResistancePriority"] = "1000"
                } else if (innerElement.tagName == "label") {
                    innerElement["numberOfLines"] = "0"
                } else if (rules.any { it.id == "LabeledToggle" }) {
                    innerElement.childElements
                        .find { it.tagName == "subviews" }
                        ?.childElements
                        ?.find { it.tagName == "label" }
                        ?.set("numberOfLines", "0")
                }
                if (allAttributes["android:layout_height"] == "wrap_content") {
                    innerElement["verticalHuggingPriority"] = "1000"
                    innerElement["verticalCompressionResistancePriority"] = "1000"
                }
            }

            val compounds = outerAttributes.keys.intersect(compoundDrawables)

            val padding = outerAttributes.insets("android:padding", resources)
            if(compounds.isNotEmpty()) {
                // Handle compound drawables
                val drawablePadding = allAttributes["android:drawablePadding"]
                    ?.let { resources.read(it) as? AndroidDimension }
                    ?.measurement?.number
                    ?: 8.0
                val handlerInfo = CompoundHandlerInfo(
                    outerAttributes = outerAttributes,
                    destOwner = destOwner,
                    innerElement = innerElement,
                    outerElement = outerElement,
                    padding = padding,
                    drawablePadding = drawablePadding
                )
                if(!handleCompoundDrawable(handlerInfo, "Top", true, false, false)) {
                    outerElement.constraintChildMatch(innerElement, ConstraintAttribute[true, false, false], constant = padding[true, false, false])
                }
                if(!handleCompoundDrawable(handlerInfo, "Bottom", true, true, false)) {
                    outerElement.constraintChildMatch(innerElement, ConstraintAttribute[true, true, false], constant = padding[true, true, false])
                }
                if(!handleCompoundDrawable(handlerInfo, "Left", false, false, false) &&
                !handleCompoundDrawable(handlerInfo, "Start", false, false, true)) {
                    outerElement.constraintChildMatch(innerElement, ConstraintAttribute[false, false, false], constant = padding[false, false, true])
                }
                if(!handleCompoundDrawable(handlerInfo, "Right", false, true, false) &&
                !handleCompoundDrawable(handlerInfo, "End", false, true, true)) {
                    outerElement.constraintChildMatch(innerElement, ConstraintAttribute[false, true, false], constant = padding[false, true, true])
                }
            } else {
                // Set up constraints between outer/inner based on padding
                outerElement.constraintChildMatchEdges(innerElement, padding)
            }

            // Handle children
            handleChildren(rules, innerElement, sourceElement)

            // Handle attributes
            handleAttributes(
                listOfNotNull(replacements.getElement("View", mapOf())),
                outerAttributes,
                sourceElement,
                outerElement
            )
            handleAttributes(rules, innerAttributes, sourceElement, innerElement)

            if(directSystemEdges != null) handleSetSize(sourceElement, allAttributes, innerElement)
            else handleSetSize(sourceElement, allAttributes, outerElement)

            assignIds(outerElement)
            return outerElement
        }
    }

    private data class CompoundHandlerInfo(
        val outerAttributes: HashMap<String, String>,
        val destOwner: Element,
        val innerElement: Element,
        val outerElement: Element,
        val padding: Insets,
        val drawablePadding: Double,
    )
    private fun handleCompoundDrawable(
        compoundHandlerInfo: CompoundHandlerInfo,
        textName: String,
        vertical: Boolean,
        end: Boolean,
        locale: Boolean
    ): Boolean = with(compoundHandlerInfo) {
        return outerAttributes["android:drawable$textName"]?.let {
            val src = resources.read(it) as AndroidDrawable
            val drawableElement = outerElement.getOrAppendChild("subviews").appendElement("view")
            drawableElement["translatesAutoresizingMaskIntoConstraints"] = "NO"
            drawableElement.setAttribute("placeholderIntrinsicWidth", "25")
            drawableElement.setAttribute("placeholderIntrinsicHeight", "25")
            val drawableElementId = "${innerElement.id()}compoundDrawable$textName"
            drawableElement["id"] = drawableElementId
            if (src is AndroidNamedDrawable) {
                handleXibEntry(drawableElement, src, "backgroundLayerName", AttributeReplacement.XibRuleType.UserDefined)
            }
            drawableElement.anchorWidth.setTo(src.size.width.toDouble())
            drawableElement.anchorHeight.setTo(src.size.height.toDouble())
            outerElement.constraintChildMatch(drawableElement, ConstraintAttribute[vertical, end, locale], constant = padding[vertical, end, locale])
            outerElement.constraintChildMatch(drawableElement, ConstraintAttribute.center(!vertical))
            ElementAnchor(innerElement, ConstraintAttribute[vertical, end, locale]).constraint(
                ElementAnchor(drawableElement, ConstraintAttribute[vertical, !end, locale]),
                constant = drawablePadding
            )
            true
        } ?: false
    }

    override fun handleChildren(
        rules: List<ElementReplacement>,
        childAddRule: String?,
        sourceElement: Element,
        destElement: Element,
        target: Element
    ) {
        val myAttributes = sourceElement.allAttributes
        when (childAddRule) {
            "linear" -> {
                destElement["insetsLayoutMarginsFromSafeArea"] = "NO"
//                if(myAttributes["app:safeInsets"] == null && myAttributes["app:safeInsetsSizing"] == null) destElement["insetsLayoutMarginsFromSafeArea"] = "NO"
                val isVertical = myAttributes["android:orientation"] == "vertical"
                val myPadding = myAttributes.insets("android:padding", resources)
                val wrappingPower = if(myAttributes["android:layout_${if(!isVertical) "height" else "width"}"] == "wrap_content")
                    sourceElement.wrapPower(!isVertical)
                else
                    -1

                // Find most common alignment
                val defaultAlignInSource: AlignOrStretch =
                    myAttributes["android:gravity"]?.toGravity()?.get(!isVertical)?.orStretch() ?: AlignOrStretch.START
                val childAlignments = sourceElement.convertibleChildElements
                    .map {
                        val attrs = it.allAttributes
                        if (attrs["android:layout_${if (isVertical) "width" else "height"}"] == "match_parent") AlignOrStretch.STRETCH
                        else attrs["android:layout_gravity"]?.toGravity()?.get(!isVertical)?.orStretch()
                            ?: defaultAlignInSource
                    }
                    .toList()
                val commonAlign: AlignOrStretch = childAlignments
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .maxByOrNull { it.value + if (it.key == AlignOrStretch.STRETCH) 0.1 else 0.0 }
                    ?.key ?: defaultAlignInSource
                val minMargins: Insets = sourceElement.convertibleChildElements
                    .map { it.allAttributes.insets("android:layout_margin", resources) }
                    .takeUnless { it.none() }
                    ?.fold(Insets(999.0, 999.0, 999.0, 999.0)) { a, b ->
                        a.left = min(a.left, b.left)
                        a.top = min(a.top, b.top)
                        a.right = min(a.right, b.right)
                        a.bottom = min(a.bottom, b.bottom)
                        a
                    } ?: Insets.zero

                // Set default alignment on stackView
                if (isVertical)
                    when (commonAlign) {
                        AlignOrStretch.START -> destElement["alignment"] = "top"
                        AlignOrStretch.CENTER -> destElement["alignment"] = "center"
                        AlignOrStretch.END -> destElement["alignment"] = "bottom"
                        AlignOrStretch.STRETCH -> {
                        }
                    }
                else
                    when (commonAlign) {
                        AlignOrStretch.START -> destElement["alignment"] = "leading"
                        AlignOrStretch.CENTER -> destElement["alignment"] = "center"
                        AlignOrStretch.END -> destElement["alignment"] = "trailing"
                        AlignOrStretch.STRETCH -> {
                        }
                    }

                // Set default margin matching orientation on stackView via default spacing
                destElement["spacing"] = (minMargins.start(isVertical) + minMargins.end(isVertical)).toString()

                // Set default margin on stackView via adding to content insets
                val myMargins = myPadding + minMargins
                destElement.getOrAppendChildWithKey("edgeInsets", "layoutMargins").apply {
                    if (isVertical) {
                        this["top"] = myMargins.top.toString()
                        this["bottom"] = myMargins.bottom.toString()
                        this["left"] = myMargins.left.toString()
                        this["right"] = myMargins.right.toString()
                    } else {
                        this["left"] = myMargins.left.toString()
                        this["right"] = myMargins.right.toString()
                        this["top"] = myMargins.top.toString()
                        this["bottom"] = myMargins.bottom.toString()
                    }
                }

                var firstWeightedChild: Element? = null
                var firstWeightedChildWeight = 1.0
                sourceElement.convertibleChildElements.forEachIndexed { index, child ->
                    val childAttributes = child.allAttributes
                    val childAlign: AlignOrStretch = childAlignments[index]
                    val rawChildMargins = childAttributes.insets("android:layout_margin", resources)
                    val totalMargins = rawChildMargins + myPadding
                    val childMargins = rawChildMargins - minMargins

                    val (destChild, outerElement) = if (childAlign == commonAlign && childMargins == Insets.zero) {
                        // If matching common alignment and margins, be direct
                        val destChild = convertElement(target, child)
                        //destChild.set("debug", "direct")
                        destElement.constraintChildBiggestOfChildrenAxis(
                            child = destChild,
                            vertical = !isVertical,
                            insets = totalMargins,
                            contentHuggingPriority = if(wrappingPower == -1) framingHugPriority else wrappingPower - 1,
                            contentCompressionResistancePriority = if(wrappingPower == -1) framingCompressionPriority else wrappingPower
                        )
                        destChild to destChild
                    } else if (childAlign == commonAlign && childMargins.start(!isVertical) == 0.0 && childMargins.end(!isVertical) == 0.0) {
                        // If matching common alignment but not margins, be direct but add spacer views
                        val additionalStartPadding = childMargins.start(isVertical)
                        val additionalEndPadding = childMargins.end(isVertical)

                        if (additionalStartPadding > 0.0) {
                            val spacer = target.appendElement("view")
                            spacer.id()
                            spacer["translatesAutoresizingMaskIntoConstraints"] = "NO"
                            spacer.anchorSize(isVertical).setTo(additionalStartPadding)
                            if(commonAlign != AlignOrStretch.STRETCH) spacer.anchorSize(!isVertical).setTo(1.0)
                        }
                        val destChild = convertElement(target, child)
                        if (additionalEndPadding > 0.0) {
                            val spacer = target.appendElement("view")
                            spacer.id()
                            spacer["translatesAutoresizingMaskIntoConstraints"] = "NO"
                            spacer.anchorSize(isVertical).setTo(additionalEndPadding)
                            if(commonAlign != AlignOrStretch.STRETCH) spacer.anchorSize(!isVertical).setTo(1.0)
                        }
                        destElement.constraintChildBiggestOfChildrenAxis(
                            child = destChild,
                            vertical = !isVertical,
                            insets = totalMargins,
                            contentHuggingPriority = if(wrappingPower == -1) framingHugPriority else wrappingPower - 1,
                            contentCompressionResistancePriority = if(wrappingPower == -1) framingCompressionPriority else wrappingPower
                        )
                        destChild to destChild
                    } else {
                        val outerElement = target.appendElement("view")
                        outerElement.id()
                        outerElement["translatesAutoresizingMaskIntoConstraints"] = "NO"
                        outerElement["customModule"] = "XmlToXibRuntime"
                        outerElement["customClass"] = "ContainerView"

                        // Stretch to fill
                        when (commonAlign) {
                            AlignOrStretch.START -> destElement.constraintChildMatch(
                                outerElement,
                                attribute = ConstraintAttribute[!isVertical, true],
                                constant = myMargins[!isVertical, true]
                            )
                            AlignOrStretch.CENTER,
                            AlignOrStretch.END -> destElement.constraintChildMatch(
                                outerElement,
                                attribute = ConstraintAttribute[!isVertical, false],
                                constant = myMargins[!isVertical, false]
                            )
                            AlignOrStretch.STRETCH -> {
                            }
                        }

                        val innerElement = convertElement(outerElement.getOrAppendChild("subviews"), child)
                        outerElement.constraintChildMatchEdgesAxis(innerElement, isVertical, childMargins)
                        if(childAlign == AlignOrStretch.STRETCH) {
                            //outerElement.set("debug", "stretch, $isVertical")
                            outerElement.constraintChildMatchEdgesAxis(innerElement, !isVertical, childMargins)
                        } else {
                            //outerElement.set("debug", "not stretch, $isVertical")
                            outerElement.constraintChildFrameAxis(
                                child = innerElement,
                                vertical = !isVertical,
                                align = childAlign.align(),
                                alignLocaleDependent = true,
                                insets = childMargins,
                                contentHuggingPriority = if(wrappingPower == -1) framingHugPriority else wrappingPower - 1,
                                contentCompressionResistancePriority = if(wrappingPower == -1) framingCompressionPriority else wrappingPower
                            )
                        }
                        destElement.constraintChildBiggestOfChildrenAxis(
                            child = outerElement,
                            vertical = !isVertical,
                            insets = totalMargins - childMargins,
                            contentHuggingPriority = if(wrappingPower == -1) framingHugPriority else wrappingPower - 1,
                            contentCompressionResistancePriority = if(wrappingPower == -1) framingCompressionPriority else wrappingPower
                        )
                        innerElement to outerElement
                    }

                    val childWeight = childAttributes["android:layout_weight"]?.toDouble() ?: 0.0
                    if (childWeight > 0) {
                        outerElement["${if (isVertical) "vertical" else "horizontal"}HuggingPriority"] = "100"
                        firstWeightedChild?.let {
                            outerElement.anchorSize(isVertical).constraint(
                                it.anchorSize(isVertical),
                                multiplier = childWeight / firstWeightedChildWeight,
                                priority = 900
                            )
                        } ?: run {
                            firstWeightedChild = outerElement
                            firstWeightedChildWeight = childWeight
                        }
                    }
                }

                // If no weights AND size isn't wrap content, add final spacer view
                if (sourceElement.convertibleChildElements.any() && firstWeightedChild == null && myAttributes["android:layout_${if (isVertical) "height" else "width"}"] != "wrap_content") {
                    when(myAttributes["android:gravity"]?.toGravity()?.get(isVertical) ?: Align.START) {
                        Align.START -> {
                            val finalSpacer = target.appendElement("view") {
                                id()
                                this["translatesAutoresizingMaskIntoConstraints"] = "NO"
                                this["${if (isVertical) "vertical" else "horizontal"}HuggingPriority"] = "100"
                            }
                            if(commonAlign != AlignOrStretch.STRETCH) finalSpacer.anchorSize(!isVertical).setTo(1.0)
                        }
                        Align.CENTER -> {
                            val finalSpacer = target.appendElement("view") {
                                id()
                                this["translatesAutoresizingMaskIntoConstraints"] = "NO"
                                this["${if (isVertical) "vertical" else "horizontal"}HuggingPriority"] = "100"
                            }
                            if(commonAlign != AlignOrStretch.STRETCH) finalSpacer.anchorSize(!isVertical).setTo(1.0)
                            // Due to the ID generation process, we append then move its position
                            val firstSpacer = target.appendElement("view") {
                                id()
                                this["translatesAutoresizingMaskIntoConstraints"] = "NO"
                                this["${if (isVertical) "vertical" else "horizontal"}HuggingPriority"] = "100"
                            }
                            if(commonAlign != AlignOrStretch.STRETCH) firstSpacer.anchorSize(!isVertical).setTo(1.0)
                            target.moveToFirst(firstSpacer)
                            firstSpacer.anchorSize(isVertical).constraint(finalSpacer.anchorSize(isVertical))
                        }
                        Align.END -> {
                            // Due to the ID generation process, we append then move its position
                            val firstSpacer = target.appendElement("view") {
                                id()
                                this["translatesAutoresizingMaskIntoConstraints"] = "NO"
                                this["${if (isVertical) "vertical" else "horizontal"}HuggingPriority"] = "100"
                            }
                            if(commonAlign != AlignOrStretch.STRETCH) firstSpacer.anchorSize(!isVertical).setTo(1.0)
                            target.moveToFirst(firstSpacer)
                        }
                    }
                }
            }
            "frame" -> {
                for (child in sourceElement.convertibleChildElements) {
                    val childAttributes = child.allAttributes
                    val childElement = convertElement(target, child)
                    val total = myAttributes.insets(
                        "android:padding",
                        resources
                    ) + childAttributes.insets("android:layout_margin", resources)
                    val gravity = childAttributes["android:layout_gravity"]?.toGravity() ?: Gravity()

                    for(vertical in listOf(true, false)) {
                        if (childAttributes["android:layout_${if(vertical) "height" else "width"}"] == "match_parent") {
                            destElement.constraintChildMatchEdgesAxis(childElement, vertical, total)
                        } else {
                            val wrappingPower = if(myAttributes["android:layout_${if(vertical) "height" else "width"}"] == "wrap_content")
                                sourceElement.wrapPower(vertical)
                            else
                                -1
                            destElement.constraintChildFrameAxis(
                                child = childElement,
                                vertical = vertical,
                                align = gravity[vertical],
                                alignLocaleDependent = gravity.localeDependent,
                                insets = total,
                                contentHuggingPriority = if(wrappingPower == -1) framingHugPriority else wrappingPower - 1,
                                contentCompressionResistancePriority = if(wrappingPower == -1) framingCompressionPriority else wrappingPower
                            )
                        }
                    }
                }
            }
            "frame-first-visible" -> {
                val childElements = sourceElement.convertibleChildElements

                val firstChild = childElements.first()
                val childAttributes = firstChild.allAttributes
                val childElement = convertElement(target, firstChild)
                val total = myAttributes.insets(
                    "android:padding",
                    resources
                ) + childAttributes.insets("android:layout_margin", resources)
                val gravity = childAttributes["android:layout_gravity"]?.toGravity() ?: Gravity()

                for(vertical in listOf(true, false)) {
                    if (childAttributes["android:layout_${if(vertical) "height" else "width"}"] == "match_parent") {
                        destElement.constraintChildMatchEdgesAxis(childElement, vertical, total)
                    } else {
                        val wrappingPower = if(myAttributes["android:layout_${if(vertical) "height" else "width"}"] == "wrap_content")
                            sourceElement.wrapPower(vertical)
                        else
                            -1
                        destElement.constraintChildFrameAxis(
                            child = childElement,
                            vertical = vertical,
                            align = gravity[vertical],
                            alignLocaleDependent = gravity.localeDependent,
                            insets = total,
                            contentHuggingPriority = if(wrappingPower == -1) framingHugPriority else wrappingPower - 1,
                            contentCompressionResistancePriority = if(wrappingPower == -1) framingCompressionPriority else wrappingPower
                        )
                    }
                }

                for (child in childElements.drop(1)) {
                    val otherElement = convertElement(target, child)
                    otherElement["hidden"] = "YES"

                    otherElement.anchorCenterX.constraint(childElement.anchorCenterX, identifier = "joiner-x")
                    otherElement.anchorCenterY.constraint(childElement.anchorCenterY, identifier = "joiner-y")
                }
            }
            "scroll-vertical" -> {
                val padding = myAttributes.insets("android:padding", resources)
                for (child in sourceElement.convertibleChildElements) {
                    val innerElement = convertElement(target, child)
                    val paddingPlusMargins = padding + child.allAttributes.insets("android:layout_margin", resources)
                    destElement.constraintChildMatchEdges(innerElement, paddingPlusMargins)
                    innerElement.anchorWidth.constraint(
                        destElement.anchorWidth,
                        constant = -paddingPlusMargins.left - paddingPlusMargins.right
                    )
                }
            }
            "scroll-horizontal" -> {
                val padding = myAttributes.insets("android:padding", resources)
                for (child in sourceElement.convertibleChildElements) {
                    val innerElement = convertElement(target, child)
                    val paddingPlusMargins = padding + child.allAttributes.insets("android:layout_margin", resources)
                    destElement.constraintChildMatchEdges(innerElement, paddingPlusMargins)
                    innerElement.anchorHeight.constraint(
                        destElement.anchorHeight,
                        constant = -paddingPlusMargins.top - paddingPlusMargins.bottom
                    )
                }
            }
            else -> super.handleChildren(rules, childAddRule, sourceElement, destElement, target)
        }
    }

    override fun handleAttributes(
        rules: List<ElementReplacement>,
        allAttributes: Map<String, String>,
        sourceElement: Element,
        destElement: Element
    ) {
        super.handleAttributes(rules, allAttributes, sourceElement, destElement)
        if (sourceElement.tagName == "TextView" && sourceElement["android:layout_width"] != "wrap_content") {
            destElement["numberOfLines"] = "0"
        }
    }

    fun handleXibEntry(destElement: Element, value: AndroidValue, key: String, type: AttributeReplacement.XibRuleType) {
        when (type) {
            AttributeReplacement.XibRuleType.SubNode -> {
                when (value) {
                    is AndroidColorLiteral -> destElement.appendElement("color") {
                        this["key"] = key
                        this["red"] = value.value.redFloat.toString()
                        this["green"] = value.value.greenFloat.toString()
                        this["blue"] = value.value.blueFloat.toString()
                        this["alpha"] = value.value.alphaFloat.toString()
                        this["colorSpace"] = "calibratedRGB"
                    }
                    is AndroidColorResource -> destElement.appendElement("color") {
                        this["key"] = key
                        this["name"] = "color_" + value.name
                    }
                    is AndroidColorStateResource -> destElement.appendElement("color") {
                        this["key"] = key
                        this["name"] = "color_" + value.name
                    }
                    else -> throw IllegalArgumentException("Type ${value::class.simpleName} and rule $type not compatible")
                }
            }
            AttributeReplacement.XibRuleType.Attribute -> {
                when (value) {
                    is AndroidNamedDrawable -> destElement[key] = value.name
                    is AndroidString -> destElement[key] = value.value
                    is AndroidNumber -> destElement[key] = value.value.toString()
                    else -> throw IllegalArgumentException("Type ${value::class.simpleName} and rule $type not compatible")
                }
            }
            AttributeReplacement.XibRuleType.UserDefined -> {
                destElement.getOrAppendChild("userDefinedRuntimeAttributes").apply {
                    getOrAppendChildWithKey("userDefinedRuntimeAttribute", key, "keyPath").apply {
                        when (value) {
                            is AndroidDimension -> {
                                this["type"] = "number"
                                this.getOrAppendChildWithKey("real", "value").apply {
                                    if (key.contains("rotation")) {
                                        this["value"] = value.measurement.number.times(Math.PI).div(180).toString()
                                    } else {
                                        this["value"] = value.measurement.number.toString()
                                    }
                                }
                            }
                            is AndroidString -> {
                                when (value.value) {
                                    "true", "false" -> {
                                        this["type"] = "boolean"
                                        this["value"] = if (value.value.toBoolean()) "YES" else "NO"
                                    }
                                    else -> {
                                        this["type"] = "string"
                                        this["value"] = value.value
                                    }
                                }
                            }
                            is AndroidNamedDrawable -> {
                                this["type"] = "string"
                                this["value"] = value.name
                            }
                            is AndroidColor -> {
                                this["type"] = "color"
                                handleXibEntry(this, value, "value", AttributeReplacement.XibRuleType.SubNode)
                            }
                            else -> TODO()
                        }
                    }
                }
            }
            AttributeReplacement.XibRuleType.StateSubNode -> {
                when (value) {
                    is AndroidColorStateResource -> {
                        value.colors.asMap.entries.forEach { state ->
                            when (val subvalue = state.value?.value) {
                                is AndroidColorLiteral -> {
                                    destElement.getOrAppendChildWithKey("state", state.key.toString().toLowerCase())
                                        .apply {
                                            getOrAppendChildWithKey("color", key).apply {
                                                this["red"] = subvalue.value.redFloat.toString()
                                                this["green"] = subvalue.value.greenFloat.toString()
                                                this["blue"] = subvalue.value.blueFloat.toString()
                                                this["alpha"] = subvalue.value.alphaFloat.toString()
                                                this["colorSpace"] = "calibratedRGB"
                                            }
                                        }
                                }
                                is AndroidColorResource -> {
                                    destElement.getOrAppendChildWithKey("state", state.key.toString().toLowerCase())
                                        .apply {
                                            getOrAppendChildWithKey("color", key).apply {
                                                this["name"] = "color_" + subvalue.name
                                            }
                                        }
                                }
                            }
                        }
                    }
                    else -> {
                        handleXibEntry(
                            destElement.getOrAppendChildWithKey("state", "normal"),
                            value,
                            key,
                            AttributeReplacement.XibRuleType.SubNode
                        )
                    }
                }
            }
            AttributeReplacement.XibRuleType.StateAttribute -> {
                when (value) {
                    is AndroidColorStateResource -> {
                        value.colors.asMap.forEach { state, subValue ->
                            val sub = subValue ?: return@forEach
                            handleXibEntry(
                                destElement.getOrAppendChildWithKey("state", state.name.lowercase()),
                                sub.value,
                                key,
                                AttributeReplacement.XibRuleType.Attribute
                            )
                        }
                    }
                    is AndroidDrawableState -> {
                        value.value.states.asMap.forEach { state, subValue ->
                            val sub = subValue as? AndroidDrawable.Reference ?: return@forEach
                            handleXibEntry(
                                destElement.getOrAppendChildWithKey("state", state.name.lowercase()),
                                sub.drawable.value,
                                key,
                                AttributeReplacement.XibRuleType.Attribute
                            )
                        }
                    }
                    else -> {
                        handleXibEntry(
                            destElement.getOrAppendChildWithKey("state", "normal"),
                            value,
                            key,
                            AttributeReplacement.XibRuleType.Attribute
                        )
                    }
                }
            }
        }
    }

    override fun handleAttribute(
        rules: List<ElementReplacement>,
        sourceElement: Element,
        destElement: Element,
        allAttributes: Map<String, String>,
        key: String,
        raw: String
    ) {
        super.handleAttribute(rules, sourceElement, destElement, allAttributes, key, raw)
        if(key.substringAfterLast(':').startsWith("ios_")) {
            destElement.setAttribute(key.substringAfter(":ios_"), raw)
        }
    }

    override fun handleAttribute(
        allAttributes: Map<String, String>,
        attributeRule: AttributeReplacement,
        destElement: Element,
        value: AndroidValue,
        inStyle: Boolean
    ) {
        usedResources.add(value)
        super.handleAttribute(allAttributes, attributeRule, destElement, value, inStyle)
        attributeRule.xib.entries.sortedBy { it.key }.forEach {
            handleXibEntry(
                destElement.xpathElementOrCreate(it.key.substringBefore("|", "")),
                value,
                it.key.substringAfter("|", it.key),
                it.value
            )
        }
    }

    private fun handleSetSize(
        sourceElement: Element,
        childAttributes: Map<String, String>,
        childElement: Element
    ) {
        childAttributes["android:layout_width"]?.let {
            (resources.read(it) as? AndroidDimension)?.measurement?.number?.takeUnless {
                it == 0.0 && childAttributes["android:layout_weight"] != null && (sourceElement.parentNode as? Element)?.let {
                    it.allAttributes["android:orientation"] != "vertical"
                } == true
            }?.let {
                childElement.anchorWidth.setTo(it, priority = if(childAttributes["android:minWidth"] != null) 999 else 1000)
            }
        }
        childAttributes["android:layout_height"]?.let {
            (resources.read(it) as? AndroidDimension)?.measurement?.number?.takeUnless {
                it == 0.0 && childAttributes["android:layout_weight"] != null && (sourceElement.parentNode as? Element)?.let {
                    it.allAttributes["android:orientation"] == "vertical"
                } == true
            }?.let {
                childElement.anchorHeight.setTo(it, priority = if(childAttributes["android:minHeight"] != null) 999 else 1000)
            }
        }
    }

    fun convertDocument(layout: AndroidLayoutFile, androidXml: Document): Document {
        outlets.clear()

        val xib = baseFile.clone()
        val resourceNode = xib.documentElement.xpathElement("resources")!!
        val fontsNode = xib.documentElement.xpathElement("customFonts")!!
        val objectsNode = xib.documentElement.xpathElement("objects")!!
        val fileOwnerIdentifier = objectsNode.xpathElement("placeholder[@placeholderIdentifier=\"IBFilesOwner\"]")!!
        val view = convertElement(objectsNode, androidXml.documentElement)
        view.id()
        fileOwnerIdentifier["customClass"] = layout.className
        fileOwnerIdentifier["customModule"] = project.moduleName
        fileOwnerIdentifier["customModuleProvider"] = "target"
        fileOwnerIdentifier.getOrAppendChild("connections").apply {
            for (entry in outlets) {
                appendElement("outlet") {
                    this["property"] = "_".plus(entry.key).safeSwiftIdentifier()
                    this["destination"] = entry.key
                    id()
                }
            }
        }
        usedResources
            .asSequence()
            .flatMap {
                when (it) {
                    is AndroidColorStateResource -> it.colors.asMap.values.mapNotNull { it?.value }
                        .asSequence() + sequenceOf(it)
                    is AndroidDrawableState -> it.value.states.asMap.values.mapNotNull { (it as? AndroidDrawable.Reference)?.drawable?.value }
                        .asSequence() + sequenceOf(it)
                    else -> sequenceOf(it)
                }
            }
            .distinct()
            .forEach {
                when (it) {
                    is AndroidFontLiteral -> {
                        val font = it
                        fontsNode.appendElement("array") {
                            this["key"] = font.file.name
                            appendElement("string") {
                                this.textContent = font.postScriptName
                            }
                        }
                    }
                    is AndroidFontSet -> {
                        it.subFonts.map { it.literal.value }.forEach { font ->
                            fontsNode.appendElement("array") {
                                this["key"] = font.file.name
                                appendElement("string") {
                                    this.textContent = font.postScriptName
                                }
                            }
                        }
                    }
                    is AndroidColorResource -> {
                        resourceNode.appendElement("namedColor") {
                            this["name"] = "color_${it.name}"
                            appendElement("color") {
                                this["red"] = it.value.redFloat.toString()
                                this["green"] = it.value.greenFloat.toString()
                                this["blue"] = it.value.blueFloat.toString()
                                this["alpha"] = it.value.alphaFloat.toString()
                                this["colorSpace"] = "custom"
                                this["customColorSpace"] = "sRGB"
                            }
                        }
                    }
                    is AndroidColorStateResource -> {
                        resourceNode.appendElement("namedColor") {
                            this["name"] = "color_${it.name}"
                            appendElement("color") {
                                this["red"] = it.value.redFloat.toString()
                                this["green"] = it.value.greenFloat.toString()
                                this["blue"] = it.value.blueFloat.toString()
                                this["alpha"] = it.value.alphaFloat.toString()
                                this["colorSpace"] = "custom"
                                this["customColorSpace"] = "sRGB"
                            }
                        }
                    }
                    is AndroidNamedDrawableWithSize -> {
                        resourceNode.appendElement("image") {
                            this["name"] = it.name
                            this["width"] = it.size.width.toString()
                            this["height"] = it.size.height.toString()
                        }
                    }
                }
            }
        return xib
    }

    val baseFile = """
        <?xml version="1.0" encoding="UTF-8"?>
        <document type="com.apple.InterfaceBuilder3.CocoaTouch.XIB" version="3.0" toolsVersion="18122" targetRuntime="iOS.CocoaTouch" propertyAccessControl="none" useAutolayout="YES" useTraitCollections="YES" useSafeAreas="YES" colorMatched="YES">
            <device id="retina6_1" orientation="portrait" appearance="light"/>
            <dependencies>
                <deployment identifier="iOS"/>
                <plugIn identifier="com.apple.InterfaceBuilder.IBCocoaTouchPlugin" version="18093"/>
                <capability name="Safe area layout guides" minToolsVersion="9.0"/>
                <capability name="System colors in document resources" minToolsVersion="11.0"/>
                <capability name="documents saved in the Xcode 8 format" minToolsVersion="8.0"/>
            </dependencies>
            <customFonts key="customFonts">
            </customFonts>
            <objects>
                <placeholder placeholderIdentifier="IBFilesOwner" id="-1" userLabel="File's Owner">
                    <attributedString key="userComments">
                        <fragment content="Generated with XmlToXib, will be overwritten"/>
                    </attributedString>
                </placeholder>
                <placeholder placeholderIdentifier="IBFirstResponder" id="-2" customClass="UIResponder"/>
            </objects>
            <resources>
            </resources>
        </document>
    """.trimIndent().readXml()

    fun swiftFile(
        androidXmlName: String,
        xibFile: File,
        variant: String? = null,
    ): IosLayoutFile = IosLayoutFile(
        projectName = project.name,
        name = androidXmlName,
        variants = setOfNotNull(variant),
        files = setOf(xibFile),
        bindings = outlets.mapValues { IosLayoutFile.Hook(it.key, it.value) }
    )
}
