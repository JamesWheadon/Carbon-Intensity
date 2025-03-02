plugins {
    kotlin("jvm") version "2.0.0"
}

repositories {
    mavenCentral()
}

val http4kVersion: String by project
val http4kBomVersion: String by project
val junitVersion: String by project
val kotlinVersion: String by project
val result4kVersion: String by project

dependencies {
    implementation(project(":libraries:core"))

    implementation("org.http4k:http4k-core:$http4kVersion")
    implementation("org.http4k:http4k-format-jackson:$http4kVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("dev.forkhandles:result4k:$result4kVersion")

    testImplementation(project(":libraries:core-test"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.http4k:http4k-testing-hamkrest:$http4kVersion")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
