plugins {
    id("com.intensity.kotlin-conventions")
}

dependencies {
    implementation(projects.libraries.core)

    implementation(libs.http4k.jackson)
    implementation(libs.http4k.contract)
}
