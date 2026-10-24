rootProject.name = "nocturne"

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
