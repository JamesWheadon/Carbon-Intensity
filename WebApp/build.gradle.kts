plugins {
    kotlin("jvm") version "2.0.0"
    id("com.google.cloud.tools.jib") version "3.4.4"
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
    implementation("dev.forkhandles:result4k:2.20.0.0")

    testImplementation("org.http4k:http4k-testing-approval:${http4kVersion}")
    testImplementation("org.http4k:http4k-testing-hamkrest:${http4kVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-api:${junitVersion}")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:${junitVersion}")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}

jib {
    from {
        image = "gcr.io/distroless/java17"
    }
    to {
        image = "europe-west2-docker.pkg.dev/eighth-sandbox-442218-k5/cloud-run-source-deploy/web-app:latest"
    }
    container {
        mainClass = "com.intensity.CarbonIntensityKt"
        ports = listOf("8080")
        environment = mapOf(
            "PORT" to "8080",
            "BASE_URI" to "https://scheduler-1088477649607.europe-west2.run.app"
        )
    }
}
