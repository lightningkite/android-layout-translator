package com.lightningkite.convertlayout.ios

import java.io.File

data class IosProject(
    val folder: File,
    val name: String
) {
    val assetsFolder: File get() = folder.resolve("Assets.xcassets")
    val swiftResourcesFolder: File get() = folder.resolve("resources")
    val baseFolderForLocalizations: File get() = folder.resolve("localizations")
}