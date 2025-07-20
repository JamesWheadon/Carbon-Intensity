plugins {
    id("com.intensity.kotlin-conventions")
}

dependencies {
    testImplementation(projects.services.central)
    testImplementation(projects.services.limitCalculator)
    testImplementation(projects.services.weightedCalculator)
    testImplementation(projects.libraries.core)
    testImplementation(projects.libraries.observabilty)
    testImplementation(testFixtures(projects.libraries.core))
    testImplementation(testFixtures(projects.libraries.nationalGrid))
    testImplementation(testFixtures(projects.libraries.octopus))
    testImplementation(testFixtures(projects.libraries.observabilty))

    testImplementation(libs.http4k.core)
    testImplementation(libs.http4k.jackson)
    testImplementation(libs.http4k.contract)
    testImplementation(libs.result4k)
    testImplementation(libs.junit.engine)
    testImplementation(libs.http4k.hamkrest)
    testImplementation(libs.http4k.approval)
}
