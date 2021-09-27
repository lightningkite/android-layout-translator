package com.lightningkite.convertlayout.ios

import com.lightningkite.convertlayout.MacLocation
import com.lightningkite.convertlayout.android.AndroidResources
import com.lightningkite.convertlayout.rules.Replacements
import com.lightningkite.convertlayout.xml.childElements
import com.lightningkite.convertlayout.xml.readXml
import com.lightningkite.convertlayout.xml.xpathElement
import com.lightningkite.convertlayout.xml.xpathElementOrCreate
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class IosProjectTest {
    val macLocation = MacLocation(
        File("XmlToXibRuntime/XmlToXibRuntimeExample/XmlToXibRuntimeExample"),
        "/Users/joseph/sync/XmlToXibRuntime/XmlToXibRuntimeExample/XmlToXibRuntimeExample"
    )

    @Test fun justPush() {
        macLocation.push()
    }

    @Test
    fun testResources() {
        macLocation.pull()

        println("Reading...")
        val translator = IosTranslator(
            androidFolder = File("test-project/app"),
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
    fun xpathTest() {
        val xml = """
        <stackView alignment="center" contentMode="scaleToFill" customClass="LabeledToggle" customModule="XmlToXibRuntime" id="radioView2" opaque="NO" spacing="16" translatesAutoresizingMaskIntoConstraints="NO" verticalHuggingPriority="750">
          <subviews>
            <view contentMode="scaleToFill" customClass="M13Checkbox" customModule="M13Checkbox" horizontalHuggingPriority="750" id="qXZ-w9-97U" translatesAutoresizingMaskIntoConstraints="NO" verticalHuggingPriority="750">
              <color key="backgroundColor" systemColor="systemBackgroundColor"/>
              <constraints>
                <constraint constant="20.5" firstAttribute="height" id="OOV-Fn-fHE"/>
                <constraint constant="20.5" firstAttribute="width" id="yo9-ae-yfC"/>
              </constraints>
              <userDefinedRuntimeAttributes>
                <userDefinedRuntimeAttribute keyPath="_IBMarkType" type="string" value="Radio"/>
                <userDefinedRuntimeAttribute keyPath="_IBBoxType" type="string" value="Circle"/>
                <userDefinedRuntimeAttribute keyPath="_IBCheckState" type="string" value="Unchecked"/>
                <userDefinedRuntimeAttribute keyPath="checkmarkLineWidth" type="number">
                  <real key="value" value="1"/>
                </userDefinedRuntimeAttribute>
                <userDefinedRuntimeAttribute keyPath="boxLineWidth" type="number">
                  <real key="value" value="1"/>
                </userDefinedRuntimeAttribute>
              </userDefinedRuntimeAttributes>
            </view>
            <label adjustsFontSizeToFit="NO" baselineAdjustment="alignBaselines" contentMode="left" customClass="StyledUILabel" customModule="XmlToXibRuntime" horizontalHuggingPriority="100" id="fZN-wP-dlp" lineBreakMode="tailTruncation" opaque="NO" text="Test Text" textAlignment="natural" translatesAutoresizingMaskIntoConstraints="NO" userInteractionEnabled="NO" verticalHuggingPriority="251">
              <fontDescription key="fontDescription" pointSize="17" type="system"/>
              <userDefinedRuntimeAttributes>
                <userDefinedRuntimeAttribute keyPath="textAllCaps" type="boolean" value="YES"/>
              </userDefinedRuntimeAttributes>
              <color key="color" name="color_state_primary"/>
            </label>
          </subviews>
          <edgeInsets bottom="8" key="layoutMargins" left="8" right="8" top="8"/>
        </stackView>
        """.trimIndent().readXml().documentElement
        println(xml.xpathElement("subviews/label")?.childElements?.joinToString())
        println(xml.xpathElement("subviews/label/fontDescription[@key='fontDescription']"))
        println(xml.xpathElementOrCreate("subviews/label/fontDescription[@key='fontDescription']"))
    }
}