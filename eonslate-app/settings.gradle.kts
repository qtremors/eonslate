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
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    // Modules may not declare their own repositories.
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Eonslate"
include(":app")
include(":core:navigation:api")
include(":core:tracker:domain")
include(":core:tracker:data")
include(":core:tracker:ui")
include(":core:ui")
include(":feature:home")
include(":feature:entry")
include(":feature:trackerdetail")
include(":feature:trackereditor")
