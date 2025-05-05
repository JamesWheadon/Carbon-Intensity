plugins {
    id("org.jetbrains.kotlin.jvm")
    id("java-test-fixtures")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
