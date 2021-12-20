package com.lightningkite.convertlayout.web

import com.lightningkite.convertlayout.MacLocation
import com.lightningkite.convertlayout.ios.IosTranslator
import com.lightningkite.convertlayout.xml.childElements
import com.lightningkite.convertlayout.xml.readXml
import com.lightningkite.convertlayout.xml.xpathElement
import com.lightningkite.convertlayout.xml.xpathElementOrCreate
import org.junit.Test
import java.io.File

class WebProjectTest {
    @Test
    fun testResources() {
        val translator = WebTranslator(
            androidFolder = File("../test-project"),
            webFolder = File("../test-project-web"),
            webName = "TestProject",
            replacementFolders = listOf(File("../web-runtime"))
        )
        println("Starting...")
//        translator.resources.fonts.forEach { key, value ->
//            println("$key: $value")
//        }
        translator()
        println("Complete!")
    }

}