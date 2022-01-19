import com.lightningkite.deployhelpers.*
import java.util.Properties

val kotlinVersion = "1.6.0"
plugins {
    kotlin("jvm")
    java
    `java-gradle-plugin`
    idea
    signing
    id("org.jetbrains.dokka")
    `maven-publish`
}

group = "com.lightningkite.xmltoxib"

gradlePlugin {
    plugins {
        val khrysalisPlugin by creating() {
            id = "com.lightningkite.xmltoxib"
            implementationClass = "com.lightningkite.convertlayout.gradle.XmlToXibPlugin"
        }
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    google()
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    api(localGroovy())
    api(gradleApi())

    api(group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin", version = kotlinVersion)
    api(group = "org.jetbrains.kotlin", name = "kotlin-gradle-plugin-api", version = kotlinVersion)

    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    api("com.fasterxml.jackson.core:jackson-databind:2.13.0")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.13.0")

    // https://mvnrepository.com/artifact/org.apache.xmlgraphics/batik-transcoder
    implementation(group = "org.apache.xmlgraphics", name = "batik-transcoder", version = "1.13")
    implementation(group = "org.apache.xmlgraphics", name = "batik-codec", version = "1.13")

    // https://mvnrepository.com/artifact/net.mabboud.fontverter/FontVerter
    implementation(group = "org.apache.pdfbox", name = "fontbox", version = "2.0.24")
//    implementation(group = "net.mabboud.fontverter", name = "FontVerter", version = "1.2.22")

    testImplementation("junit:junit:4.13.2")
}



standardPublishing {
    name.set("Android XML to iOS Xib Converter")
    description.set("A Gradle plugin that automatically generates ViewGenerators from Android Layout XMLs")
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
