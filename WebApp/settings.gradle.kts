rootProject.name = "WebApp"

pluginManagement {
    repositories {
        gradlePluginPortal()
    }
    includeBuild("convention-plugins")
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include("system")
includeSubProjects("clients")
includeSubProjects("libraries")
includeSubProjects("services")

fun includeSubProjects(directory: String) {
    file(directory).listFiles()?.forEach { file ->
        if (file.listFiles()?.any { child -> child.name == "build.gradle.kts" } == true) {
            include(":$directory:${file.name}")
        }
    }
}
