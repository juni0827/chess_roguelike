// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

layout.buildDirectory.set(rootProject.layout.projectDirectory.dir("artifacts/build/root"))

subprojects {
    val projectBuildPath = project.path.trimStart(':').replace(':', '/')
    layout.buildDirectory.set(rootProject.layout.projectDirectory.dir("artifacts/build/$projectBuildPath"))
}
