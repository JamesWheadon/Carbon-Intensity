plugins {
    id("com.intensity.deployable-conventions")
}

dependencies {
    implementation(projects.libraries.core)

    testImplementation(libs.junit.engine)
    testImplementation(libs.http4k.hamkrest)
}
