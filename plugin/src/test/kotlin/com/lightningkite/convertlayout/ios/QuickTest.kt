package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.MacLocation
import com.lightningkite.convertlayout.rules.AttributeReplacement
import org.junit.Test
import java.io.File

class QuickTest {
    @Test
    fun testResources() {
        val translator = IosTranslator(
            androidFolder = File("/home/jivie/Projects/klyp-android/app"),
            iosFolder = File("/home/jivie/Projects/klyp-ios/Klyp"),
            iosName = "Klyp",
            iosModuleName = "KLYP",
            replacementFolders = listOf(File("/home/jivie/Projects/klyp-ios"), File("/home/jivie/Projects/android-xml-to-ios-xib/XmlToXibRuntime"), File("/home/jivie/Projects/RxSwiftPlus"))
        )
        println("Starting...")
        translator()
        println("Complete!")
    }

}