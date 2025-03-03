pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

rootProject.name = "WebApp"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

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
