plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose.compiler)
}

android {
    namespace = "com.never.nocturne"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.never.nocturne"
        minSdk = 24
        targetSdk = 37
        versionCode = 2
        versionName = "0.2.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(compose.material3)
    implementation(compose.ui)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.documentfile)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.media3.exoplayer)
    implementation(libs.media3.session)

    implementation("dev.chrisbanes.haze:haze:1.6.10")
    implementation("dev.chrisbanes.haze:haze-materials:1.6.10")
    implementation("androidx.palette:palette-ktx:1.0.0")
    implementation("io.coil-kt:coil-compose:2.7.0")
}
