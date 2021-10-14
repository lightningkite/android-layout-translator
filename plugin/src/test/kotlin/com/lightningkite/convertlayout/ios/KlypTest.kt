package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.MacLocation
import org.junit.Test
import java.io.File

class KlypTest {
    val macLocation = MacLocation(
        File("KlypConversionTesting/XmlToXibRuntimeExample").also { it.mkdirs() },
        "/Users/joseph/sync/KlypConversionTesting/XmlToXibRuntimeExample"
    )

    @Test fun justPush() {
        macLocation.push()
    }

    @Test
    fun testResources() {
        macLocation.pull()

        println("Reading...")
        val translator = IosTranslator(
            androidFolder = File("/home/jivie/Projects/klyp-khrysalis/app"),
            iosFolder = macLocation.file.also { it.mkdirs() },
            iosName = "XmlToXibRuntimeExample",
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

        println("Pushing...")
        macLocation.push()
    }

    @Test
    fun testSection() {
        macLocation.pull()

        println("Reading...")
        val translator = IosTranslator(
            androidFolder = File("/home/jivie/Projects/klyp-khrysalis/app"),
            iosFolder = macLocation.file.also { it.mkdirs() },
            iosName = "XmlToXibRuntimeExample",
            replacementFolders = listOf(File("."))
        )

        println("drawables: ")
        for(i in translator.resources.drawables) {
            println(i.key + ": " + i.value)
        }

        with(translator) {
            translate(translator.resources.layouts["stylist_profile"]!!.layout.value)
        }

        println("Pushing...")
        macLocation.push()
    }

}