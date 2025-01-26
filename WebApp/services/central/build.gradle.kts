plugins {
    kotlin("jvm") version "2.0.0"
    id("com.google.cloud.tools.jib") version "3.4.4"
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
    implementation(platform("org.http4k:http4k-bom:$http4kBomVersion"))

    implementation(project(":libraries:core"))
    implementation(project(":libraries:national-grid"))
    implementation(project(":libraries:open-api"))

    implementation("org.http4k:http4k-core:$http4kVersion")
    implementation("org.http4k:http4k-format-jackson:$http4kVersion")
    implementation("org.http4k:http4k-contract:$http4kVersion")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    implementation("dev.forkhandles:result4k:$result4kVersion")

    testImplementation(project(":libraries:core-test"))

    testImplementation("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testImplementation("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
    testImplementation("org.http4k:http4k-testing-approval:$http4kVersion")
    testImplementation("org.http4k:http4k-testing-hamkrest:$http4kVersion")
    testImplementation("org.http4k:http4k-testing-tracerbullet:$http4kVersion")
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
            "SCHEDULER_URL" to "https://scheduler-1088477649607.europe-west2.run.app"
        )
    }
}
