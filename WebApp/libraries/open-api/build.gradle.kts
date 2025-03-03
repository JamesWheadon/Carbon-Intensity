plugins {
    alias(libs.plugins.kotlin)
}

dependencies {
    implementation(projects.libraries.core)

    implementation(libs.http4k.jackson)
    implementation(libs.http4k.contract)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
