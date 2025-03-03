plugins {
    alias(libs.plugins.kotlin)
}

dependencies {
    implementation(libs.http4k.hamkrest)
    implementation(libs.result4k)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
