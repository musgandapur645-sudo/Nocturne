import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.ui)
    implementation(project(":shared"))
}

compose.desktop {
    application {
        mainClass = "com.never.localmusic.desktop.MainKt"

        nativeDistributions {
            // Windows and Linux packaging; Phase 0 proves the pipeline with an app image
            // (.deb needs dpkg-deb, which Arch doesn't ship; MSI/EXE need a Windows host)
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "local-music-player"
            packageVersion = "0.1.0"
        }
    }
}
