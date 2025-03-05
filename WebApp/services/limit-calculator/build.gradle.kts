plugins {
    id("com.intensity.deployable-conventions")
}

dependencies {
    implementation(projects.libraries.core)

    implementation(libs.result4k)

    testImplementation(projects.libraries.coreTest)

    testImplementation(libs.junit.engine)
    testImplementation(libs.http4k.hamkrest)
}
