pluginManagement {
    plugins {
        id("org.jetbrains.kotlin.android") version "2.2.10"
        id("org.jetbrains.kotlin.plugin.compose") version "2.2.10"
    }

    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven { url = uri("https://packages.bureau.id/api/packages/Bureau/maven") }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://packages.bureau.id/api/packages/Bureau/maven") }
    }
}

rootProject.name = "Test OAuth"
include(":app")
include(":bureauidapp")
