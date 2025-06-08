plugins {
    id("com.intensity.kotlin-conventions")
}

dependencies {
    implementation(libs.http4k.core)
    implementation(libs.openTelemetry.api)
    testFixturesImplementation(libs.openTelemetry.api)
    implementation(libs.openTelemetry.sdk)
    testFixturesImplementation(libs.openTelemetry.sdk)
    implementation(libs.openTelemetry.exporter)
    testFixturesImplementation(libs.openTelemetry.exporter)
    implementation(libs.openTelemetry.semconv)
    testFixturesImplementation(libs.openTelemetry.semconv)
    testFixturesImplementation(libs.openTelemetry.testing)

    testImplementation(libs.junit.engine)
    testImplementation(libs.junit.params)
    testImplementation(libs.http4k.hamkrest)
}
