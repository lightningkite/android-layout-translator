package com.lightningkite.convertlayout.android

import com.lightningkite.convertlayout.xml.*
import org.apache.fontbox.ttf.OTFParser
import org.apache.fontbox.ttf.TTFParser
import org.w3c.dom.Element
import org.w3c.dom.Text
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap

private val whitespaceRegexContent = "\\s+"
private val whitespaceRegex = Regex(whitespaceRegexContent)

class AndroidResources {
    var packageName: String = ""
    var styles: MutableMap<String, AndroidStyle> = TreeMap()
    val colors: MutableMap<String, AndroidColor> = TreeMap()
    val drawables: MutableMap<String, AndroidDrawable> = TreeMap()
    val fonts: MutableMap<String, AndroidFont> = TreeMap()
    val strings: MutableMap<String, AndroidStringResource> = TreeMap()
    val dimensions: MutableMap<String, AndroidDimensionResource> = TreeMap()
    val layouts: MutableMap<String, AndroidLayoutResource> = TreeMap()

    val all: Map<String, AndroidValue>
        get() = listOf(styles, colors, drawables, fonts, strings, dimensions, layouts)
            .reduce<Map<String, AndroidValue>, Map<String, AndroidValue>> { a, b -> a + b }

    fun read(value: String): AndroidValue {
        return when {
            value.startsWith("?android:attr/") -> AndroidAttrReference(value.substringAfter("?android:attr/"))
            value.startsWith('#') -> AndroidColorLiteral(value.hashColorToParts())
            value.startsWith("@style/") -> styles[value.substringAfter('/')] ?: AndroidStyle(
                value.substringAfter('/'),
                mapOf()
            )
            value.startsWith("@layout/") -> layouts[value.substringAfter('/')]
                ?: throw IllegalStateException("Reference $value not found")
            value.startsWith("@font/") -> fonts[value.substringAfter('/')]
                ?: throw IllegalStateException("Reference $value not found")
            value.startsWith("@mipmap/") -> drawables[value.substringAfter('/')]
                ?: throw IllegalStateException("Reference $value not found")
            value.startsWith("@drawable/") -> drawables[value.substringAfter('/')]
                ?: throw IllegalStateException("Reference $value not found")
            value.startsWith("@android:color/") -> AndroidColorLiteral(
                ColorInParts.basicColors[value.substringAfter('/')]
                    ?: throw IllegalStateException("Reference $value not found")
            )
            value.startsWith("@color/") -> colors[value.substringAfter('/')]
                ?: throw IllegalStateException("Reference $value not found")
            value.startsWith("@string/") -> strings[value.substringAfter('/')]
                ?: throw IllegalStateException("Reference $value not found")
            value.startsWith("@dimen/") -> dimensions[value.substringAfter('/')]
                ?: throw IllegalStateException("Reference $value not found")
            value.endsWith("dp") -> AndroidDimensionLiteral(
                Measurement(
                    number = value.filter { it.isDigit() || it == '.' }.toDouble(),
                    unit = MeasurementUnit.DP
                )
            )
            value.endsWith("dip") -> AndroidDimensionLiteral(
                Measurement(
                    number = value.filter { it.isDigit() || it == '.' }.toDouble(),
                    unit = MeasurementUnit.DP
                )
            )
            value.endsWith("sp") -> AndroidDimensionLiteral(
                Measurement(
                    number = value.filter { it.isDigit() || it == '.' }.toDouble(),
                    unit = MeasurementUnit.SP
                )
            )
            value.endsWith("sip") -> AndroidDimensionLiteral(
                Measurement(
                    number = value.filter { it.isDigit() || it == '.' }.toDouble(),
                    unit = MeasurementUnit.SP
                )
            )
            value.endsWith("px") -> AndroidDimensionLiteral(
                Measurement(
                    number = value.filter { it.isDigit() || it == '.' }.toDouble(),
                    unit = MeasurementUnit.PX
                )
            )
            value.toDoubleOrNull() != null -> AndroidNumber(value.toDouble())
            else -> AndroidStringLiteral(
                value
                    .removePrefix("\\")
                    .replace("\\n", "\n")
                    .replace("\\t", "\t")
                    .replace("\\\"", "\"")
                    .replace("\\'", "'")
            )
        }
    }

    inline fun <reified T : AndroidValue> readLazy(value: String): Lazy<T> = Lazy(value) {
        val v = read(value)
        if (v !is T) throw IllegalStateException("Read $value (a ${v::class.simpleName}) but expected a ${T::class.simpleName}")
        v
    }

    @JvmName("readLazy1")
    inline fun <reified T : AndroidValue> String.readLazy(): Lazy<T> = Lazy(this) {
        val v = read(this)
        if (v !is T) throw IllegalStateException("Read $this (a ${v::class.simpleName}) but expected a ${T::class.simpleName}")
        v
    }

    fun parse(androidResourcesDirectory: File) {
        this.packageName = androidResourcesDirectory.resolve("../AndroidManifest.xml").readXml().documentElement["package"] ?: "unknown"
        getFonts(androidResourcesDirectory.resolve("font"))
        getStrings(androidResourcesDirectory)
        getDimensions(androidResourcesDirectory.resolve("values/dimens.xml"))
        getColors(androidResourcesDirectory.resolve("values/colors.xml"))
        androidResourcesDirectory.resolve("color").listFiles()?.forEach {
            getStateColor(it)
        }
        getDrawables(androidResourcesDirectory)
        getStyles(androidResourcesDirectory.resolve("values/styles.xml"))
        getStyles(androidResourcesDirectory.resolve("values/themes.xml"))
        layouts.putAll(
            AndroidLayoutFile.parseAll(androidResourcesDirectory, this)
                .mapValues { AndroidLayoutResource(it.key, Lazy(it.value)) })
    }

    private fun getStyles(file: File) {
        if (!file.exists()) return
        file.readXml().documentElement.childElements
            .filter { it.tagName == "style" }
            .forEach {
                styles[it["name"]!!] = AndroidStyle(
                    name = it["name"]!!,
                    map = it.childElements
                        .filter { it.tagName == "item" }
                        .associate {
                            it["name"]!! to it.children
                                .filter { it is Text }
                                .joinToString { it.textContent }
                                .trim()
                        },
                    parent = it["parent"]?.let {
                        Lazy(it) {
                            styles[it] ?: styles[it.removePrefix("@style/")] ?: AndroidStyle("", mapOf())
                        }
                    }
                )
            }
    }

    private fun getDrawables(androidResourcesDirectory: File) {
        if (!androidResourcesDirectory.exists()) return
        androidResourcesDirectory.listFiles()!!
            .filter { it.name.startsWith("drawable") }
            .forEach { base ->
                val typeName = base.name.substringAfter("drawable-", "")
                for (file in base.listFiles()!!) {
                    val name = file.nameWithoutExtension
                    when (file.extension) {
                        "png" -> {
                            drawables[name]?.let { it as? AndroidBitmap }?.let {
                                drawables[name] = it.copy(files = it.files + (typeName to file))
                            } ?: run {
                                drawables[name] = AndroidBitmap(name, mapOf(typeName to file))
                            }
                        }
                        "xml" -> {
                            val element = file.readXml().documentElement
                            if (element.tagName == "vector") {
                                drawables[name] = AndroidVector(name, file)
                            } else {
                                val d = parseXmlDrawable(element) ?: continue
                                drawables[name] = d.toResource(file)
                            }
                        }
                    }
                }
            }
    }

    private fun parseXmlDrawable(element: Element): AndroidDrawableXml? {
        return when (element.tagName) {
            "selector" -> parseXmlSelector(element)
            "shape" -> parseXmlShape(element)
            "layer-list" -> parseXmlLayerList(element)
            "bitmap" -> parseXmlBitmap(element)
            else -> null
        }
    }

    private fun parseXmlSelector(element: Element): AndroidDrawableState.Value {
        var normal: AndroidDrawableXml = AndroidShape.Value(AndroidShape.Value.ShapeType.Rectangle)
        var selected: AndroidDrawableXml? = null
        var highlighted: AndroidDrawableXml? = null
        var disabled: AndroidDrawableXml? = null
        var focused: AndroidDrawableXml? = null
        element
            .childElements
            .filter { it.tagName == "item" }
            .forEach { subnode ->
                val c: AndroidDrawableXml? = subnode["android:drawable"]
                    ?.let { AndroidDrawable.Reference(readLazy(it)) }
                    ?: subnode.childElements.firstOrNull()
                        ?.let { parseXmlDrawable(it) }
                when {
                    subnode["android:state_enabled"] == "false" -> disabled = c
                    subnode["android:state_pressed"] == "true" -> highlighted = c
                    subnode["android:state_checked"] == "true" -> selected = c
                    subnode["android:state_selected"] == "true" -> selected = c
                    subnode["android:state_focused"] == "true" -> focused = c
                    c != null -> normal = c
                }
            }
        return AndroidDrawableState.Value(
            StateSelector(
                normal = normal,
                selected = selected,
                highlighted = highlighted,
                disabled = disabled,
                focused = focused
            )
        )
    }

    private fun parseXmlShape(element: Element): AndroidShape.Value {
        val strokeElement = element.childElements.find { it.tagName == "stroke" }
        val solidElement = element.childElements.find { it.tagName == "solid" }
        val gradientElement = element.childElements.find { it.tagName == "gradient" }
        val cornersElement = element.childElements.find { it.tagName == "corners" }
        val defaultRadius = cornersElement?.get("android:radius")?.readLazy<AndroidDimension>()
        return AndroidShape.Value(
            shapeType = if (element["android:shape"] == "oval") AndroidShape.Value.ShapeType.Oval else AndroidShape.Value.ShapeType.Rectangle,
            stroke = strokeElement?.get("android:color")?.readLazy(),
            strokeWidth = strokeElement?.get("android:width")?.readLazy(),
            fill = solidElement?.get("android:color")?.readLazy(),
            gradient = gradientElement?.let {
                AndroidShape.Value.Gradient(
                    startColor = it.get("android:startColor")?.readLazy()!!,
                    centerColor = it.get("android:centerColor")?.readLazy(),
                    endColor = it.get("android:endColor")?.readLazy()!!,
                    angle = it.get("android:angle")?.toDoubleOrNull() ?: 0.0,
                )
            },
            topLeftCorner = cornersElement?.get("android:topLeftRadius")?.readLazy() ?: defaultRadius,
            topRightCorner = cornersElement?.get("android:topRightRadius")?.readLazy() ?: defaultRadius,
            bottomLeftCorner = cornersElement?.get("android:bottomLeftRadius")?.readLazy() ?: defaultRadius,
            bottomRightCorner = cornersElement?.get("android:bottomRightRadius")?.readLazy() ?: defaultRadius,
        )
    }

    private fun parseXmlLayerList(element: Element): AndroidLayer.Value {
        return AndroidLayer.Value(element.childElements.map {
            AndroidLayer.Layer(
                drawable = it["android:drawable"]
                    ?.let { AndroidDrawable.Reference(readLazy(it)) }
                    ?: it.childElements.firstOrNull()
                        ?.let { parseXmlDrawable(it) } ?: throw IllegalStateException(),
                width = it["android:width"]?.readLazy(),
                height = it["android:height"]?.readLazy(),
                top = it["android:top"]?.readLazy(),
                left = it["android:left"]?.readLazy(),
                right = it["android:right"]?.readLazy(),
                bottom = it["android:bottom"]?.readLazy(),
            )
        }.toList())
    }

    private fun parseXmlBitmap(element: Element): AndroidDrawableXml {
        return AndroidBitmap.Reference(
            base = element["android:src"]?.readLazy() ?: throw IllegalStateException(),
            tint = element["android:tint"]?.readLazy()
        )
    }

    private fun getStrings(file: File) {
        if (!file.exists()) return
        file
            .listFiles()!!
            .asSequence()
            .filter { it.name.startsWith("values") }
            .map { it.resolve("strings.xml") }
            .filter { it.exists() }
            .flatMap {
                val category = it.name.substringAfter('-', "")
                it.readXml().documentElement.childElements
                    .filter { it.tagName == "string" }
                    .map {
                        val name = it["name"]!!
                        Triple(
                            category,
                            name,
                            it.textContent.replace(whitespaceRegex, " ")
                                .replace("\\n", "\n")
                                .replace("\\t", "\t")
                                .replace("\\'", "\'")
                                .replace("\\\"", "\"")
                                .trim()
                        )
                    }
            }
            .groupBy { it.second }
            .forEach {
                strings[it.key] = AndroidStringResource(
                    name = it.key,
                    value = it.value.find { it.first == "" }?.third ?: it.value.first().third,
                    languages = it.value.associate { it.first to it.third }
                )
            }
    }

    private fun getFonts(folder: File) {
        if (!folder.exists()) return
        if (!folder.isDirectory) return
        //fonts themselves first
        folder.listFiles()!!
            .filter { it.extension.toLowerCase() == "otf" || it.extension.toLowerCase() == "ttf" }
            .forEach { file ->
                val font = when(file.extension.lowercase()) {
                    "ttf" -> {
                        TTFParser().parse(file)
                    }
                    "otf" -> {
                        OTFParser().parse(file)
                    }
                    else -> throw NotImplementedError()
                }
                val iosFont = AndroidFontLiteral(
                    fontSuperFamily = font.naming.getName(16, 1, 0, 0) ?: font.naming.nameRecords.find { it.nameId == 16 }?.string ?: "",
                    fontFamily = font.naming.fontFamily,
                    fontSubFamily = font.naming.fontSubFamily,
                    postScriptName = font.naming.postScriptName,
                    file = file
                )
                fonts[file.nameWithoutExtension] = iosFont
            }
        folder.listFiles()!!
            .filter { it.extension.toLowerCase() == "xml" }
            .forEach { file ->
                val xml = file.readXml().documentElement
                fonts[file.nameWithoutExtension] = AndroidFontSet(
                    xml.childElements
                        .filter { it.tagName == "font" }
                        .mapNotNull {
                            AndroidFontSet.SubFont(
                                style = it["android:fontStyle"] ?: "normal",
                                weight = it["android:fontWeight"]?.toIntOrNull() ?: 700,
                                literal = readLazy(
                                    it["app:font"] ?: it["android:font"] ?: return@mapNotNull null,
                                )
                            )
                        }
                        .toList()
                )
            }
    }

    private fun getDimensions(file: File) {
        if (!file.exists()) return
        file.readXml().documentElement
            .childElements
            .filter { it.tagName == "dimen" }
            .forEach {
                val raw = it.textContent
                val name = it["name"]!!
                dimensions[name] = AndroidDimensionResource(
                    name = name,
                    measurement = Measurement(
                        number = raw.filter { it.isDigit() || it == '.' }.toDouble(),
                        unit = raw.filter { it.isLetter() }.toLowerCase().let {
                            when (it) {
                                "px" -> MeasurementUnit.PX
                                "dp", "dip" -> MeasurementUnit.DP
                                "sp", "sip" -> MeasurementUnit.SP
                                else -> MeasurementUnit.PX
                            }
                        })
                )
            }
    }

    private fun getColors(file: File) {
        if (!file.exists()) return
        val colorsToProcess = ArrayList<Pair<String, String>>()
        file.readXml().documentElement
            .childElements
            .filter { it.tagName == "color" }
            .forEach {
                val raw = it.textContent
                val name = (it["name"] ?: "noname")
                when {
                    raw.startsWith("@color/") -> {
                        val colorName = raw.removePrefix("@color/")
                        colorsToProcess.add(name to colorName)
                    }
                    raw.startsWith("@android:color/") -> {
                        val colorName = raw.removePrefix("@android:color/")
                        colorsToProcess.add(name to colorName)
                    }
                    raw.startsWith("#") -> {
                        this.colors[name] = AndroidColorResource(
                            name = name,
                            value = raw.hashColorToParts()
                        )
                    }
                    else -> {
                    }
                }
            }
        while (colorsToProcess.isNotEmpty()) {
            val popped = colorsToProcess.removeAt(0)
            this.colors[popped.second]?.let {
                this.colors[popped.first] = it
            } ?: colorsToProcess.find { it.first == popped.second }?.let {
                colorsToProcess.add(popped.first to it.second)
            }
        }
    }

    private fun getStateColor(file: File) {
        if (!file.exists()) return
        var normal: Lazy<AndroidColor> = Lazy(AndroidColorLiteral(ColorInParts.transparent))
        var selected: Lazy<AndroidColor>? = null
        var highlighted: Lazy<AndroidColor>? = null
        var disabled: Lazy<AndroidColor>? = null
        var focused: Lazy<AndroidColor>? = null
        file.readXml().documentElement
            .childElements
            .filter { it.tagName == "item" }
            .forEach { subnode ->
                val raw = subnode["android:color"] ?: ""
                val c = readLazy<AndroidColor>(raw)
                when {
                    subnode["android:state_enabled"] == "false" -> disabled = c
                    subnode["android:state_pressed"] == "true" -> highlighted = c
                    subnode["android:state_checked"] == "true" -> selected = c
                    subnode["android:state_selected"] == "true" -> selected = c
                    subnode["android:state_focused"] == "true" -> focused = c
                    else -> normal = c
                }
            }
        colors[file.nameWithoutExtension] = AndroidColorStateResource(
            name = file.nameWithoutExtension, colors = StateSelector(
                normal = normal,
                selected = selected,
                highlighted = highlighted,
                disabled = disabled,
                focused = focused
            )
        )
    }

    fun HasGet.getPath(path: String): String = (this as Any).getPath(path)
    fun Element.getPath(path: String): String = (this as Any).getPath(path)
    fun Map<String, *>.getPath(path: String): String = (this as Any).getPath(path)
    private fun Any.getPath(path: String): String {
        var current: Any? = this
        for (part in path.split('.')) {
            while (current is Lazy<*>) {
                current = current.value
            }
            current = when (current) {
                is HasGet -> current[part]
                is Element -> when(part) {
                    "halfSize" -> (current.findSize()!! / 2).toString()
                    else -> current[part]?.let { read(it) }
                }
                is Map<*, *> -> current[part]
                else -> return current.toString()
            }
        }
        while (current is Lazy<*>) {
            current = current.value
        }
        return current.toString()
    }

    fun Element.findSize(): Double? {
        return this["android:layout_width"]?.let { read(it) as? AndroidDimension }?.measurement?.number
            ?: this["android:layout_height"]?.let { read(it) as? AndroidDimension }?.measurement?.number
            ?: (this.parentNode as? Element)?.findSize()?.let {
                it - (
                        this["android:padding"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_margin"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:paddingLeft"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_marginLeft"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:paddingRight"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_marginRight"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:paddingStart"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_marginStart"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:paddingEnd"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_marginEnd"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:paddingTop"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_marginTop"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:paddingBottom"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_marginBottom"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:paddingHorizontal"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_marginHorizontal"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:paddingVertical"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: this["android:layout_marginVertical"]?.let { read(it) as? AndroidDimension }?.measurement?.number?.times(2)
                            ?: 0.0
                        )
            }
    }
}