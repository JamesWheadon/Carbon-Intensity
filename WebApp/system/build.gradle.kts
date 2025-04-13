plugins {
    id("com.intensity.kotlin-conventions")
}

dependencies {
    testImplementation(projects.services.central)
    testImplementation(projects.services.limitCalculator)
    testImplementation(projects.services.weightedCalculator)
    testImplementation(projects.libraries.core)
    testImplementation(projects.libraries.coreTest)
    testImplementation(testFixtures(projects.clients.nationalGrid))
    testImplementation(testFixtures(projects.clients.octopus))

    testImplementation(libs.http4k.core)
    testImplementation(libs.http4k.jackson)
    testImplementation(libs.http4k.contract)
    testImplementation(libs.result4k)
    testImplementation(libs.junit.engine)
    testImplementation(libs.http4k.hamkrest)
    testImplementation(libs.http4k.approval)
    testImplementation(libs.http4k.tracerbullet)
}
