plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core-game"))
    implementation(project(":content-io"))
}

application {
    mainClass = "com.chessroguelike.desktop.DesktopAppKt"
}
