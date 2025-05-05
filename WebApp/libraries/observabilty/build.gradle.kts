plugins {
    id("com.intensity.kotlin-conventions")
}

dependencies {
    testFixturesImplementation(libs.openTelemetry.api)
    testFixturesImplementation(libs.openTelemetry.sdk)
    testFixturesImplementation(libs.openTelemetry.exporter)
    testFixturesImplementation(libs.openTelemetry.semconv)
    testFixturesImplementation(libs.openTelemetry.testing)
}
