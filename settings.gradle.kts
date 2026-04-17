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

rootProject.name = "ChessRoguelike"
include(":app")
include(":core-game")
include(":content-io")
include(":desktop-app")

project(":app").projectDir = file("apps/android")
project(":desktop-app").projectDir = file("apps/desktop")
project(":core-game").projectDir = file("libraries/core-game")
project(":content-io").projectDir = file("libraries/content-io")
