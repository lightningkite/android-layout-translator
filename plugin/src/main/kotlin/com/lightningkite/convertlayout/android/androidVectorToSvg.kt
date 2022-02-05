package com.lightningkite.convertlayout.android

import com.lightningkite.convertlayout.xml.*
import org.w3c.dom.Element
import java.io.File
import java.lang.Appendable

fun AndroidVector.toSvg(resources: AndroidResources, scale: Float = 1f, out: File = File.createTempFile(name, ".svg")): File {
    out.printWriter().use {
        androidVectorToSvg(resources, this.file.readXml().documentElement, scale).emit(it)
    }
    return out
}

private data class SvgColor(val color: String, val alpha: String)

private class SvgEmitter(
    val width: Double,
    val height: Double,
    val viewportWidth: Double,
    val viewportHeight: Double,
    val resolveColor: (String)->SvgColor,
) {
    val defs = StringBuilder()
    val main = StringBuilder()
    fun emit(appendable: Appendable) {
        appendable.appendLine("<svg xmlns='http://www.w3.org/2000/svg' width='${width}' height='${height}' viewBox='0 0 $viewportWidth $viewportHeight'>")
        if(defs.isNotBlank()) {
            appendable.appendLine("<defs>")
            appendable.appendLine(defs)
            appendable.appendLine("</defs>")
        }
        appendable.appendLine(main)
        appendable.appendLine("</svg>")
    }
}

private fun androidVectorToSvg(
    resource: AndroidResources,
    node: Element,
    scale: Float
): SvgEmitter {
    val viewportWidth = node["android:viewportWidth"]?.toDoubleOrNull() ?: 24.0
    val viewportHeight = node["android:viewportHeight"]?.toDoubleOrNull() ?: 24.0
    val width = (node["android:width"]?.takeWhile { it.isDigit() }?.toDoubleOrNull() ?: viewportWidth) * scale
    val height = (node["android:height"]?.takeWhile { it.isDigit() }?.toDoubleOrNull() ?: viewportHeight) * scale
    val emitter = SvgEmitter(width, height, viewportWidth, viewportHeight) {
        val v = resource.read(it).let { it as? AndroidColor}?.value ?: return@SvgEmitter SvgColor("black", "0")
        SvgColor(v.webNoAlpha, v.alphaFloat.toString())
    }
    node.childElements.forEachIndexed { index, value ->
        value.writeElement(emitter, "$index")
    }
    return emitter
}

private fun Element.writeElement(svgOut: SvgEmitter, path: String): Unit {
    when(tagName) {
        "group" -> {
            svgOut.main.appendLine("<g>")
            for(sub in this.childElements.withIndex()) {
                sub.value.writeElement(svgOut, path + "-${sub.index}")
            }
            svgOut.main.appendLine("</g>")
        }
        "path" -> {
            svgOut.main.appendLine("<path d='${this["android:pathData"]}' ")
            val gradientChild = this.childElements.find { it.tagName == "aapt:attr" }?.childElements?.find { it.tagName == "gradient" }

            gradientChild?.also { gradientNode ->
                // Define the gradient
                val x1 = gradientNode["android:startX"]?.toDoubleOrNull()?.div(svgOut.viewportWidth)
                val y1 = gradientNode["android:startY"]?.toDoubleOrNull()?.div(svgOut.viewportHeight)
                val x2 = gradientNode["android:endX"]?.toDoubleOrNull()?.div(svgOut.viewportWidth)
                val y2 = gradientNode["android:endY"]?.toDoubleOrNull()?.div(svgOut.viewportHeight)
                svgOut.defs.appendLine("<linearGradient id='grad${path}' x1='$x1' y1='$y1' x2='$x2' y2='$y2'>")
                gradientNode.childElements.filter { it.tagName == "item" }.forEach {
                    val color = it["android:color"]?.let(svgOut.resolveColor) ?: return@forEach
                    val offset = (it["android:offset"]?.toDoubleOrNull() ?: 0.0) * 100
                    svgOut.defs.appendLine("<stop offset='$offset%' style='stop-color: ${color.color}; stop-opacity: ${color.alpha}'/>")
                }
                gradientNode["android:startColor"]?.let(svgOut.resolveColor)?.let {
                    svgOut.defs.appendLine("<stop offset='0%' style='stop-color: ${it.color}; stop-opacity: ${it.alpha}'/>")
                }
                gradientNode["android:endColor"]?.let(svgOut.resolveColor)?.let {
                    svgOut.defs.appendLine("<stop offset='0%' style='stop-color: ${it.color}; stop-opacity: ${it.alpha}'/>")
                }
                svgOut.defs.appendLine("</linearGradient>")

                // Use the gradient
                svgOut.main.appendLine("fill='url(#grad${path})'")
            } ?: this["android:fillColor"]?.let(svgOut.resolveColor)?.let {
                svgOut.main.appendLine("fill='${it.color}' fill-opacity='${it.alpha}'")
            } ?: run {
                svgOut.main.appendLine("fill='none'")
            }
            this["android:strokeColor"]?.let(svgOut.resolveColor)?.let {
                svgOut.main.appendLine("stroke='${it.color}' stroke-opacity='${it.alpha}'")
            }
            this["android:strokeWidth"]?.let {
                svgOut.main.appendLine("stroke-width='$it'")
            }
            svgOut.main.appendLine("/>")
        }
        else -> {}
    }
}