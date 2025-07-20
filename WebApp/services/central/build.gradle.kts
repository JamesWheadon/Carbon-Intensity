plugins {
    id("com.intensity.deployable-conventions")
}

dependencies {
    implementation(projects.libraries.core)
    implementation(projects.libraries.observabilty)
    implementation(projects.libraries.openApi)
    implementation(projects.libraries.nationalGrid)
    implementation(projects.libraries.octopus)

    implementation(libs.http4k.core)
    implementation(libs.http4k.jackson)
    implementation(libs.http4k.contract)
    implementation(libs.result4k)

    testImplementation(testFixtures(projects.libraries.core))
    testImplementation(testFixtures(projects.libraries.nationalGrid))
    testImplementation(testFixtures(projects.libraries.octopus))
    testImplementation(testFixtures(projects.libraries.observabilty))

    testImplementation(libs.junit.engine)
    testImplementation(libs.http4k.hamkrest)
    testImplementation(libs.http4k.approval)
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
