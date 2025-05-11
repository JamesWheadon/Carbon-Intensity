plugins {
    id("com.intensity.deployable-conventions")
}

dependencies {
    implementation(projects.libraries.core)

    implementation(libs.http4k.core)
    implementation(libs.http4k.jackson)
    implementation(libs.result4k)

    implementation(libs.http4k.openTelemetry)
    implementation(libs.openTelemetry.api)
    implementation(libs.openTelemetry.sdk)
    implementation(libs.openTelemetry.exporter)
    implementation(libs.openTelemetry.semconv)

    testImplementation(testFixtures(projects.libraries.core))

    testImplementation(libs.junit.engine)
    testImplementation(libs.http4k.hamkrest)
}
