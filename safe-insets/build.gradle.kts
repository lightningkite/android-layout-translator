import com.lightningkite.deployhelpers.*

plugins {
    id("com.android.library")
    id("kotlin-android")
    id("signing")
    id("org.jetbrains.dokka")
    `maven-publish`
}

android {
    compileSdk = 31
    defaultConfig {
        minSdk = 21
        targetSdk = 31
    }
    compileOptions {
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.4.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.4.0")
    api("androidx.appcompat:appcompat:1.4.1")
    api("dev.b3nedikt.viewpump:viewpump:4.0.10")
    api("com.google.android.material:material:1.5.0")
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
