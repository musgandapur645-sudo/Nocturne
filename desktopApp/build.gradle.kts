import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.bundling.Jar
import java.io.File

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
            packageName = "nocturne"
            packageVersion = "0.1.0"
        }
    }
}

// ============================================================
// Option A: Fat jar (self-contained, includes all dependencies)
// Runs with: java -jar desktopApp-runnable.jar
// ============================================================
tasks.register<Jar>("runnableJar") {
    archiveClassifier.set("runnable")
    manifest {
        attributes["Main-Class"] = "com.never.localmusic.desktop.MainKt"
    }
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    // Include the desktopApp compiled classes
    val classesDirProvider = project.layout.buildDirectory.dir("classes/kotlin/main")
    inputs.dir(classesDirProvider)
    doFirst {
        val classesDir = classesDirProvider.get().asFile
        if (classesDir.exists()) {
            from(classesDir)
        }
    }

    // Include all runtime dependencies so the jar is self-contained
    from(configurations.runtimeClasspath.get().getAsFileTree().filter { it.name.endsWith(".jar") }.map { zipTree(it) }) {}
}

// ============================================================
// Option B: Thin jar (classes only) with Class-Path manifest
// Requires the runtime dependencies to be on the classpath.
// Runs with: java -cp "lib/*:desktopApp-thin.jar" com.never.localmusic.desktop.MainKt
// OR: java -jar desktopApp-thin.jar  (if lib/ is alongside the jar)
// ============================================================
tasks.register<Jar>("thinJar") {
    archiveClassifier.set("thin")
    manifest {
        attributes["Main-Class"] = "com.never.localmusic.desktop.MainKt"
        // Class-Path will be populated in doFirst based on runtimeClasspath
        attributes["Class-Path"] = ""
    }

    // Include the desktopApp compiled classes
    val classesDirProvider = project.layout.buildDirectory.dir("classes/kotlin/main")
    inputs.dir(classesDirProvider)
    doFirst {
        val classesDir = classesDirProvider.get().asFile
        if (classesDir.exists()) {
            from(classesDir)
        }

        // Build the Class-Path manifest attribute
        val runtimeClasspath = configurations.getByName("runtimeClasspath")
        val classpathEntries = runtimeClasspath.files
            .filter { it.name.endsWith(".jar") }
            .map { "lib/" + it.name }  // Assume jars will be in lib/ directory alongside the thin jar
            .joinToString(" ")

        manifest.attributes["Class-Path"] = classpathEntries
    }
}

// Copy runtime dependencies to a directory for thin jar distribution
tasks.register<Copy>("copyDependencies") {
    val destDir = project.findProperty("destDir")?.toString() ?: "build/lib"
    into(destDir)
    from(configurations.runtimeClasspath)
}
