plugins {
    kotlin("jvm") version "2.0.0"
}

repositories {
    mavenCentral()
}

val http4kVersion: String by project
val result4kVersion: String by project

dependencies {
    implementation("org.http4k:http4k-testing-hamkrest:$http4kVersion")
    implementation("dev.forkhandles:result4k:$result4kVersion")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
