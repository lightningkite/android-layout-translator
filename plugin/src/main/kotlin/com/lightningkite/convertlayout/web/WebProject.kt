package com.lightningkite.convertlayout.web

import java.io.File

data class WebProject(
    val folder: File,
    val name: String,
) {
    val resourcesFolder: File get() = folder.resolve("src/resources")
    val fontFolder: File get() = resourcesFolder.resolve("fonts")
    val layoutsFolder: File get() = resourcesFolder.resolve("layouts")
    val drawablesFolder: File get() = resourcesFolder.resolve("drawables")
    init {
        folder.mkdirs()
        resourcesFolder.mkdirs()
        layoutsFolder.mkdirs()
        drawablesFolder.mkdirs()
    }
}



