plugins {
    alias(libs.plugins.kotlin)
}

dependencies {
    implementation(libs.http4k.jackson)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
