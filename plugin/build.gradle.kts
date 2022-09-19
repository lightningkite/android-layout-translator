import com.lightningkite.deployhelpers.*

plugins {
    kotlin("jvm")
    java
    `java-gradle-plugin`
    idea
    signing
    id("org.jetbrains.dokka")
    `maven-publish`
}


gradlePlugin {
    plugins {
        val khrysalisPlugin by creating() {
            id = "com.lightningkite.androidlayouttranslator"
            implementationClass = "com.lightningkite.convertlayout.gradle.AndroidLayoutConverterPlugin"
        }
    }
}


tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class).all {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

val kotlinVersion:String by project
val jacksonVersion:String by project
dependencies {
    api(localGroovy())
    api(gradleApi())

    api("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    api("org.jetbrains.kotlin:kotlin-gradle-plugin-api:$kotlinVersion")

    implementation("org.jetbrains.kotlin:kotlin-stdlib")

    api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")

    // https://mvnrepository.com/artifact/org.apache.xmlgraphics/batik-transcoder
    implementation("org.apache.xmlgraphics:batik-transcoder:1.14")
    implementation("org.apache.xmlgraphics:batik-codec:1.14")

    // https://mvnrepository.com/artifact/net.mabboud.fontverter/FontVerter
    implementation("org.apache.pdfbox:fontbox:2.0.26")
//    implementation(group = "net.mabboud.fontverter", name = "FontVerter", version = "1.2.22")

    testImplementation("junit:junit:4.13.2")
}



standardPublishing {
    name.set("Android XML Converter")
    description.set("A Gradle plugin that translates Android XML layouts to Xib and HTML")
    github("lightningkite", "android-layout-translator")

    licenses { mit() }

    developers {
        developer(
            id = "LightningKiteJoseph",
            name = "Joseph Ivie",
            email = "joseph@lightningkite.com",
        )
    }
}
