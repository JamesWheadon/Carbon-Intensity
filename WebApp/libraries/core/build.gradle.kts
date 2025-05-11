plugins {
    id("com.intensity.kotlin-conventions")
}

dependencies {
    implementation(libs.http4k.jackson)
    implementation(libs.result4k)

    testImplementation(libs.junit.engine)
    testImplementation(libs.http4k.hamkrest)

    testFixturesImplementation(libs.http4k.hamkrest)
    testFixturesImplementation(libs.http4k.jackson)
    testFixturesImplementation(libs.result4k)
}
