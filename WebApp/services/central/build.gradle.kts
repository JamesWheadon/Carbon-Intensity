plugins {
    id("com.intensity.deployable-conventions")
}

dependencies {
    implementation(projects.libraries.core)
    implementation(projects.libraries.openApi)
    implementation(projects.clients.nationalGrid)
    implementation(projects.clients.scheduler)

    implementation(libs.http4k.core)
    implementation(libs.http4k.jackson)
    implementation(libs.http4k.contract)
    implementation(libs.result4k)

    testImplementation(projects.libraries.coreTest)
    testImplementation(testFixtures(projects.clients.nationalGrid))
    testImplementation(testFixtures(projects.clients.scheduler))

    testImplementation(libs.junit.engine)
    testImplementation(libs.http4k.hamkrest)
    testImplementation(libs.http4k.approval)
    testImplementation(libs.http4k.tracerbullet)
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
