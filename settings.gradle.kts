pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RealPredictAI"

// UI (ya existente en el repositorio — sin modificar)
include(":app")

// Módulos del motor (engine) — añadidos por el agente Z.ai
include(":core-common")
include(":core-domain")
include(":core-statistics")
include(":core-data")
include(":core-security")
include(":core-ai")
