package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.MacLocation
import com.lightningkite.convertlayout.android.AndroidResources
import com.lightningkite.convertlayout.rules.Replacements
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class IosProjectTest {
    val macLocation = MacLocation(
        File("XmlToXibRuntime/XmlToXibRuntimeExample/XmlToXibRuntimeExample"),
        "/Users/joseph/sync/XmlToXibRuntime/XmlToXibRuntimeExample/XmlToXibRuntimeExample"
    )

    @Test
    fun testResources() {
        macLocation.pull()

        println("Reading...")
        val translator = IosTranslator(
            androidFolder = File("test-project/app"),
            iosFolder = macLocation.file.also { it.mkdirs() },
            iosName = "XmlToXibRuntime",
            replacementFolders = listOf(File("."))
        )

        println("Resources: ")
        for(i in translator.resources.all) {
            println(i.key + ": " + i.value)
        }

        println("Rules for Elements: ")
        for(i in translator.replacements.elements) {
            println(i.key + ": " + i.value)
        }

        println("Rules for Attributes: ")
        for(i in translator.replacements.attributes) {
            println(i.key + ": " + i.value)
        }

        println("Starting...")
        translator()
        println("Complete!")

//        println("Pushing...")
//        macLocation.push()
    }
}