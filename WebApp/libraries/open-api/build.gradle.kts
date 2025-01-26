plugins {
    kotlin("jvm") version "2.0.0"
}

repositories {
    mavenCentral()
}

val http4kVersion: String by project

dependencies {
    implementation(project(":libraries:core"))

    implementation("org.http4k:http4k-format-jackson:$http4kVersion")
    implementation("org.http4k:http4k-contract:$http4kVersion")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
