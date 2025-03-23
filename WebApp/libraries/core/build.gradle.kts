plugins {
    id("com.intensity.kotlin-conventions")
}

dependencies {
    implementation(libs.http4k.jackson)
    implementation(libs.result4k)

    testImplementation(projects.libraries.coreTest)

    testImplementation(libs.junit.engine)
    testImplementation(libs.http4k.hamkrest)
}
