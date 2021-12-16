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
version = "0.7.3"

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


// Signing and publishing
val props = project.rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { stream ->
    Properties().apply { load(stream) }
}
val signingKey: String? = (System.getenv("SIGNING_KEY")?.takeUnless { it.isEmpty() }
    ?: props?.getProperty("signingKey")?.toString())
    ?.lineSequence()
    ?.filter { it.trim().firstOrNull()?.let { it.isLetterOrDigit() || it == '=' || it == '/' || it == '+' } == true }
    ?.joinToString("\n")
val signingPassword: String? = System.getenv("SIGNING_PASSWORD")?.takeUnless { it.isEmpty() }
    ?: props?.getProperty("signingPassword")?.toString()
val useSigning = signingKey != null && signingPassword != null

if (signingKey != null) {
    if (!signingKey.contains('\n')) {
        throw IllegalArgumentException("Expected signing key to have multiple lines")
    }
    if (signingKey.contains('"')) {
        throw IllegalArgumentException("Signing key has quote outta nowhere")
    }
}

val deploymentUser = (System.getenv("OSSRH_USERNAME")?.takeUnless { it.isEmpty() }
    ?: props?.getProperty("ossrhUsername")?.toString())
    ?.trim()
val deploymentPassword = (System.getenv("OSSRH_PASSWORD")?.takeUnless { it.isEmpty() }
    ?: props?.getProperty("ossrhPassword")?.toString())
    ?.trim()
val useDeployment = deploymentUser != null || deploymentPassword != null


tasks {

    val sourceJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets["main"].java.srcDirs)
        from(project.projectDir.resolve("src/include"))
    }
    val javadocJar by creating(Jar::class) {
        dependsOn("dokkaJavadoc")
        archiveClassifier.set("javadoc")
        from(project.file("build/dokka/javadoc"))
    }
    artifacts {
        archives(sourceJar)
        archives(javadocJar)
    }
}

afterEvaluate {
    publishing {
        this.publications.forEach {
            (it as MavenPublication).setPom()
        }
        publications.getByName<MavenPublication>("pluginMaven") {
            artifact(tasks.getByName("sourceJar"))
            if (useSigning) {
                artifact(tasks.getByName("javadocJar"))
            }
        }
        if (useDeployment) {
            repositories {
                maven {
                    name = "MavenCentral"
                    val releasesRepoUrl = "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/"
                    val snapshotsRepoUrl = "https://s01.oss.sonatype.org/content/repositories/snapshots/"
                    url = uri(if (version.toString().endsWith("SNAPSHOT")) snapshotsRepoUrl else releasesRepoUrl)
                    credentials {
                        this.username = deploymentUser
                        this.password = deploymentPassword
                    }
                }
            }
        }
    }
    if (useSigning) {
        signing {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(publishing.publications)
        }
    }
}

fun MavenPublication.setPom() {
    pom {
        name.set("Android XML to iOS Xib Converter")
        description.set("A Gradle plugin that automatically generates ViewGenerators from Android Layout XMLs")
        url.set("https://github.com/lightningkite/android-xml-to-ios-xib")

        scm {
            connection.set("scm:git:https://github.com/lightningkite/android-xml-to-ios-xib.git")
            developerConnection.set("scm:git:https://github.com/lightningkite/android-xml-to-ios-xib.git")
            url.set("https://github.com/lightningkite/android-xml-to-ios-xib")
        }

        licenses {
            license {
                name.set("The MIT License (MIT)")
                url.set("https://www.mit.edu/~amini/LICENSE.md")
                distribution.set("repo")
            }
        }

        developers {
            developer {
                id.set("LightningKiteJoseph")
                name.set("Joseph Ivie")
                email.set("joseph@lightningkite.com")
            }
        }
    }
}