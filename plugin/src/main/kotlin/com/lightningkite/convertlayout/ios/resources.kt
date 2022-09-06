package com.lightningkite.convertlayout.ios

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lightningkite.convertlayout.android.*
import com.lightningkite.convertlayout.util.*

fun IosTranslator.importResources() {
    importDrawables()
    importStringsDimensionsColors()
    importColorAssets()
}

fun IosTranslator.importDrawables() {
    for (it in resources.drawables.values) {
        importDrawable(it)
    }
}

fun IosTranslator.importDrawable(drawableResource: AndroidDrawable) {
    try {
        when (drawableResource) {
            is AndroidBitmap -> importBitmap(drawableResource)
            is AndroidXmlDrawable -> importDrawableXml(drawableResource)
            is AndroidVector -> importVector(drawableResource)
            else -> {}
        }
    } catch (e: Exception) {
        throw Exception("Failed to read ${drawableResource}", e)
    }
}

fun IosTranslator.importDrawableXml(drawableResource: AndroidXmlDrawable) {
    project.swiftResourcesFolder
        .resolve("drawables")
        .also { it.mkdirs() }
        .resolve(drawableResource.name + ".swift")
        .writeText(buildSmartTabbedString {
            appendLine("import XmlToXibRuntime")
            appendLine("public extension R.drawable {")
            appendLine("static func ${drawableResource.name}() -> CALayer {")
            writeAndroidXml(drawableResource.value)
            appendLine("}")
            appendLine("}")
        })
}

val AndroidDrawableXml.scaleOverResize: Boolean
    get() = when (this) {
        is AndroidDrawable.Reference -> when (this.drawable.value) {
            is AndroidBitmap -> true
            is AndroidColorLiteral -> false
            is AndroidColorResource -> false
            is AndroidColorStateResource -> false
            is AndroidDrawableState -> false
            is AndroidLayer -> false
            is AndroidShape -> false
            is AndroidVector -> true
            is AndroidNamedColor -> false
        }
        is AndroidBitmap.Reference -> true
        is AndroidShape.Value -> false
        is AndroidDrawableState.Value -> false
        is AndroidLayer.Value -> false
    }

fun Appendable.writeAndroidXml(xml: AndroidDrawableXml) {
    when (xml) {
        is AndroidDrawable.Reference -> when(val sub = xml.drawable.value){
            is AndroidNamedDrawable -> appendLine("R.drawable.${sub.name}()")
            is AndroidColorResource -> writeAndroidXml(AndroidShape.Value(AndroidShape.Value.ShapeType.Rectangle, fill = Lazy(sub)))
            is AndroidColorStateResource -> writeAndroidXml(AndroidShape.Value(AndroidShape.Value.ShapeType.Rectangle, fill = Lazy(sub)))
            else -> {}
        }
        is AndroidBitmap.Reference -> appendLine("R.drawable.${xml.base.value.name}()")
        is AndroidShape.Value -> {
            xml.gradient?.let { gradient ->
                when (xml.shapeType) {
                    AndroidShape.Value.ShapeType.Rectangle -> {
                        appendLine("LayerMaker.rectGradient(")
                        appendLine("startColor: ${gradient.startColor.value.swift}, ")
                        appendLine("midColor: ${gradient.centerColor?.value?.swift ?: "nil"}, ")
                        appendLine("endColor: ${gradient.endColor.value.swift}, ")
                        appendLine("gradientAngle: ${gradient.angle}, ")
                        appendLine("strokeColor: ${xml.stroke?.value?.swift ?: ".clear"}, ")
                        appendLine("strokeWidth: ${xml.strokeWidth?.value?.swift ?: "0"}, ")
                        appendLine("topLeftRadius: ${xml.topLeftCorner?.value?.swift ?: "0"}, ")
                        appendLine("topRightRadius: ${xml.topRightCorner?.value?.swift ?: "0"}, ")
                        appendLine("bottomLeftRadius: ${xml.bottomLeftCorner?.value?.swift ?: "0"}, ")
                        appendLine("bottomRightRadius: ${xml.bottomRightCorner?.value?.swift ?: "0"}")
                        appendLine(")")
                    }
                    AndroidShape.Value.ShapeType.Oval -> {
                        appendLine("LayerMaker.ovalGradient(")
                        appendLine("startColor: ${gradient.startColor.value.swift}, ")
                        appendLine("midColor: ${gradient.centerColor?.value?.swift ?: "nil"}, ")
                        appendLine("endColor: ${gradient.endColor.value.swift}, ")
                        appendLine("gradientAngle: ${gradient.angle}, ")
                        appendLine("strokeColor: ${xml.stroke?.value?.swift ?: ".clear"}, ")
                        appendLine("strokeWidth: ${xml.strokeWidth?.value?.swift ?: "0"}")
                        appendLine(")")
                    }
                }
            } ?: run {
                when (xml.shapeType) {
                    AndroidShape.Value.ShapeType.Rectangle -> {
                        appendLine("LayerMaker.rect(")
                        appendLine("fillColor: ${xml.fill?.value?.swift ?: ".clear"}, ")
                        appendLine("strokeColor: ${xml.stroke?.value?.swift ?: ".clear"}, ")
                        appendLine("strokeWidth: ${xml.strokeWidth?.value?.swift ?: "0"}, ")
                        appendLine("topLeftRadius: ${xml.topLeftCorner?.value?.swift ?: "0"}, ")
                        appendLine("topRightRadius: ${xml.topRightCorner?.value?.swift ?: "0"}, ")
                        appendLine("bottomLeftRadius: ${xml.bottomLeftCorner?.value?.swift ?: "0"}, ")
                        appendLine("bottomRightRadius: ${xml.bottomRightCorner?.value?.swift ?: "0"}")
                        appendLine(")")
                    }
                    AndroidShape.Value.ShapeType.Oval -> {
                        appendLine("LayerMaker.oval(")
                        appendLine("fillColor: ${xml.fill?.value?.swift ?: ".clear"}, ")
                        appendLine("strokeColor: ${xml.stroke?.value?.swift ?: ".clear"}, ")
                        appendLine("strokeWidth: ${xml.strokeWidth?.value?.swift ?: "0"}")
                        appendLine(")")
                    }
                }
            }
        }
        is AndroidDrawableState.Value -> {
            appendLine("LayerMaker.state(.init(")

            append("normal: ")
            writeAndroidXml(xml.states.normal)
            appendLine(",")

            append("selected: ")
            xml.states.selected?.let { writeAndroidXml(it) } ?: append("nil")
            appendLine(",")

            append("highlighted: ")
            xml.states.highlighted?.let { writeAndroidXml(it) } ?: append("nil")
            appendLine(",")

            append("disabled: ")
            xml.states.disabled?.let { writeAndroidXml(it) } ?: append("nil")
            appendLine(",")

            append("focused: ")
            xml.states.focused?.let { writeAndroidXml(it) } ?: append("nil")
            appendLine("))")
        }
        is AndroidLayer.Value -> {
            appendLine("LayerMaker.autosize(")
            xml.states.forEachBetween(
                forItem = { sub ->
                    append(".init(layer: ")
                    writeAndroidXml(sub.drawable)
                    append(", insets: .init(top: ")
                    append(sub.top?.value?.swift ?: "0")
                    append(", left: ")
                    append(sub.left?.value?.swift ?: "0")
                    append(", bottom: ")
                    append(sub.bottom?.value?.swift ?: "0")
                    append(", right: ")
                    append(sub.right?.value?.swift ?: "0")
                    append("), scaleOverResize: ")
                    append(sub.drawable.scaleOverResize.toString())
                    append(")")
                },
                between = {
                    appendLine(", ")
                }
            )
            appendLine(")")
        }
    }
}

fun IosTranslator.importVector(drawableResource: AndroidVector) {
    val iosFolder = project.assetsFolder.resolve(drawableResource.name + ".imageset").apply { mkdirs() }

    val one =
        drawableResource.toSvg(resources, 1f).convertSvgToPng(iosFolder.resolve("${drawableResource.name}-one.png"))
    val two =
        drawableResource.toSvg(resources, 2f).convertSvgToPng(iosFolder.resolve("${drawableResource.name}-two.png"))
    val three =
        drawableResource.toSvg(resources, 3f).convertSvgToPng(iosFolder.resolve("${drawableResource.name}-three.png"))

    jacksonObjectMapper().writeValue(
        iosFolder.resolve("Contents.json"),
        PngJsonContents(images = listOf(one, two, three).mapIndexed { index, file ->
            PngJsonContents.Image(filename = file.name, scale = "${index + 1}x")
        })
    )
}

fun IosTranslator.importBitmap(drawableResource: AndroidBitmap) {
    val one = drawableResource.files["ldpi"] ?: drawableResource.files["mdpi"]
    val two = drawableResource.files["hdpi"] ?: drawableResource.files["xhdpi"] ?: drawableResource.files[""]
    val three = drawableResource.files["xxhdpi"] ?: drawableResource.files["xxxhdpi"]

    if (one == null && two == null && three == null) throw IllegalArgumentException("No PNGs found!")

    val iosFolder = project.assetsFolder.resolve(drawableResource.name + ".imageset").apply { mkdirs() }
    jacksonObjectMapper().writeValue(
        iosFolder.resolve("Contents.json"),
        PngJsonContents(images = listOf(one, two, three).mapIndexed { index, file ->
            if (file == null) return@mapIndexed null
            PngJsonContents.Image(filename = file.name, scale = "${index + 1}x")
        }.filterNotNull())
    )
    listOf(one, two, three).filterNotNull().forEach {
        if (it.checksum() != iosFolder.resolve(it.name).checksum()) {
            it.copyTo(iosFolder.resolve(it.name), overwrite = true)
        }
    }
}

private data class PngJsonContents(
    val info: Info = Info(),
    val images: List<Image> = listOf()
) {
    data class Info(val version: Int = 1, val author: String = "xcode")
    data class Image(val filename: String, val scale: String = "1x", val idiom: String = "universal")
}

fun IosTranslator.importStringsDimensionsColors() {
    val locales = resources.strings.values.flatMap { it.languages.keys }.filter { it.isNotEmpty() }.toSet()
    locales.forEach { locale ->
        project.baseFolderForLocalizations
            .resolve("${locale}.lproj")
            .resolve("Localizable.strings")
            .apply { parentFile.mkdirs() }
            .printWriter().use {
                with(it) {
                    appendLine("/*")
                    appendLine("Translation to locale $locale")
                    appendLine("Automatically written by Khrysalis")
                    appendLine("*/")
                    appendLine("")
                    for (entry in resources.strings.values) {
                        val baseString = entry.value
                            .replace("\\'", "'")
                            .replace("\\$", "$")
                            .replace(Regex("\n *"), " ")
                        val fixedString = (entry.languages[locale] ?: continue)
                            .replace("\\'", "'")
                            .replace("\\$", "$")
                            .replace(Regex("\n *"), " ")
                        appendLine("\"$baseString\" = \"$fixedString\";")
                    }
                }
            }
    }
    project.swiftResourcesFolder.mkdirs()
    project.swiftResourcesFolder.resolve("R.swift").writeText(buildSmartTabbedString {
        appendLine("//")
        appendLine("// R.swift")
        appendLine("// Created by Khrysalis")
        appendLine("//")
        appendLine("")
        appendLine("import Foundation")
        appendLine("import UIKit")
        appendLine("import XmlToXibRuntime")
        appendLine("")
        appendLine("")
        appendLine("public enum R {")

        appendLine("public enum drawable {")
        for (entry in resources.drawables) {
            when (val value = entry.value) {
                is AndroidBitmap, is AndroidVector ->
                    appendLine("public static func ${entry.key.safeSwiftIdentifier()}() -> CALayer { return CAImageLayer(UIImage(named: \"${entry.key}.png\")) }")
                else -> {}
            }
        }
        appendLine("public static let allEntries: Dictionary<String, ()->CALayer> = [")
        var firstDrawable = true
        resources.drawables.keys.forEachBetween(
            forItem = { entry ->
                append("\"$entry\": ${entry.safeSwiftIdentifier()}")
            },
            between = { appendLine(",") }
        )
        appendLine()
        appendLine("]")
        appendLine("}")

        appendLine("public enum string {")
        for ((key, value) in resources.strings.entries) {
            val fixedString = value.value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
            appendLine("public static let ${key.safeSwiftIdentifier()} = NSLocalizedString(\"$fixedString\", comment: \"$key\")")
        }
        appendLine("}")

        appendLine("public enum dimen {")
        for ((key, value) in resources.dimensions.entries) {
            if (key.contains("programmatic", true)) {
                appendLine("public static var ${key.safeSwiftIdentifier()}: CGFloat = ${value.measurement.number}")
            } else {
                appendLine("public static let ${key.safeSwiftIdentifier()}: CGFloat = ${value.measurement.number}")
            }
        }
        appendLine("}")

        appendLine("public enum color {")

        for (entry in resources.colors.entries) {
            when (val value = entry.value) {
                is AndroidColorResource -> appendLine("public static let ${entry.key}: UIColor = UIColor(named: \"color_${entry.key}\")!")
                is AndroidColorStateResource -> {
                    appendLine("public static let ${entry.key}: UIColor = UIColor(named: \"color_${entry.key}\")!")
                    appendLine("public static let ${entry.key}State: StateSelector<UIColor> = StateSelector(normal: ${value.colors.normal.value.swift}, selected: ${value.colors.selected?.value?.swift ?: "nil"}, highlighted: ${value.colors.highlighted?.value?.swift ?: "nil"}, disabled: ${value.colors.disabled?.value?.swift ?: "nil"}, focused: ${value.colors.focused?.value?.swift ?: "nil"})")
                }
                else -> {}
            }
        }
        appendLine("}")

        appendLine("}")
    })
}

fun IosTranslator.importColorAssets() {
    project.assetsFolder.mkdirs()
    val mapper = jacksonObjectMapper()
    for ((k, v) in resources.colors) {
        project.assetsFolder.resolve("color_$k.colorset").apply { mkdirs() }.resolve("Contents.json").writeText(
            mapper.writeValueAsString(
                mapOf<String, Any?>(
                    "colors" to listOf(
                        mapOf(
                            "color" to mapOf(
                                "color-space" to "srgb",
                                "components" to mapOf(
                                    "alpha" to v.value.alphaFloat.toString(),
                                    "red" to "0x" + v.value.red.toString(16).toUpperCase()
                                        .padStart(
                                            2,
                                            '0'
                                        ),
                                    "green" to "0x" + v.value.green.toString(16).toUpperCase()
                                        .padStart(
                                            2,
                                            '0'
                                        ),
                                    "blue" to "0x" + v.value.blue.toString(16).toUpperCase()
                                        .padStart(
                                            2,
                                            '0'
                                        )
                                )
                            ),
                            "idiom" to "universal"
                        )
                    ),
                    "info" to mapOf(
                        "author" to "xcode",
                        "version" to 1
                    )
                )
            )
        )
    }
}
