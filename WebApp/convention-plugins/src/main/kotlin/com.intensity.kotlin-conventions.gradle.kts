plugins {
    id("org.jetbrains.kotlin.jvm")
}

tasks.test {
    useJUnitPlatform()
}

kotlin {
    jvmToolchain(17)
}
