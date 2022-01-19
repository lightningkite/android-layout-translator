package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.web.WebTranslator
import org.junit.Test
import java.io.File

class QuickTest {
    @Test
    fun runIos() {
        val translator = IosTranslator(
            androidFolder = File("../test-project"),
            iosFolder = File("build/test-project-ios"),
            iosName = "TestProject",
            replacementFolders = listOf(File("../XmlToXibRuntime"))
        )
        println("Starting...")
        translator()
        println("Complete!")
    }
    @Test
    fun runWeb() {
        val translator = WebTranslator(
            androidFolder = File("../test-project"),
            webFolder = File("build/test-project-web"),
            webName = "TestProject",
            replacementFolders = listOf(File("../web-runtime"))
        )
        println("Starting...")
        translator()
        println("Complete!")
    }

}