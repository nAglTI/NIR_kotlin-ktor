import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    kotlin("multiplatform") version "1.4.31"
    application
}

group = "ru.nagl"
version = "1.0-SNAPSHOT"

repositories {
    jcenter()
    mavenCentral()
    maven { url = uri("https://dl.bintray.com/kotlin/kotlinx") }
    maven { url = uri("https://dl.bintray.com/kotlin/ktor") }
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "11"
        }
        withJava()
    }
    js(IR) {
        browser {
            binaries.executable()
            webpackTask {
                cssSupport.enabled = true
            }
            runTask {
                cssSupport.enabled = true
            }
        }
    }
    sourceSets {
        val commonMain by getting
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation("com.h2database:h2:1.4.200")
                implementation("org.xerial:sqlite-jdbc:3.34.0")
                implementation("io.ktor:ktor-server-netty:1.4.+")
                implementation("io.ktor:ktor-html-builder:1.4.+")
                implementation("io.ktor:ktor-serialization:1.4.+")
                implementation("io.ktor:ktor-server-sessions:1.4.+")
                implementation("org.jetbrains.kotlinx:kotlinx-html-jvm:0.7.2")
                implementation("org.slf4j:slf4j-log4j12:2.0.0-alpha1")
                implementation("org.jetbrains.exposed:exposed-core:0.30.1")
                implementation("org.jetbrains.exposed:exposed-dao:0.30.1")
                implementation("org.jetbrains.exposed:exposed-jdbc:0.30.1")
                implementation("org.postgresql:postgresql:42.2.19")
                implementation("org.jetbrains:kotlin-css-jvm:1.0.0-pre.148-kotlin-1.4.30")
            }
        }
        val jvmTest by getting
        val jsMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-html:0.7.2")
            }
        }
        val jsTest by getting
    }
}

application {
    mainClassName = "ServerKt"
}

tasks.getByName<KotlinWebpack>("jsBrowserProductionWebpack") {
    outputFileName = "output.js"
}

tasks.getByName<Jar>("jvmJar") {
    dependsOn(tasks.getByName("jsBrowserProductionWebpack"))
    val jsBrowserProductionWebpack = tasks.getByName<KotlinWebpack>("jsBrowserProductionWebpack")
    from(File(jsBrowserProductionWebpack.destinationDirectory, jsBrowserProductionWebpack.outputFileName))
}

tasks.getByName<JavaExec>("run") {
    dependsOn(tasks.getByName<Jar>("jvmJar"))
    classpath(tasks.getByName<Jar>("jvmJar"))
}