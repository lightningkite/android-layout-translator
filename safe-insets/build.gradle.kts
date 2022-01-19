import com.lightningkite.deployhelpers.*

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("signing")
    id("org.jetbrains.dokka")
    `maven-publish`
}

group = "com.lightningkite.xmltoxib"

repositories {
    mavenCentral()
    google()
    mavenLocal()
}

android {
    compileSdkVersion(31)
    defaultConfig {
        minSdkVersion(21)
        targetSdkVersion(31)
        versionCode = 0
        versionName = "0.0.1"
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    api("androidx.appcompat:appcompat:1.4.0")
    api("dev.b3nedikt.viewpump:viewpump:4.0.8")
    api("com.google.android.material:material:1.4.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:1.1.5")
}


standardPublishing {
    name.set("Android Safe Insets")
    description.set("A tool for making safe insets work easily on Android.")
    github("lightningkite", "android-xml-to-ios-xib")

    licenses { mit() }

    developers {
        developer(
            id = "LightningKiteJoseph",
            name = "Joseph Ivie",
            email = "joseph@lightningkite.com",
        )
    }
}
