rootProject.name = "WebApp"

includeSubProjects("libraries")
includeSubProjects("services")

fun includeSubProjects(directory: String) {
    file(directory).listFiles()?.forEach { file ->
        if (file.listFiles()?.any { child -> child.name == "build.gradle.kts" } == true) {
            include(":$directory:${file.name}")
        }
    }
}
