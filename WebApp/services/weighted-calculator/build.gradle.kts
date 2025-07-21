plugins {
    id("com.intensity.deployable-conventions")
}

dependencies {
    implementation(projects.libraries.core)
    implementation(projects.libraries.observabilty)

    implementation(libs.http4k.core)
    implementation(libs.http4k.jackson)
    implementation(libs.result4k)

    testImplementation(testFixtures(projects.libraries.core))
    testImplementation(testFixtures(projects.libraries.observabilty))

    testImplementation(libs.junit.engine)
    testImplementation(libs.http4k.hamkrest)
}
