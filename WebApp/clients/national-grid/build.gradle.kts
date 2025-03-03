plugins {
    id("com.intensity.kotlin-conventions")
}

dependencies {
    implementation(projects.libraries.core)

    implementation(libs.http4k.core)
    implementation(libs.http4k.jackson)
    implementation(libs.result4k)

    testImplementation(projects.libraries.coreTest)

    testImplementation(libs.junit.engine)
    testImplementation(libs.http4k.hamkrest)
}
