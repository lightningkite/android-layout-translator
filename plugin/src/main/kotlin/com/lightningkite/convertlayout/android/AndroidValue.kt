package com.lightningkite.convertlayout.android

import com.lightningkite.convertlayout.util.DeferMap
import com.lightningkite.convertlayout.util.camelCase
import com.lightningkite.convertlayout.xml.get
import com.lightningkite.convertlayout.xml.readXml
import java.io.File
import javax.imageio.ImageIO

data class Size(val width: Int, val height: Int) {
    operator fun times(amount: Int): Size = Size(
        width * amount,
        height * amount,
    )

    operator fun div(amount: Int): Size = Size(
        width / amount,
        height / amount,
    )
}

class Lazy<T>(val name: String, val getter: () -> T) {
    constructor(direct: T) : this(direct.toString(), { direct })

    private var _value: T? = null
    private var retrieved: Boolean = false
    val value: T
        get() {
            if (!retrieved) _value = getter()
            @Suppress("UNCHECKED_CAST")
            return _value as T
        }

    override fun toString(): String = "Lazy('$name')"
}

interface HasGet {
    operator fun get(key: String): Any?
}

sealed interface AndroidValue : HasGet {
}

sealed interface AndroidDimension : AndroidValue {
    val measurement: Measurement
    override fun get(key: String): Any? = measurement[key]
        ?: throw IllegalArgumentException("No key $key for ${this::class.simpleName}")
}

sealed interface AndroidDrawable : AndroidValue {
    data class Reference(
        val drawable: Lazy<AndroidDrawable>
    ) : AndroidDrawableXml {
        override fun toResource(file: File): AndroidDrawable = TODO()
        override fun get(key: String): Any? = drawable.value[key]
    }
}

sealed interface AndroidNamedDrawable : AndroidDrawable {
    val name: String
}

sealed interface AndroidDrawableWithSize: AndroidDrawable {
    val size: Size
}

sealed interface AndroidNamedDrawableWithSize : AndroidNamedDrawable, AndroidDrawableWithSize

sealed interface AndroidXmlDrawable : AndroidNamedDrawable {
    val value: AndroidDrawableXml
}

sealed interface AndroidDrawableXml : HasGet {
    fun toResource(file: File): AndroidDrawable
}

sealed interface AndroidColor : AndroidDrawable {
    val value: ColorInParts
    override fun get(key: String): Any? = value[key]
        ?: throw IllegalArgumentException("No key $key for ${this::class.simpleName}")
}

sealed interface AndroidString : AndroidValue {
    val value: String
    override fun get(key: String): Any? = when (key) {
        "escaped" -> value
            .replace("\n", "&#xA;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
            .replace("&", "&amp;")
            .replace("\t", "&#x9;")
        else -> throw IllegalArgumentException("No key $key for ${this::class.simpleName}")
    }
}

sealed interface AndroidFont : AndroidValue {
}

data class AndroidFontLiteral(
    val family: String,
    val name: String,
    val file: File? = null
) : AndroidFont {

    override fun get(key: String): Any? = when (key) {
        "family" -> family
        "name" -> name
        "file" -> file?.toString() ?: ""
        else -> throw IllegalArgumentException("No key $key for ${this::class.simpleName}")
    }
}

data class AndroidFontSet(
    val subFonts: List<Lazy<AndroidFontLiteral>>
) : AndroidFont {

    override fun get(key: String): Any? = throw IllegalArgumentException("No key $key for ${this::class.simpleName}")
}

data class AndroidColorLiteral(
    override val value: ColorInParts
) : AndroidColor {
}

data class AndroidColorResource(
    val name: String,
    override val value: ColorInParts
) : AndroidColor {

    override fun get(key: String): Any? = when (key) {
        "name" -> name
        else -> super.get(key)
    }
}

data class AndroidColorStateResource(
    val name: String,
    val colors: StateSelector<Lazy<AndroidColor>>
) : AndroidColor {
    override val value: ColorInParts get() = colors.normal.value.value

    override fun get(key: String): Any? = when (key) {
        "name" -> name
        "normal" -> colors.normal
        "selected" -> colors.selected
        "highlighted" -> colors.highlighted
        "disabled" -> colors.disabled
        "focused" -> colors.focused
        else -> super.get(key)
    }
}

data class AndroidVector(
    override val name: String,
    val file: File
) : AndroidNamedDrawableWithSize {

    override val size: Size by lazy {
        file.readXml().documentElement.let {
            Size(
                width = it["android:width"]!!.filter { it.isDigit() || it == '.' }.toDouble().toInt(),
                height = it["android:height"]!!.filter { it.isDigit() || it == '.' }.toDouble().toInt()
            )
        }
    }

    override fun get(key: String): Any? = when (key) {
        "name" -> name
        else -> throw IllegalArgumentException("No key $key for ${this::class.simpleName}")
    }
}

data class AndroidBitmap(
    override val name: String,
    val files: Map<String, File>,
//    override val width: Int,
//    override val height: Int,
) : AndroidNamedDrawableWithSize {

    override fun get(key: String): Any? = when (key) {
        "name" -> name
        else -> throw IllegalArgumentException("No key $key for ${this::class.simpleName}")
    }

    val sizes: Map<String, Size> by lazy {
        files.mapValues {
            ImageIO.read(it.value).let { Size(it.width, it.height) }
        }
    }

    override val size: Size by lazy {
        sizes[""]
            ?: sizes["mdpi"]
            ?: sizes["xhdpi"]?.div(2)
            ?: sizes["xxxhdpi"]?.div(4)
            ?: sizes["xxhdpi"]?.div(3)
            ?: sizes["hdpi"]?.times(2)?.div(3)
            ?: sizes["ldpi"]?.times(3)?.div(2)
            ?: Size(24, 24)
    }

    data class Reference(val base: Lazy<AndroidBitmap>, val tint: Lazy<AndroidColor>? = null) :
        AndroidDrawableXml {
        override fun toResource(file: File): AndroidDrawable = TODO()
        override fun get(key: String): Any? = when (key) {
            "base" -> base
            "tint" -> tint
            else -> throw IllegalArgumentException("No key $key for ${this::class.simpleName}")
        }
    }
}

data class AndroidShape(
    override val name: String,
    val file: File,
    val shape: Value
) : AndroidXmlDrawable {
    override val value: AndroidDrawableXml
        get() = shape

    override fun get(key: String): Any? = when (key) {
        "name" -> name
        else -> shape[key]
    }

    data class Value(
        val shapeType: ShapeType,
        val stroke: Lazy<AndroidColor>? = null,
        val strokeWidth: Lazy<AndroidDimension>? = null,
        val fill: Lazy<AndroidColor>? = null,
        val gradient: Gradient? = null,
        val topLeftCorner: Lazy<AndroidDimension>? = null,
        val topRightCorner: Lazy<AndroidDimension>? = null,
        val bottomLeftCorner: Lazy<AndroidDimension>? = null,
        val bottomRightCorner: Lazy<AndroidDimension>? = null
    ) : AndroidDrawableXml {
        enum class ShapeType { Rectangle, Oval }

        data class Gradient(
            val startColor: Lazy<AndroidColor>,
            val centerColor: Lazy<AndroidColor>? = null,
            val endColor: Lazy<AndroidColor>,
            val angle: Double
        ) {
            operator fun get(key: String): Any? = when (key) {
                "startColor" -> startColor
                "centerColor" -> centerColor
                "endColor" -> endColor
                "angle" -> angle
                else -> throw IllegalArgumentException("No key $key for ${this::class.simpleName}")
            }
        }

        override fun get(key: String): Any? = when (key) {
            "shapeType" -> shapeType
            "stroke" -> stroke
            "strokeWidth" -> strokeWidth
            "fill" -> fill
            "gradient" -> gradient
            "topLeftCorner" -> topLeftCorner
            "topRightCorner" -> topRightCorner
            "bottomLeftCorner" -> bottomLeftCorner
            "bottomRightCorner" -> bottomRightCorner
            else -> throw IllegalArgumentException("No key $key for ${this::class.simpleName}")
        }

        override fun toResource(file: File): AndroidDrawable = AndroidShape(file.nameWithoutExtension, file, this)
    }
}

data class AndroidDrawableState(
    override val name: String,
    val file: File,
    override val value: Value
) : AndroidXmlDrawable {

    override fun get(key: String): Any? = when (key) {
        "name" -> name
        else -> throw IllegalArgumentException("No key $key for ${this::class.simpleName}")
    }

    data class Value(
        val states: StateSelector<AndroidDrawableXml>
    ) : AndroidDrawableXml {
        override fun toResource(file: File) = AndroidDrawableState(file.nameWithoutExtension, file, this)
        override fun get(key: String): Any? = states.get(key)
    }
}

data class AndroidLayer(
    override val name: String,
    val file: File,
    override val value: Value
) : AndroidXmlDrawable, AndroidDrawableWithSize {
    override val size: Size
        get() = Size(
            width = value.states.map { it.width?.value?.measurement?.number?.toInt() ?: 16 }.maxOrNull() ?: 16,
            height = value.states.map { it.height?.value?.measurement?.number?.toInt() ?: 16 }.maxOrNull() ?: 16
        )

    override fun get(key: String): Any? = when (key) {
        "name" -> name
        else -> throw IllegalArgumentException("No key $key for ${this::class.simpleName}")
    }

    data class Value(
        val states: List<Layer>
    ) : AndroidDrawableXml {
        override fun toResource(file: File) = AndroidLayer(file.nameWithoutExtension, file, this)
        override fun get(key: String): Any? = states[key.toInt()]
    }

    data class Layer(
        val drawable: AndroidDrawableXml,
        val width: Lazy<AndroidDimension>? = null,
        val height: Lazy<AndroidDimension>? = null,
        val top: Lazy<AndroidDimension>? = null,
        val left: Lazy<AndroidDimension>? = null,
        val right: Lazy<AndroidDimension>? = null,
        val bottom: Lazy<AndroidDimension>? = null
    )
}

data class AndroidLayoutResource(
    val name: String,
    val layout: Lazy<AndroidLayoutFile>
) : AndroidValue {

    override fun get(key: String): Any? = when (key) {
        "name" -> name
        "xmlClass" -> name.camelCase().capitalize() + "Binding"
        else -> throw IllegalArgumentException("No key $key for ${this::class.simpleName}")
    }
}

data class AndroidDimensionLiteral(
    override val measurement: Measurement
) : AndroidDimension {
}

data class AndroidDimensionResource(
    val name: String,
    override val measurement: Measurement
) : AndroidDimension {

    override fun get(key: String): Any? = when (key) {
        "name" -> name
        else -> super.get(key)
    }
}

data class AndroidNumber(
    val value: Double
) : AndroidValue {

    override fun get(key: String): Any? = when (key) {
        "value" -> value.toString()
        else -> throw IllegalArgumentException("No key $key for ${this::class.simpleName}")
    }
}

data class AndroidStringLiteral(
    override val value: String
) : AndroidString {

    override fun get(key: String): Any? = when (key) {
        "value" -> value
        else -> super.get(key)
    }
}

data class AndroidStringResource(
    val name: String,
    override val value: String,
    val languages: Map<String, String>
) : AndroidString {

    override fun get(key: String): Any? = when (key) {
        "name" -> name
        "value" -> value
        else -> super.get(key)
    }
}

data class AndroidStyle(
    val name: String,
    val map: Map<String, String>,
    val parent: Lazy<AndroidStyle>? = null
) : AndroidValue {
    val chainedMap: Map<String, String> get() = DeferMap(listOfNotNull(map, parent?.value?.chainedMap))


    override fun get(key: String): Any? = when (key) {
        "name" -> name
        else -> throw IllegalArgumentException("No key $key for ${this::class.simpleName}")
    }
}

enum class MeasurementUnit {
    PX, DP, SP
}

data class Measurement(
    val number: Double,
    val unit: MeasurementUnit
) {
    operator fun get(key: String): Any? = when (key) {
        "number" -> number.toString()
        "half" -> (number / 2).toString()
        "halfInteger" -> (number / 2).toInt().toString()
        "integer" -> number.toInt().toString()
        "unit" -> unit.name.toLowerCase()
        else -> null
    }
}

data class ColorInParts(
    val red: Int = 0,
    val green: Int = 0,
    val blue: Int = 0,
    val alpha: Int = 0xFF
) : HasGet {
    val web: String
        get() = "#" + red.toString(16).padStart(2, '0') + green.toString(16).padStart(2, '0') + blue.toString(16)
            .padStart(2, '0') + alpha.toString(16).padStart(2, '0')
    val webNoAlpha: String
        get() = "#" + red.toString(16).padStart(2, '0') + green.toString(16).padStart(2, '0') + blue.toString(16)
            .padStart(2, '0')
    val android: String
        get() = "#" + alpha.toString(16).padStart(2, '0') + red.toString(16).padStart(2, '0') + green.toString(16)
            .padStart(2, '0') + blue.toString(16).padStart(2, '0')
    val redFloat: Float get() = red / 255f
    val greenFloat: Float get() = green / 255f
    val blueFloat: Float get() = blue / 255f
    val alphaFloat: Float get() = alpha / 255f

    companion object {
        val transparent = ColorInParts(alpha = 0)
        val black = ColorInParts()
        val white = ColorInParts(red = 0xFF, green = 0xFF, blue = 0xFF)
        val basicColors = mapOf(
            "transparent" to transparent,
            "black" to black,
            "white" to white,
        )
    }

    override fun get(key: String): Any?? = when (key) {
        "web" -> web
        "android" -> android
        "red" -> red.toString(16).padStart(2, '0')
        "green" -> green.toString(16).padStart(2, '0')
        "blue" -> blue.toString(16).padStart(2, '0')
        "alpha" -> alpha.toString(16).padStart(2, '0')
        "redFloat" -> redFloat.toString()
        "greenFloat" -> greenFloat.toString()
        "blueFloat" -> blueFloat.toString()
        "alphaFloat" -> alphaFloat.toString()
        else -> null
    }
}

fun String.hashColorToParts(): ColorInParts {
    return when (this.length) {
        3 + 1 -> ColorInParts(
            red = (this[1].toString().toInt(16) * 0x11),
            green = (this[2].toString().toInt(16) * 0x11),
            blue = (this[3].toString().toInt(16) * 0x11)
        )
        4 + 1 -> ColorInParts(
            red = (this[2].toString().toInt(16) * 0x11),
            green = (this[3].toString().toInt(16) * 0x11),
            blue = (this[4].toString().toInt(16) * 0x11),
            alpha = (this[1].toString().toInt(16) * 0x11),
        )
        6 + 1 -> ColorInParts(
            red = (this.substring(1, 3).toInt(16)),
            green = (this.substring(3, 5).toInt(16)),
            blue = (this.substring(5, 7).toInt(16))
        )
        8 + 1 -> ColorInParts(
            red = (this.substring(3, 5).toInt(16)),
            green = (this.substring(5, 7).toInt(16)),
            blue = (this.substring(7, 9).toInt(16)),
            alpha = (this.substring(1, 3).toInt(16)),
        )
        else -> ColorInParts()
    }
}

enum class UiState { Normal, Selected, Highlighted, Disabled, Focused }
data class StateSelector<T>(
    val normal: T,
    val selected: T? = null,
    val highlighted: T? = null,
    val disabled: T? = null,
    val focused: T? = null
) : HasGet {
    val asMap: Map<UiState, T?>
        get() = mapOf(
            UiState.Normal to normal,
            UiState.Selected to selected,
            UiState.Highlighted to highlighted,
            UiState.Disabled to disabled,
            UiState.Focused to focused,
        )
    val isSet: Boolean get() = selected != null || highlighted != null || disabled != null || focused != null
    val variants: Map<String, T>
        get() = listOf(
            "" to normal,
            "_selected" to selected,
            "_highlighted" to highlighted,
            "_disabled" to disabled,
            "_focused" to focused
        ).filter { it.second != null }.associate { it.first to it.second!! }

    override fun get(key: String): T = when (key) {
        "normal" -> normal
        "selected" -> selected ?: normal
        "highlighted" -> highlighted ?: normal
        "disabled" -> disabled ?: normal
        "focused" -> focused ?: normal
        else -> normal
    }

    operator fun get(state: UiState): T = when (state) {
        UiState.Normal -> normal
        UiState.Selected -> selected ?: normal
        UiState.Highlighted -> highlighted ?: normal
        UiState.Disabled -> disabled ?: normal
        UiState.Focused -> focused ?: normal
    }

    fun copy(state: UiState, to: T) = when (state) {
        UiState.Normal -> copy(normal = to)
        UiState.Selected -> copy(selected = to)
        UiState.Highlighted -> copy(highlighted = to)
        UiState.Disabled -> copy(disabled = to)
        UiState.Focused -> copy(focused = to)
    }
}