import groovy.json.JsonOutput
import java.net.URL

buildscript {
    repositories {
        google()
        jcenter()
    }

    dependencies {
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.20")
        classpath("org.jetbrains.kotlin:kotlin-serialization:2.0.20")
    }
}

plugins {
    `java-library`
    `maven-publish`
    kotlin("jvm") version "2.0.20"
    kotlin("plugin.serialization") version "2.0.20"
    kotlin("kapt") version "2.0.20"
    id("net.researchgate.release") version "3.0.2"
}

group = "com.github.polygon-io"

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.0.20")

    val ktorVersion = "2.3.12"
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-websockets-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-okhttp-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // Annotation processor that generates Java builders for data classes
    val ktBuilderVersion = "1.2.2"
    implementation("com.thinkinglogic.builder:kotlin-builder-annotation:$ktBuilderVersion")
    kapt("com.thinkinglogic.builder:kotlin-builder-processor:$ktBuilderVersion")

    testImplementation("junit:junit:4.13.2")
}

allprojects {
    repositories {
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}

val sourcesJar =
    tasks.create("sources", Jar::class) {
        dependsOn(JavaPlugin.CLASSES_TASK_NAME)
        archiveClassifier = "sources"
        from(sourceSets["main"].allSource)
    }

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_22.toString()
        kotlinOptions.freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    register("restSpec") {
        group = "Gather Spec"
        description = "Retrieve the most recent Polygon.io REST openapi spec"

        val spec = JsonOutput.prettyPrint(URL("https://api.polygon.io/openapi").readText())
        File(".polygon/rest.json").writeText(spec)
    }

    register("websocketSpec") {
        group = "Gather Spec"
        description = "Retrieve the most recent Polygon.io websocket spec"

        val spec = JsonOutput.prettyPrint(URL("https://api.polygon.io/specs/websocket.json").readText())
        File(".polygon/websocket.json").writeText(spec)
    }

    artifacts {
        add("archives", sourcesJar)
        add("archives", jar)
    }
}

release {
    git {
        requireBranch = "master"
    }
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "com.github.polygon-io"
            artifactId = "client-jvm"
            version = "1.0.0"
            artifact(sourcesJar)

            from(components["java"])

            pom {
                name = "Polygon JVM Client SDK"
                description = "The official JVM client library SDK, written in Kotlin, for accessing the Polygon REST and WebSocket API."
                url = "https://github.com/Siege-Tech/client-jvm"

                scm {
                    connection = "scm:git:https://github.com/Siege-Tech/client-jvm.git"
                    developerConnection = "scm:git:https://github.com/Siege-Tech/client-jvm.git"
                    url = "https://github.com/Siege-Tech/client-jvm.git"
                }
            }
        }
    }

    repositories {
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/Siege-Tech/client-jvm")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
    }
}
