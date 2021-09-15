package com.lightningkite.convertlayout.android

import com.lightningkite.convertlayout.xml.*
import org.mabb.fontverter.FontVerter
import org.w3c.dom.Element
import org.w3c.dom.Text
import java.io.File

private val whitespaceRegexContent = "\\s+"
private val whitespaceRegex = Regex(whitespaceRegexContent)

class AndroidResources {
    var styles: MutableMap<String, AndroidStyle> = HashMap()
    val colors: MutableMap<String, AndroidColorValue> = HashMap()
    val drawables: MutableMap<String, AndroidDrawable> = HashMap()
    val fonts: MutableMap<String, AndroidFont> = HashMap()
    val strings: MutableMap<String, AndroidStringResource> = HashMap()
    val dimensions: MutableMap<String, AndroidDimensionResource> = HashMap()
    val layouts: MutableMap<String, AndroidLayoutResource> = HashMap()

    val all: Map<String, AndroidValue> get() = listOf(styles, colors, drawables, fonts, strings, dimensions, layouts)
        .reduce<Map<String, AndroidValue>, Map<String, AndroidValue>>{ a, b -> a + b}

    fun read(value: String): AndroidValue {
        return when {
            value.startsWith('#') -> AndroidColor(value.hashColorToParts())
            value.startsWith("@style/") -> styles[value.substringAfter('/')]!!
            value.startsWith("@layout/") -> layouts[value.substringAfter('/')]!!
            value.startsWith("@font/") -> fonts[value.substringAfter('/')]!!
            value.startsWith("@mipmap/") -> drawables[value.substringAfter('/')]!!
            value.startsWith("@drawable/") -> drawables[value.substringAfter('/')]!!
            value.startsWith("@android:color/") -> AndroidColor(ColorInParts.basicColors[value.substringAfter('/')]!!)
            value.startsWith("@color/") -> colors[value.substringAfter('/')]!!
            value.startsWith("@string/") -> strings[value.substringAfter('/')]!!
            value.startsWith("@dimen/") -> dimensions[value.substringAfter('/')]!!
            value.endsWith("dp") -> AndroidDimension(
                Measurement(
                    number = value.filter { it.isDigit() || it == '.' }.toDouble(),
                    unit = MeasurementUnit.DP
                )
            )
            value.endsWith("dip") -> AndroidDimension(
                Measurement(
                    number = value.filter { it.isDigit() || it == '.' }.toDouble(),
                    unit = MeasurementUnit.DP
                )
            )
            value.endsWith("sp") -> AndroidDimension(
                Measurement(
                    number = value.filter { it.isDigit() || it == '.' }.toDouble(),
                    unit = MeasurementUnit.SP
                )
            )
            value.endsWith("sip") -> AndroidDimension(
                Measurement(
                    number = value.filter { it.isDigit() || it == '.' }.toDouble(),
                    unit = MeasurementUnit.SP
                )
            )
            value.endsWith("px") -> AndroidDimension(
                Measurement(
                    number = value.filter { it.isDigit() || it == '.' }.toDouble(),
                    unit = MeasurementUnit.PX
                )
            )
            value.toDoubleOrNull() != null -> AndroidNumber(value.toDouble())
            else -> AndroidString(value)
        }
    }
    
    inline fun <reified T: AndroidValue> readLazy(value: String): Lazy<T> = Lazy(value) { read(value) as T }
    @JvmName("readLazy1")
    inline fun <reified T: AndroidValue> String.readLazy(): Lazy<T> = Lazy(this) { read(this) as T }

    fun parse(androidResourcesDirectory: File) {
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
    }

    private fun getStyles(file: File) {
        if(!file.exists()) return
        file.readXml().documentElement.childElements
            .forEach {
                styles[it["name"]!!] = AndroidStyle(
                    name = it["name"]!!,
                    map = it.childElements
                        .filter { it.tagName == "style" }
                        .associate {
                            it["name"]!! to it.children
                                .filter { it is Text }
                                .joinToString { it.textContent }
                                .trim()
                        }
                )
            }
    }

    private fun getDrawables(androidResourcesDirectory: File) {
        if(!androidResourcesDirectory.exists()) return
        androidResourcesDirectory.listFiles()!!
            .filter { it.name.startsWith("drawable") }
            .forEach { base ->
                val typeName = base.name.substringAfter("drawable-", "")
                for (file in base.listFiles()!!) {
                    val name = file.nameWithoutExtension
                    when(file.extension){
                        "png" -> {
                            drawables[name]?.let { it as? AndroidBitmap }?.let {
                                drawables[name] = it.copy(files = it.files + (typeName to file))
                            } ?: run {
                                drawables[name] = AndroidBitmap(name, mapOf(typeName to file))
                            }
                        }
                        "xml" -> {
                            val element = file.readXml().documentElement
                            if(element.tagName == "vector") {
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
        var normal: AndroidDrawableXml? = null
        var selected: AndroidDrawableXml? = null
        var highlighted: AndroidDrawableXml? = null
        var disabled: AndroidDrawableXml? = null
        var focused: AndroidDrawableXml? = null
        element
            .childElements
            .filter { it.tagName == "item" }
            .forEach { subnode ->
                val c: AndroidDrawableXml? = subnode["android:drawable"]
                    ?.substringAfter('/')
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
        return AndroidDrawableState.Value(StateSelector(
            normal = normal!!,
            selected = selected,
            highlighted = highlighted,
            disabled = disabled,
            focused = focused
        ))
    }
    private fun parseXmlShape(element: Element): AndroidShape.Value {
        val strokeElement = element.childElements.find { it.tagName == "stroke" }
        val solidElement = element.childElements.find { it.tagName == "solid" }
        val gradientElement = element.childElements.find { it.tagName == "gradient" }
        val cornersElement = element.childElements.find { it.tagName == "corners" }
        return AndroidShape.Value(
            shapeType = if(element["android:shape"] == "oval") AndroidShape.Value.ShapeType.Oval else AndroidShape.Value.ShapeType.Rectangle,
            stroke = strokeElement?.get("android:color")?.readLazy(),
            strokeWidth = strokeElement?.get("android:width")?.readLazy(),
            fill = solidElement?.get("android:color")?.readLazy(),
            gradient = gradientElement?.let {
                AndroidShape.Value.Gradient(
                    startColor = strokeElement?.get("startColor")?.readLazy() ?: Lazy(AndroidColor(ColorInParts.transparent)),
                    centerColor = strokeElement?.get("centerColor")?.readLazy(),
                    endColor = strokeElement?.get("endColor")?.readLazy() ?: Lazy(AndroidColor(ColorInParts.transparent)),
                    angle = strokeElement?.get("angle")?.toDoubleOrNull() ?: 0.0,
                )
            },
            topLeftCorner = cornersElement?.get("android:topLeftRadius")?.readLazy(),
            topRightCorner = cornersElement?.get("android:topRightRadius")?.readLazy(),
            bottomLeftCorner = cornersElement?.get("android:bottomLeftRadius")?.readLazy(),
            bottomRightCorner = cornersElement?.get("android:bottomRightRadius")?.readLazy(),
        )
    }
    private fun parseXmlLayerList(element: Element): AndroidLayer.Value {
        return AndroidLayer.Value(element.childElements.map {
            AndroidLayer.Layer(
                drawable = it["android:drawable"]
                    ?.substringAfter('/')
                    ?.let { AndroidDrawable.Reference(readLazy(it)) }
                    ?: it.childElements.firstOrNull()
                        ?.let { parseXmlDrawable(it) } ?: throw IllegalStateException(),
                width = element["android:width"]?.readLazy(),
                height = element["android:height"]?.readLazy(),
                top = element["android:top"]?.readLazy(),
                left = element["android:left"]?.readLazy(),
                right = element["android:right"]?.readLazy(),
                bottom = element["android:bottom"]?.readLazy(),
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
        println("Looking for fonts in ${folder}...")
        //fonts themselves first
        folder.listFiles()!!
            .filter { it.extension.toLowerCase() == "otf" || it.extension.toLowerCase() == "ttf" }
            .forEach { file ->
                try {
                    val font = FontVerter.readFont(file)
                    if (!font.isValid) {
                        font.normalize()
                    }
                    val iosFont = AndroidFont(
                        family = font.properties.family.filter { it in ' '..'~' },
                        name = font.name.filter { it in '!'..'~' },
                        file = file
                    )
                    println("Found font $iosFont")
                    fonts[file.nameWithoutExtension] = iosFont
                } catch (e: Exception) {
                    println("Font read failed for $file")
                    e.printStackTrace()
                    fonts[file.nameWithoutExtension] = AndroidFont(
                        family = file.nameWithoutExtension,
                        name = file.nameWithoutExtension,
                        file = file
                    )
                }
            }
        folder.listFiles()!!
            .filter { it.extension.toLowerCase() == "xml" }
            .forEach { file ->
                println("Found font set $file")
                val xml = file.readXml().documentElement
                xml.childElements
                    .filter { it.tagName == "font" }
                    .mapNotNull { it["android:font"] }
                    .forEach {
                        val name = it.substringAfter('/')
                        fonts[file.nameWithoutExtension] =
                            fonts[name] ?: throw IllegalArgumentException("No font $name found")
                    }
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
        var normal: Lazy<AndroidColorValue> = Lazy(AndroidColor(ColorInParts.transparent))
        var selected: Lazy<AndroidColorValue>? = null
        var highlighted: Lazy<AndroidColorValue>? = null
        var disabled: Lazy<AndroidColorValue>? = null
        var focused: Lazy<AndroidColorValue>? = null
        file.readXml().documentElement
            .childElements
            .filter { it.tagName == "item" }
            .forEach { subnode ->
                val raw = subnode["android:color"] ?: ""
                val c = readLazy<AndroidColorValue>(raw)
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
}