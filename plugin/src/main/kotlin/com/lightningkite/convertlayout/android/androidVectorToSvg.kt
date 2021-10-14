package com.lightningkite.convertlayout.android

import com.lightningkite.convertlayout.xml.*
import org.w3c.dom.Element
import java.io.File

fun AndroidVector.toSvg(resources: AndroidResources, scale: Float = 1f, out: File = File.createTempFile(name, ".svg")): File {
    out.printWriter().use {
        androidVectorToSvg(resources, this.file.readXml().documentElement, scale, it)
    }
    return out
}

private fun androidVectorToSvg(
    resource: AndroidResources,
    node: Element,
    scale: Float,
    svgOut: Appendable
) {
    val colorResolver = { it: String -> resource.read(it).let { it as? AndroidColor}?.value?.webNoAlpha ?: "black" }
    val viewportWidth = node["android:viewportWidth"]?.toDoubleOrNull() ?: 24.0
    val viewportHeight = node["android:viewportHeight"]?.toDoubleOrNull() ?: 24.0
    val width = (node["android:width"]?.toDoubleOrNull() ?: viewportWidth) * scale
    val height = (node["android:height"]?.toDoubleOrNull() ?: viewportHeight) * scale
    svgOut.appendLine("<svg xmlns='http://www.w3.org/2000/svg' width='${width}' height='${height}' viewBox='0 0 $viewportWidth $viewportHeight'>")
    node.childElements
        .filter { it.tagName == "path" }
        .withIndex()
        .mapNotNull { (index, subnode) ->
            subnode.childElements
                .find { it.tagName == "aapt:attr" && it["name"] == "android:fillColor" }
                ?.children?.mapNotNull { it as? Element }?.find { it.tagName == "gradient" }?.let { index to it }
        }
        .toList()
        .takeUnless { it.isEmpty() }
        ?.let {
            svgOut.appendLine("<defs>")
            it.forEach { (index, gradientNode) ->
                val x1 = gradientNode["android:startX"]?.toDoubleOrNull()
                val y1 = gradientNode["android:startY"]?.toDoubleOrNull()
                val x2 = gradientNode["android:endX"]?.toDoubleOrNull()
                val y2 = gradientNode["android:endY"]?.toDoubleOrNull()
                svgOut.appendLine("<linearGradient id='grad$index' x1='$x1' y1='$y1' x2='$x2' y2='$y2'>")
                gradientNode.childElements.filter { it.tagName == "item" }.forEach {
                    val color = it["android:color"]?.let(colorResolver)
                    val offset = (it["android:offset"]?.toDoubleOrNull() ?: 0.0) * 100
                    svgOut.appendLine("<stop offset='$offset%' style='stop-color: $color;'/>")
                }
                gradientNode["android:startColor"]?.let(colorResolver)?.let {
                    svgOut.appendLine("<stop offset='0%' style='stop-color: $it;'/>")
                }
                gradientNode["android:endColor"]?.let(colorResolver)?.let {
                    svgOut.appendLine("<stop offset='0%' style='stop-color: $it;'/>")
                }
                svgOut.appendLine("</linearGradient>")
            }
            svgOut.appendLine("</defs>")
        }
    node.childElements.filter { it.tagName == "path" }.forEachIndexed { index, subnode ->
        subnode.childElements
            .find { it.tagName == "aapt:attr" && it["name"] == "android:fillColor" }
            ?.childElements?.find { it.tagName == "gradient" }
            ?.let { gradientNode ->
                svgOut.appendLine("<path d='${subnode["android:pathData"]}' fill='url(#grad${index})'/>")
            } ?: run {

            svgOut.appendLine("<path d='${subnode["android:pathData"]}' ")
            subnode["android:fillColor"]?.let(colorResolver)?.let {
                svgOut.appendLine("fill='$it'")
            } ?: run {
                svgOut.appendLine("fill='none'")
            }
            subnode["android:strokeColor"]?.let(colorResolver)?.let {
                svgOut.appendLine("stroke='$it'")
            }
            subnode["android:strokeWidth"]?.let {
                svgOut.appendLine("stroke-width='$it'")
            }
            svgOut.appendLine("/>")
        }
    }
    svgOut.appendLine("</svg>")
}