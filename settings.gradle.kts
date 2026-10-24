rootProject.name = "local-music-player"

include(":shared")
include(":androidApp")
include(":desktopApp")

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}
