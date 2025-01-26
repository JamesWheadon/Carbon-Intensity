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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

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
