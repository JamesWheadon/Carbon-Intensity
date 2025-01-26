plugins {
    kotlin("jvm") version "2.0.0"
}

repositories {
    mavenCentral()
}

val http4kVersion: String by project

dependencies {
    implementation("org.http4k:http4k-format-jackson:$http4kVersion")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
