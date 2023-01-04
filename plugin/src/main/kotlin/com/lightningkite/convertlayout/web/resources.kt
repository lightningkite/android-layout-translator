package com.lightningkite.convertlayout.web

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.lightningkite.convertlayout.android.*
import com.lightningkite.convertlayout.rules.AttributeReplacement
import com.lightningkite.convertlayout.util.*
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.sourceElementsConfigurationName

fun WebTranslator.importResources() {
    val sass = StringBuilder()
    sass.appendLine("""@import "~@lightningkite/android-xml-runtime/index.scss";""")
    importFonts(sass)
    importDimensionsColors(sass)
    importDrawables(sass)
    importStyles(sass)
    importStrings()
    project.resourcesFolder.resolve("resources.scss").writeText(sass.toString())
}

fun WebTranslator.importFonts(sass: StringBuilder) {
    for((key, font) in this.resources.fonts) {
        when(font) {
            is AndroidFontLiteral -> {
                val destFile = project.fontFolder.resolve(font.file.name)
                font.file.copyTo(destFile, overwrite = true)
                sass.appendLine("""
                    @font-face {
                      font-family: ${font.fontFamily};
                      src: url(./fonts/${font.file.name});
                    }
                """.trimIndent())
            }
            is AndroidFontSet -> {

            }
        }
    }
}

fun WebTranslator.importDrawables(sass: StringBuilder) {
    for (it in resources.drawables.values) {
        importDrawable(it, sass)
    }
}

fun WebTranslator.importDrawable(drawableResource: AndroidDrawable, sass: StringBuilder) {
    try {
        when (drawableResource) {
            is AndroidBitmap -> importBitmap(drawableResource, sass)
            is AndroidXmlDrawable -> {
                sass.appendLine(".drawable-${drawableResource.name} {")
                sass.writeSass(drawableResource.value)
                sass.appendLine("}")
                sass.appendLine(".compound-${drawableResource.name}::after { @extend .drawable-${drawableResource.name}; }")
            }
            is AndroidVector -> importVector(drawableResource, sass)
            else -> {}
        }
    } catch (e: Exception) {
        throw Exception("Failed to read ${drawableResource}", e)
    }
}

fun Appendable.writeSass(xml: AndroidDrawableXml) {
    when (xml) {
        is AndroidDrawable.Reference -> when (val sub = xml.drawable.value) {
            is AndroidNamedDrawable -> appendLine("@extend .drawable-${sub.name};")
            is AndroidColor -> appendLine("background-color: ${sub.value.web};")
        }
        is AndroidBitmap.Reference -> {
            appendLine("@extend .drawable-${xml.base.value.name};")
            appendLine("width: ${xml.size.width};")
            appendLine("height: ${xml.size.height};")
        }
        is AndroidShape.Value -> {
            xml.gradient?.let { gradient ->
                listOfNotNull(gradient.startColor.value, gradient.centerColor?.value, gradient.endColor.value)
                    .map { it.sass }
                    .joinToString()
                    .let { appendLine("background-image: linear-gradient(${90 - gradient.angle}deg, $it);") }
            } ?: xml.fill?.value?.let { appendLine("background-color: ${it.sass};") }
            xml.stroke?.value?.let { color ->
                appendLine("border-color: ${color.sass};")
            }
            xml.strokeWidth?.value?.measurement?.web?.let {
                appendLine("border-width: $it;")
                appendLine("border-style: solid;")
            }
            when (xml.shapeType) {
                AndroidShape.Value.ShapeType.Rectangle -> {
                    xml.topLeftCorner?.value?.measurement?.web?.let {
                        appendLine("border-top-left-radius: $it;")
                    }
                    xml.topRightCorner?.value?.measurement?.web?.let {
                        appendLine("border-top-right-radius: $it;")
                    }
                    xml.bottomLeftCorner?.value?.measurement?.web?.let {
                        appendLine("border-bottom-left-radius: $it;")
                    }
                    xml.bottomRightCorner?.value?.measurement?.web?.let {
                        appendLine("border-bottom-right-radius: $it;")
                    }
                }
                AndroidShape.Value.ShapeType.Oval -> {
                    appendLine("border-radius: 50%;")
                }
            }
        }
        is AndroidDrawableState.Value -> {
            writeSass(xml.states.normal)
            xml.states.focused?.let {
                appendLine("&:focus, :focus>& {")
                writeSass(it)
                appendLine("}")
            }
            xml.states.selected?.let {
                appendLine("&.checked, .checked>& {")
                writeSass(it)
                appendLine("}")
            }
            xml.states.highlighted?.let {
                appendLine("&:active:hover {")
                writeSass(it)
                appendLine("}")
            }
            xml.states.disabled?.let {
                appendLine("&:disabled, :disabled>& {")
                writeSass(it)
                appendLine("}")
            }
        }
        is AndroidLayer.Value -> {
            appendLine("// WARNING: You can't do that - it would require multiple views inside of this one.")
            for (layer in xml.states) {
                writeSass(layer.drawable)
            }
        }
    }
}

fun WebTranslator.importVector(drawableResource: AndroidVector, sass: StringBuilder) {
    val svgFile = project.drawablesFolder.resolve(drawableResource.name + ".svg")
    drawableResource.toSvg(resources, 1f, svgFile)
    sass.appendLine(".drawable-${drawableResource.name} {")
    sass.appendLine("@extend .imageDrawable;")
    sass.appendLine("""background-image: url("drawables/${svgFile.name}");""")
    sass.appendLine("--drawable-width: ${drawableResource.size.width}px;")
    sass.appendLine("--drawable-height: ${drawableResource.size.height}px;")
    sass.appendLine("}")
    sass.appendLine(".compound-${drawableResource.name}::after { @extend .drawable-${drawableResource.name}; }")
}

fun WebTranslator.importBitmap(drawableResource: AndroidBitmap, sass: StringBuilder) {
    val sourceFile = drawableResource.files["ldpi"]
        ?: drawableResource.files["mdpi"]
        ?: drawableResource.files["hdpi"]
        ?: drawableResource.files["xhdpi"]
        ?: drawableResource.files["xxhdpi"]
        ?: drawableResource.files["xxxhdpi"]!!
    val destFile = project.drawablesFolder.resolve(drawableResource.name + "." + sourceFile.extension)
    sourceFile.copyTo(destFile, overwrite = true)
    sass.appendLine(".drawable-${drawableResource.name} {")
    sass.appendLine("@extend .imageDrawable;")
    sass.appendLine("""background-image: url("./drawables/${destFile.name}");""")
    sass.appendLine("--drawable-width: ${drawableResource.size.width}px;")
    sass.appendLine("--drawable-height: ${drawableResource.size.height}px;")
    sass.appendLine("}")
    sass.appendLine(".compound-${drawableResource.name}::after { @extend .drawable-${drawableResource.name}; }")
}

fun WebTranslator.importStrings() {
    project.resourcesFolder.resolve("R.ts").writeText(buildSmartTabbedString {
        appendLine("/*")
        appendLine("R.ts")
        appendLine("Created by Khrysalis")
        appendLine("*/")
        appendLine("//! Declares ${resources.packageName}.R")
        appendLine("export interface StringInterface {")
        for (key in resources.strings.keys) {
            appendLine("${key.safeJsIdentifier()}: string")
        }
        appendLine("}")
        appendLine("export namespace DefaultStrings {")
        for ((key, value) in resources.strings.entries) {
            val fixedString = value.value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\t", "\\t")
            appendLine("export const ${key.safeJsIdentifier()} = \"$fixedString\"")
        }
        appendLine("}")
        appendLine("export const Strings: StringInterface = Object.assign({}, DefaultStrings);")
        appendLine("export namespace Colors {")
        for ((key, value) in resources.colors.entries) {
            appendLine(
                "export const ${key.safeJsIdentifier()} = \"${
                    when (value) {
                        is AndroidColorStateResource -> "--color-" + value.name
                        else -> value.value.web
                    }
                }\""
            )
        }
        appendLine("}")
        appendLine("export namespace Dimen {")
        for ((key, value) in resources.dimensions.entries) {
            appendLine("export const ${key.safeJsIdentifier()} = \"${value.name}\"")
        }
        appendLine("}")
        for ((key, value) in resources.drawables.entries) {
            when (value) {
                is AndroidBitmap -> {
                    val sourceFile = value.files["ldpi"]
                        ?: value.files["mdpi"]
                        ?: value.files["hdpi"]
                        ?: value.files["xhdpi"]
                        ?: value.files["xxhdpi"]
                        ?: value.files["xxxhdpi"]!!
                    appendLine("import drawable_${key.safeJsIdentifier()} from \"./drawables/${sourceFile.name}\"")
                }
                is AndroidVector -> appendLine("import drawable_${key.safeJsIdentifier()} from \"./drawables/${value.file.nameWithoutExtension}.svg\"")
                else -> {}
            }
        }
        appendLine("export namespace Drawables {")
        for ((key, value) in resources.drawables.entries) {
            when (value) {
                is AndroidBitmap, is AndroidVector -> appendLine("export const ${key} = {name: \"${key}\", file: drawable_${key}}")
                else -> appendLine("export const ${key} = {name: \"${key}\"}")
            }
        }
        appendLine("}")
    })
}

fun WebTranslator.importDimensionsColors(sass: StringBuilder) {
    resources.dimensions.values.forEach {
        sass.appendLine("$${it.name}: ${it.measurement.web};")
    }
    resources.colors.values.mapNotNull { it as? AndroidColorResource }.forEach {
        sass.appendLine("$${it.name}: ${it.value.web};")
    }
    sass.appendLine("* {")
    resources.colors.values.mapNotNull { it as? AndroidColorStateResource }.forEach {
        sass.appendLine("--color-${it.name}: ${it.colors.normal.value.sass};")
    }
    sass.appendLine("}")
    sass.appendLine(".checked, .checked>* {")
    resources.colors.values.mapNotNull { it as? AndroidColorStateResource }.forEach {
        val color = it.colors.selected?.value ?: return@forEach
        sass.appendLine("--color-${it.name}: ${color.sass};")
    }
    sass.appendLine("}")
    sass.appendLine(":disabled ~ * {")
    resources.colors.values.mapNotNull { it as? AndroidColorStateResource }.forEach {
        val color = it.colors.disabled?.value ?: return@forEach
        sass.appendLine("--color-${it.name}: ${color.sass};")
    }
    sass.appendLine("}")
    sass.appendLine(":active:hover ~ * {")
    resources.colors.values.mapNotNull { it as? AndroidColorStateResource }.forEach {
        val color = it.colors.highlighted?.value ?: return@forEach
        sass.appendLine("--color-${it.name}: ${color.sass};")
    }
    sass.appendLine("}")
    sass.appendLine(":focus ~ * {")
    resources.colors.values.mapNotNull { it as? AndroidColorStateResource }.forEach {
        val color = it.colors.focused?.value ?: return@forEach
        sass.appendLine("--color-${it.name}: ${color.sass};")
    }
    sass.appendLine("}")
}

fun WebTranslator.importStyles(sass: StringBuilder) {
    for ((name, style) in resources.styles) {
        sass.appendLine(".style-${name} {")
        style.parent?.value?.name?.takeUnless { it.isBlank() }?.let {
            sass.appendLine("@extend .style-$it;")
        }
        for ((key, rawValue) in style.map) {
            val value = resources.read(rawValue)
            if (!replacements.canBeInStyleSheet(key)) continue
            replacements.getAttributeOptionsForStyle(key, AttributeReplacement.ValueType2[value::class], rawValue)
                .forEach { rule ->
                    // Potential upgrade: Support element and parentElement?
                    var preAmpersand = ""
                    var postAmpersand = ""
                    rule.element.takeUnless { it == "View" }?.let { element ->
                        val uniqueElementType = replacements.elements[element]?.singleOrNull()?.uniqueElementType
                        if (uniqueElementType != null) postAmpersand += "$uniqueElementType"
                        else postAmpersand += ".android-${element}"
                    } ?: rule.parentElement?.let { parentElement ->
                        val uniqueElementType = replacements.elements[parentElement]?.singleOrNull()?.uniqueElementType
                        if (uniqueElementType != null) preAmpersand = "$uniqueElementType $preAmpersand"
                        else preAmpersand = ".android-$parentElement $preAmpersand"
                    }
                    val sub =
                        if (preAmpersand.isNotEmpty() || postAmpersand.isNotEmpty()) "$preAmpersand&$postAmpersand" else null
                    if (sub != null) sass.appendLine("$sub {")
                    for ((path, subrule) in rule.rules) {
                        if (!path.isEmpty()) {
                            sass.appendLine(path.replace('/', ' ') + " {")
                        }
                        subrule(
                            value = value,
                            getter = { with(resources) { style.chainedMap.getPath(it) } },
                            action = { subrule ->
                                for ((ckey, ctemplate) in subrule.css) {
                                    val cvalue = ctemplate?.write { with(resources) { value.getPath(it) } }
                                    sass.appendLine("$ckey: $cvalue;")
                                }
                                subrule.classes
                                    .takeUnless { it.isEmpty() }
                                    ?.map { "." + it.write { with(resources) { value.getPath(it) } } }
                                    ?.joinToString()
                                    ?.let { sass.appendLine("@extend $it;") }
                            }
                        )
                        if (!path.isEmpty()) {
                            sass.appendLine("}")
                        }
                    }
                    if (sub != null) sass.appendLine("}")
                }
        }
        sass.appendLine("}")
    }
}

val AndroidColor.sass: String
    get() = when (this) {
        is AndroidColorStateResource -> "var(--color-$name)"
        is AndroidColorResource -> "#{$${name}}"
        is AndroidColorLiteral -> value.web
    }

val AndroidDimension.sass: String
    get() = when (this) {
        is AndroidDimensionResource -> "#{$${name}}"
        else -> measurement.web
    }
