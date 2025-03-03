plugins {
    id("org.jetbrains.kotlin.jvm") version "2.0.0"
    `kotlin-dsl`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.0")
    implementation("com.google.cloud.tools:jib-gradle-plugin:3.4.4")
}
