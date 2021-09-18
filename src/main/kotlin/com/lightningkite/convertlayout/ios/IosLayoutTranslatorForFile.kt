package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.android.*
import com.lightningkite.convertlayout.rules.ElementReplacement
import com.lightningkite.convertlayout.rules.Replacements
import com.lightningkite.convertlayout.xml.*
import org.jetbrains.kotlin.konan.file.File
import org.w3c.dom.Document
import org.w3c.dom.Element
import kotlin.math.min

internal class IosLayoutTranslatorForFile(
    val project: IosProject,
    replacements: Replacements,
    resources: AndroidResources
) : AndroidLayoutTranslator(replacements, resources) {

    var outlets: MutableMap<String, SwiftIdentifier> = HashMap()
    val usedResources: MutableSet<AndroidValue> = HashSet()

    override fun convertElement(
        destOwner: Element,
        rules: List<ElementReplacement>,
        sourceElement: Element,
        allAttributes: Map<String, String>
    ): Element {
        val innerAttributes = HashMap<String, String>()
        val outerAttributes = HashMap<String, String>()
        val autowrap = rules.asSequence()
            .flatMap { it.autoWrapFor?.asSequence() ?: sequenceOf() }
            .toSet()
        for((key, value) in allAttributes) {
            if(key in autowrap) outerAttributes[key] = value
            else innerAttributes[key] = value
        }

        if(outerAttributes.isEmpty()) {
            val newElement =
                destOwner.appendFragment(rules.asSequence().mapNotNull { it.template }.first().write { sourceElement.getPath(it) })
            allAttributes["android:id"]
                ?.substringAfter('/')
                ?.also { outlets[it] = newElement.swiftIdentifier() }
                ?.let { newElement["id"] = it }
            newElement.getOrPut("id") { generateId() }
            if(sourceElement.childElements.none()) {
                if(allAttributes["android:layout_width"] == "wrap_content")
                    newElement["horizontalHuggingPriority"] = "750"
                if(allAttributes["android:layout_height"] == "wrap_content")
                    newElement["verticalHuggingPriority"] = "750"
            }

            // Handle children
            handleChildren(rules, newElement, sourceElement)

            // Handle attributes
            handleAttributes(innerAttributes, sourceElement, newElement)

            return newElement
        } else {
            val outerElement = destOwner.appendElement("view")
            outerElement["translatesAutoresizingMaskIntoConstraints"] = "NO"
            val innerElement =
                outerElement.getOrAppendChild("subviews").appendFragment(rules.asSequence().mapNotNull { it.template }.first().write { sourceElement.getPath(it) })
            allAttributes["android:id"]
                ?.substringAfter('/')
                ?.also { outlets[it] = innerElement.swiftIdentifier() }
                ?.let { innerElement["id"] = it }
            outerElement.getOrPut("id") { generateId() }
            innerElement.getOrPut("id") { generateId() }

            if(sourceElement.childElements.none()) {
                if(allAttributes["android:layout_width"] == "wrap_content")
                    innerElement["horizontalHuggingPriority"] = "750"
                if(allAttributes["android:layout_height"] == "wrap_content")
                    innerElement["verticalHuggingPriority"] = "750"
            }

            // Set up constraints between outer/inner based on padding
            val padding = outerAttributes.insets("android:padding", resources)
            outerElement.anchorLeading.constraint(innerElement.anchorLeading, constant = padding.left)
            outerElement.anchorTop.constraint(innerElement.anchorTop, constant = padding.top)
            innerElement.anchorTrailing.constraint(outerElement.anchorTrailing, constant = padding.right)
            innerElement.anchorBottom.constraint(outerElement.anchorBottom, constant = padding.bottom)

            // Handle children
            handleChildren(rules, innerElement, sourceElement)

            // Handle attributes
            handleAttributes(outerAttributes, sourceElement, outerElement)
            handleAttributes(innerAttributes, sourceElement, innerElement)

            return outerElement
        }
    }

    override fun handleChildren(
        rules: List<ElementReplacement>,
        childAddRule: String?,
        sourceElement: Element,
        destElement: Element,
        target: Element
    ) {
        when (childAddRule) {
            "linear" -> {
                val isVertical = sourceElement["android:orientation"] == "vertical"
                val myPadding = sourceElement.allAttributes.insets("android:padding", resources)

                // Find most common alignment
                val defaultAlignInSource: AlignOrStretch = sourceElement["android:gravity"]?.toGravity()?.get(!isVertical)?.orStretch() ?: AlignOrStretch.START
                val childAlignments = sourceElement.childElements
                    .map {
                        if(it["android:layout_${if(isVertical) "width" else "height"}"] == "match_parent") AlignOrStretch.STRETCH
                        else it["android:layout_gravity"]?.toGravity()?.get(!isVertical)?.orStretch() ?: defaultAlignInSource
                    }
                    .toList()
                val commonAlign: AlignOrStretch = childAlignments
                    .groupingBy { it }
                    .eachCount()
                    .entries
                    .maxByOrNull { it.value + if(it.key == AlignOrStretch.STRETCH) 0.1 else 0.0 }
                    ?.key ?: defaultAlignInSource
                val minMargins: Insets = sourceElement.childElements
                    .map { it.allAttributes.insets("android:layout_margin", resources) }
                    .fold(Insets(999.0, 999.0, 999.0, 999.0)) { a, b ->
                        a.left = min(a.left, b.left)
                        a.top = min(a.top, b.top)
                        a.right = min(a.right, b.right)
                        a.bottom = min(a.bottom, b.bottom)
                        a
                    }

                // Set default alignment on stackView
                if(isVertical)
                    when(commonAlign) {
                        AlignOrStretch.START -> destElement["alignment"] = "top"
                        AlignOrStretch.CENTER -> destElement["alignment"] = "center"
                        AlignOrStretch.END -> destElement["alignment"] = "bottom"
                        AlignOrStretch.STRETCH -> {}
                    }
                else
                    when(commonAlign) {
                        AlignOrStretch.START -> destElement["alignment"] = "leading"
                        AlignOrStretch.CENTER -> destElement["alignment"] = "center"
                        AlignOrStretch.END -> destElement["alignment"] = "trailing"
                        AlignOrStretch.STRETCH -> {}
                    }

                // Set default margin matching orientation on stackView via default spacing
                destElement["spacing"] = (minMargins.start(isVertical) + minMargins.end(isVertical)).toString()

                // Set default margin on stackView via adding to content insets
                val myMargins = myPadding + minMargins
                destElement.getOrAppendChildWithKey("edgeInsets", "layoutMargins").apply {
                    if(isVertical) {
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
                sourceElement.children.mapNotNull { it as? Element }.forEachIndexed { index, child ->
                    val childAttributes = child.allAttributes
                    val childAlign: AlignOrStretch = childAlignments[index]
                    val childMargins = childAttributes.insets("android:layout_margin", resources) - minMargins

                    val destChild = if(childAlign == commonAlign && childMargins == Insets.zero) {
                        // If matching common alignment and margins, be direct
                        convertElement(target, child)
                    } else if(childAlign == commonAlign && childMargins.start(!isVertical) == 0.0 && childMargins.end(!isVertical) == 0.0) {
                        // If matching common alignment but not margins, be direct but add spacer views
                        val additionalStartPadding = childMargins.start(isVertical)
                        val additionalEndPadding = childMargins.end(isVertical)

                        if(additionalStartPadding > 0.0) {
                            val spacer = target.appendElement("view")
                            spacer.getOrPut("id") { generateId() }
                            spacer["translatesAutoresizingMaskIntoConstraints"] = "NO"
                            spacer.anchorSize(isVertical).setTo(additionalStartPadding)
                        }
                        val destChild = convertElement(target, child)
                        if(additionalEndPadding > 0.0) {
                            val spacer = target.appendElement("view")
                            spacer.getOrPut("id") { generateId() }
                            spacer["translatesAutoresizingMaskIntoConstraints"] = "NO"
                            spacer.anchorSize(isVertical).setTo(additionalEndPadding)
                        }
                        destChild
                    } else {
                        val outerElement = target.appendElement("view")
                        outerElement.getOrPut("id") { generateId() }
                        outerElement["translatesAutoresizingMaskIntoConstraints"] = "NO"
                        println("Wrapping, $commonAlign -> $childAlign")
                        when(commonAlign) {
                            AlignOrStretch.START -> outerElement.anchorEnd(!isVertical).constraint(destElement.anchorEnd(!isVertical), constant = myMargins.left.also { println("myMargins.left -> $it") })
                            AlignOrStretch.CENTER,
                            AlignOrStretch.END -> destElement.anchorStart(!isVertical).constraint(outerElement.anchorStart(!isVertical), constant = myMargins.right.also { println("myMargins.right -> $it") })
                            AlignOrStretch.STRETCH -> {}
                        }

                        val innerElement = convertElement(outerElement.getOrAppendChild("subviews"), child)
                        outerElement.anchorStart(isVertical).constraint(innerElement.anchorStart(isVertical), constant = childMargins.start(isVertical))
                        innerElement.anchorEnd(isVertical).constraint(outerElement.anchorEnd(isVertical), constant = childMargins.end(isVertical))

                        when(childAlign) {
                            AlignOrStretch.START -> {
                                outerElement.anchorStart(!isVertical).constraint(innerElement.anchorStart(!isVertical), constant = childMargins.start(!isVertical))
                                innerElement.anchorEnd(!isVertical).constraint(outerElement.anchorEnd(!isVertical), relationship = ConstraintRelation.greaterThanOrEqual, constant = childMargins.end(!isVertical))
                            }
                            AlignOrStretch.CENTER -> {
                                outerElement.anchorStart(!isVertical).constraint(innerElement.anchorStart(!isVertical), relationship = ConstraintRelation.greaterThanOrEqual, constant = childMargins.start(!isVertical))
                                innerElement.anchorEnd(!isVertical).constraint(outerElement.anchorEnd(!isVertical), relationship = ConstraintRelation.greaterThanOrEqual, constant = childMargins.end(!isVertical))
                                outerElement.anchorCenter(!isVertical).constraint(innerElement.anchorCenter(!isVertical))
                            }
                            AlignOrStretch.END -> {
                                outerElement.anchorStart(!isVertical).constraint(innerElement.anchorStart(!isVertical), relationship = ConstraintRelation.greaterThanOrEqual, constant = childMargins.start(!isVertical))
                                innerElement.anchorEnd(!isVertical).constraint(outerElement.anchorEnd(!isVertical), constant = childMargins.end(!isVertical))
                            }
                            AlignOrStretch.STRETCH -> {
                                outerElement.anchorStart(!isVertical).constraint(innerElement.anchorStart(!isVertical), constant = childMargins.start(!isVertical))
                                innerElement.anchorEnd(!isVertical).constraint(outerElement.anchorEnd(!isVertical), constant = childMargins.end(!isVertical))
                            }
                        }
                        outerElement
                    }

                    val childWeight = childAttributes["android:layout_weight"]?.toDouble() ?: 0.0
                    if(childWeight > 0) {
                        destChild["${if(isVertical) "vertical" else "horizontal"}HuggingPriority"] = "100"
                        firstWeightedChild?.let {
                            destChild.anchorSize(isVertical).constraint(it.anchorSize(isVertical), multiplier = firstWeightedChildWeight / childWeight)
                        } ?: run {
                            firstWeightedChild = destChild
                            firstWeightedChildWeight = childWeight
                        }
                    }
                    handleSetSize(childAttributes, destChild)
                }

                // If no weights AND size isn't wrap content, add final spacer view
                if(firstWeightedChild == null && sourceElement.allAttributes["android:layout_${if(isVertical) "height" else "width"}"] != "wrap_content") {
                    target.appendElement("view") {
                        getOrPut("id") { generateId() }
                        this["translatesAutoresizingMaskIntoConstraints"] = "NO"
                        this["${if(isVertical) "vertical" else "horizontal"}HuggingPriority"] = "100"
                    }
                }
            }
            "frame" -> {
                val myAttributes = sourceElement.allAttributes
                for (child in sourceElement.children.mapNotNull { it as? Element }) {
                    val childAttributes = child.allAttributes
                    val childElement = convertElement(target, child)
                    val total = myAttributes.insets("android:padding", resources) + childAttributes.insets("android:layout_margin", resources)
                    val gravity = childAttributes["android:layout_gravity"]?.toGravity() ?: Gravity()

                    if(childAttributes["android:layout_width"] == "match_parent") {
                        destElement.anchorLeading.constraint(childElement.anchorLeading, constant = total.left)
                        childElement.anchorTrailing.constraint(destElement.anchorTrailing, constant = total.right)
                    } else when(gravity.horizontal) {
                        Align.START -> destElement.anchorLeading.constraint(childElement.anchorLeading, constant = total.left)
                        Align.CENTER -> childElement.anchorCenterX.constraint(destElement.anchorCenterX)
                        Align.END -> childElement.anchorTrailing.constraint(destElement.anchorTrailing, constant = total.right)
                    }

                    if(childAttributes["android:layout_height"] == "match_parent") {
                        destElement.anchorTop.constraint(childElement.anchorTop, constant = total.left)
                        childElement.anchorBottom.constraint(destElement.anchorBottom, constant = total.right)
                    } else when(gravity.vertical) {
                        Align.START -> destElement.anchorTop.constraint(childElement.anchorTop, constant = total.left)
                        Align.CENTER -> childElement.anchorCenterY.constraint(destElement.anchorCenterY)
                        Align.END -> childElement.anchorBottom.constraint(destElement.anchorBottom, constant = total.right)
                    }

                    handleSetSize(childAttributes, childElement)
                }
            }
            "scroll-vertical" -> {
                val padding = sourceElement.allAttributes.insets("android:padding", resources)
                for (child in sourceElement.children.mapNotNull { it as? Element }) {
                    val innerElement = convertElement(target, child)
                    val paddingPlusMargins = padding + child.allAttributes.insets("android:layout_margin", resources)
                    destElement.anchorLeading.constraint(innerElement.anchorLeading, constant = paddingPlusMargins.left)
                    destElement.anchorTop.constraint(innerElement.anchorTop, constant = paddingPlusMargins.top)
                    innerElement.anchorTrailing.constraint(destElement.anchorTrailing, constant = paddingPlusMargins.right)
                    innerElement.anchorBottom.constraint(destElement.anchorBottom, constant = paddingPlusMargins.bottom)
                    innerElement.anchorWidth.constraint(destElement.anchorWidth, constant = -paddingPlusMargins.left - paddingPlusMargins.right)
                }
            }
            "scroll-horizontal" -> {
                val padding = sourceElement.allAttributes.insets("android:padding", resources)
                for (child in sourceElement.children.mapNotNull { it as? Element }) {
                    val innerElement = convertElement(target, child)
                    val paddingPlusMargins = padding + child.allAttributes.insets("android:layout_margin", resources)
                    destElement.anchorLeading.constraint(innerElement.anchorLeading, constant = paddingPlusMargins.left)
                    destElement.anchorTop.constraint(innerElement.anchorTop, constant = paddingPlusMargins.top)
                    innerElement.anchorTrailing.constraint(destElement.anchorTrailing, constant = paddingPlusMargins.right)
                    innerElement.anchorBottom.constraint(destElement.anchorBottom, constant = paddingPlusMargins.bottom)
                    innerElement.anchorHeight.constraint(destElement.anchorHeight, constant = -paddingPlusMargins.top - paddingPlusMargins.bottom)
                }
            }
            else -> super.handleChildren(rules, childAddRule, sourceElement, destElement, target)
        }
    }

    override fun handleAttribute(sourceElement: Element, destElement: Element, key: String, raw: String) {
        usedResources.add(resources.read(raw))
        super.handleAttribute(sourceElement, destElement, key, raw)
    }

    private fun handleSetSize(
        childAttributes: Map<String, String>,
        childElement: Element
    ) {
        childAttributes["android:layout_width"]?.let {
            (resources.read(it) as? AndroidDimensionValue)?.measurement?.number?.takeUnless { it == 0.0 }?.let {
                childElement.anchorWidth.setTo(it)
            }
        }
        childAttributes["android:layout_height"]?.let {
            (resources.read(it) as? AndroidDimensionValue)?.measurement?.number?.takeUnless { it == 0.0 }?.let {
                childElement.anchorHeight.setTo(it)
            }
        }
    }

    fun convertDocument(layout: AndroidLayoutFile, androidXml: Document): Document {
        outlets.clear()
        val xib = baseFile.clone()
        val resourceNode = xib.documentElement.xpathElement("resources")!!
        val objectsNode = xib.documentElement.xpathElement("objects")!!
        val fileOwnerIdentifier = objectsNode.xpathElement("placeholder[@placeholderIdentifier=\"IBFilesOwner\"]")!!
        val view = convertElement(objectsNode, androidXml.documentElement)
        view.getOrPut("id") { generateId() }
        fileOwnerIdentifier["customClass"] = layout.name + "Xml"
        fileOwnerIdentifier["customModule"] = project.name
        fileOwnerIdentifier.getOrAppendChild("connections").apply {
            for(entry in outlets) {
                appendElement("outlet") {
                    this["property"] = entry.key
                    this["destination"] = entry.key
                    this["id"] = generateId()
                }
            }
        }
        usedResources
            .asSequence()
            .mapNotNull { it as? AndroidColorResource }
            .forEach {
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
            <objects>
                <placeholder placeholderIdentifier="IBFilesOwner" id="-1" userLabel="File's Owner"/>
                <placeholder placeholderIdentifier="IBFirstResponder" id="-2" customClass="UIResponder"/>
            </objects>
            <resources>
            </resources>
        </document>
    """.trimIndent().readXml()

    fun swiftFile(
        layout: AndroidLayoutFile
    ): String {

        fun AndroidIdHook.swiftDeclaration(): String = "@IBOutlet weak public var ${this.name}: ${outlets[this.name]!!.name}${if (this.optional) "?" else "!"}"
        fun AndroidSubLayout.swiftDeclaration(): String = "@IBOutlet weak public var ${this.name}: ${this.layoutXmlClass}${if (this.optional) "?" else "!"}"

        return """
    |//
    |// ${layout.className}.swift
    |// Created by Android Xml to iOS Xib Translator
    |//
    |
    |import XmlToXibRuntime
    |${outlets.values.map { it.module }.toSet().joinToString("\n|") { "import $it" }}
    |
    |class ${layout.className}: XibView {
    |
    |    ${layout.bindings.values.joinToString("\n|    ") { it.swiftDeclaration() }}
    |    ${layout.sublayouts.values.joinToString("\n|    ") { it.swiftDeclaration() }}
    |
    |    override func awakeFromNib() {
    |        super.awakeFromNib()
    |    }
    |}
    """.trimMargin("|")
    }
}
