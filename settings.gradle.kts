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
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ListenExpenseTracker"
include(":app")

// Composite Build: Include independent core libraries with unambiguous project targets
includeBuild("../ListenArch") {
    dependencySubstitution {
        substitute(module("com.listen:listen-arch")).using(project(":listen-arch"))
    }
}
includeBuild("../ListenUiComponent") {
    dependencySubstitution {
        substitute(module("com.listen:listen-uicomponent")).using(project(":listen-uicomponent"))
    }
}