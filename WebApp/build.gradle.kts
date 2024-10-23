plugins {
    kotlin("jvm") version "2.0.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val http4kVersion: String by project
val http4kConnectVersion: String by project
val junitVersion: String by project
val kotlinVersion: String by project

dependencies {
    implementation(platform("org.http4k:http4k-bom:5.32.1.0"))
    implementation("org.http4k:http4k-core:${http4kVersion}")
    implementation("org.http4k:http4k-format-jackson:${http4kVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:${kotlinVersion}")

    testImplementation("org.http4k:http4k-testing-approval:${http4kVersion}")
    testImplementation("org.http4k:http4k-testing-hamkrest:${http4kVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:5.10.2")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(17)
}