plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.chessroguelike"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.chessroguelike"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(project(":core-game"))
    implementation(project(":content-io"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

val debugApkSource = layout.buildDirectory.file("outputs/apk/debug/app-debug.apk")
val debugApkTarget = rootProject.layout.projectDirectory.file("chess-roguelike-debug.apk")
val releaseApkSource = layout.buildDirectory.file("outputs/apk/release/app-release.apk")
val releaseApkTarget = rootProject.layout.projectDirectory.file("chess-roguelike-release.apk")

val exportDebugApkToRoot by tasks.registering {
    inputs.file(debugApkSource)
    outputs.file(debugApkTarget)
    doLast {
        debugApkTarget.asFile.parentFile.mkdirs()
        debugApkSource.get().asFile.copyTo(debugApkTarget.asFile, overwrite = true)
    }
}

val exportReleaseApkToRoot by tasks.registering {
    inputs.file(releaseApkSource)
    outputs.file(releaseApkTarget)
    doLast {
        releaseApkTarget.asFile.parentFile.mkdirs()
        releaseApkSource.get().asFile.copyTo(releaseApkTarget.asFile, overwrite = true)
    }
}

afterEvaluate {
    tasks.named("assembleDebug") {
        finalizedBy(exportDebugApkToRoot)
    }

    tasks.named("assembleRelease") {
        finalizedBy(exportReleaseApkToRoot)
    }
}
