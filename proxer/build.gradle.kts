plugins {
    kotlin("jvm") version "2.1.0"
    kotlin("plugin.serialization") version "1.9.21"

    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.iizhukov"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.ktor:ktor-server-core:2.3.0")
    implementation("io.ktor:ktor-server-netty:2.3.0")
    implementation("io.ktor:ktor-client-cio:2.3.0")
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
    implementation("io.lettuce:lettuce-core:6.2.3.RELEASE")
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:4.11.0")
    implementation("io.micrometer:micrometer-registry-prometheus:1.10.5")
    implementation("io.insert-koin:koin-ktor:3.4.1")
    implementation("io.ktor:ktor-server-content-negotiation:2.3.0")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.0")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    manifest {
        attributes["Main-Class"] = "MainKt"
    }
}
