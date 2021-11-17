package com.lightningkite.convertlayout.ios

import java.io.File

data class IosProject(
    val folder: File,
    val name: String,
    val moduleName: String,
) {
    val assetsFolder: File get() = folder.resolve("Assets.xcassets")
    val swiftResourcesFolder: File get() = folder.resolve("resources")
    val layoutsFolder: File get() = swiftResourcesFolder.resolve("layouts")
    val baseFolderForLocalizations: File get() = folder.resolve("localizations")
}



