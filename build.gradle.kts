import java.util.Properties

val kotlinVersion = "1.5.30"
plugins {
    kotlin("jvm") version "1.5.30"
    java
    `java-gradle-plugin`
    idea
    signing
    id("org.jetbrains.dokka") version "1.4.20"
    `maven-publish`
}

group = "com.lightningkite.khrysalis"
version = "0.1.0"

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

if(signingKey != null) {
    if(!signingKey.contains('\n')){
        throw IllegalArgumentException("Expected signing key to have multiple lines")
    }
    if(signingKey.contains('"')){
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

gradlePlugin {
    plugins {
        val khrysalisPlugin by creating() {
            id = "com.lightningkite.khrysalis"
            implementationClass = "com.lightningkite.khrysalis.gradle.KhrysalisPlugin"
        }
    }
}

repositories {
    mavenLocal()
    jcenter()
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

    api("com.fasterxml.jackson.core:jackson-databind:2.12.5")
    api("com.fasterxml.jackson.module:jackson-module-kotlin:2.12.5")
    api("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:2.12.5")

    // https://mvnrepository.com/artifact/org.apache.xmlgraphics/batik-transcoder
    implementation(group = "org.apache.xmlgraphics", name = "batik-transcoder", version = "1.13")
    implementation(group = "org.apache.xmlgraphics", name = "batik-codec", version = "1.13")

    // https://mvnrepository.com/artifact/net.mabboud.fontverter/FontVerter
    implementation(group = "net.mabboud.fontverter", name = "FontVerter", version = "1.2.22")

    testImplementation("junit:junit:4.12")
}

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
        publications {
            val release by creating(MavenPublication::class) {
                from(components["java"])
                artifact(tasks["sourceJar"])
                artifact(tasks["javadocJar"])
                groupId = project.group.toString()
                artifactId = project.name
                version = project.version.toString()
            }
        }
    }
    if(useSigning){
        signing {
            useInMemoryPgpKeys(signingKey, signingPassword)
            sign(configurations.archives.get())
        }
    }
}

if(useDeployment){
    tasks.register("uploadSnapshot"){
        group="upload"
        finalizedBy("uploadArchives")
        doLast{
            project.version = project.version.toString() + "-SNAPSHOT"
        }
    }

    tasks.named<Upload>("uploadArchives") {
//        repositories.withConvention(MavenRepositoryHandlerConvention::class) {
//            mavenDeployer {
//                beforeDeployment {
//                    signing.signPom(this)
//                }
//            }
//        }

        repositories.withGroovyBuilder {
            "mavenDeployer"{
                "repository"("url" to "https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/") {
                    "authentication"(
                        "userName" to deploymentUser,
                        "password" to deploymentPassword
                    )
                }
                "snapshotRepository"("url" to "https://s01.oss.sonatype.org/content/repositories/snapshots/") {
                    "authentication"(
                        "userName" to deploymentUser,
                        "password" to deploymentPassword
                    )
                }
                "pom" {
                    "project" {
                        setProperty("name", "Android XML to iOS Xib Converter")
                        setProperty("packaging", "jar")
                        setProperty(
                            "description",
                            "Convert from Android resources to iOS."
                        )
                        setProperty("url", "https://github.com/lightningkite/khrysalis")

                        "scm" {
                            setProperty("connection", "scm:git:https://github.com/lightningkite/khrysalis.git")
                            setProperty(
                                "developerConnection",
                                "scm:git:https://github.com/lightningkite/khrysalis.git"
                            )
                            setProperty("url", "https://github.com/lightningkite/khrysalis")
                        }

                        "licenses" {
                            "license"{
                                setProperty("name", "GNU General Public License v3.0")
                                setProperty("url", "https://www.gnu.org/licenses/gpl-3.0.en.html")
                                setProperty("distribution", "repo")
                            }
                            "license"{
                                setProperty("name", "Commercial License")
                                setProperty("url", "https://www.lightningkite.com")
                                setProperty("distribution", "repo")
                            }
                        }
                        "developers"{
                            "developer"{
                                setProperty("id", "bjsvedin")
                                setProperty("name", "Brady Svedin")
                                setProperty("email", "brady@lightningkite.com")
                            }
                        }
                    }
                }
            }
        }
    }
}