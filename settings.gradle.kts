rootProject.name = "android-xml-to-ios-xib"

pluginManagement {
    repositories {
        mavenCentral()
        maven("https://plugins.gradle.org/m2/")
    }
}

include("plugin")
include("android-runtime")
include("test-project")