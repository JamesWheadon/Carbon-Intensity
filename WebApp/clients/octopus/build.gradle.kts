plugins {
    id("com.intensity.client-conventions")
}

dependencies {
    implementation(projects.libraries.core)

    implementation(libs.http4k.core)
    implementation(libs.http4k.jackson)
    implementation(libs.result4k)

    testImplementation(testFixtures(projects.libraries.core))

    testImplementation(libs.junit.engine)
    testImplementation(libs.http4k.hamkrest)
}
