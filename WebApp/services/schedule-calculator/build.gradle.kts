plugins {
    alias(libs.plugins.kotlin)
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(projects.libraries.core)

    implementation(libs.http4k.core)
    implementation(libs.http4k.jackson)
    implementation(libs.result4k)

    testImplementation(projects.libraries.coreTest)

    testImplementation(libs.junit.engine)
    testImplementation(libs.http4k.hamkrest)
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
