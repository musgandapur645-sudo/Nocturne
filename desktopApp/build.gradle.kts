import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.gradle.api.tasks.bundling.Jar

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
            targetFormats(TargetFormat.Msi, TargetFormat.Exe, TargetFormat.Deb, TargetFormat.AppImage)
            packageName = "nocturne"
            packageVersion = "0.1.0"
        }
    }
}

// Fat jar: self-contained, includes all runtime dependencies.
// Run with: java -jar desktopApp-runnable.jar
tasks.register<Jar>("runnableJar") {
    archiveClassifier.set("runnable")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "com.never.localmusic.desktop.MainKt"
    }

    dependsOn(tasks.named("compileKotlin"))
    from(sourceSets.main.get().output)
    from(configurations.runtimeClasspath.get().getAsFileTree().filter { it.name.endsWith(".jar") }.map { zipTree(it) }) {}
}

// Thin jar: classes only, with Class-Path manifest pointing at lib/*.jar.
// Run with: java -cp "lib/*:desktopApp-thin.jar" com.never.localmusic.desktop.MainKt
// Or: java -jar desktopApp-thin.jar  (when lib/ is alongside the jar)
tasks.register<Jar>("thinJar") {
    archiveClassifier.set("thin")
    manifest {
        attributes["Main-Class"] = "com.never.localmusic.desktop.MainKt"
        attributes["Class-Path"] = ""
    }

    dependsOn(tasks.named("compileKotlin"))
    from(sourceSets.main.get().output)

    doFirst {
        val runtimeClasspath = configurations.getByName("runtimeClasspath")
        val classpathEntries = runtimeClasspath.files
            .filter { it.name.endsWith(".jar") }
            .map { "lib/" + it.name }
            .joinToString(" ")

        manifest.attributes["Class-Path"] = classpathEntries
    }
}

// Distributable layout under build/dist:
//   desktopApp-thin.jar + lib/*.jar
tasks.register<Copy>("assembleDistributive") {
    dependsOn(tasks.named("thinJar"))
    from(tasks.named("thinJar"))
    from(configurations.runtimeClasspath) {
        into("lib")
    }
    into(project.layout.buildDirectory.dir("dist"))
}

// Convenience task: build nocturne-thin-dist.zip in the project root.
// The zip unpacks to a nocturne/ folder: nocturne/desktopApp-thin.jar + nocturne/lib/*.jar
// Run with: ./gradlew :desktopApp:packageDistributive
tasks.register<Zip>("packageDistributive") {
    dependsOn(tasks.named("assembleDistributive"))
    from(project.layout.buildDirectory.dir("dist"))
    into("nocturne")
    archiveFileName.set("nocturne-thin-dist.zip")
    destinationDirectory.set(project.layout.projectDirectory)
}
