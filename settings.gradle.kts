pluginManagement {
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS) // 중요!
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "RecordWithMe"
include(":app")