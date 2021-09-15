package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.android.AndroidResources
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class IosProjectTest {
    @Test
    fun testResources(){
        val resources = AndroidResources()
        resources.parse(File("test-project/app/src/main/res").also { println(it.absolutePath) })
        println("Resources: ")
        for(i in resources.all) {
            println(i.key + ": " + i.value)
        }
        val ios = IosProject(File("test-project-ios").also { it.mkdirs() }, "TestIosProject")
        ios.importResources(resources)
    }
}