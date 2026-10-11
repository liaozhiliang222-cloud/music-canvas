// 仅用于 QA：发布标准 MediaSession 的假播放器，不随正式包发布。
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.musiccanvas.qa"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.musiccanvas.qa"
        minSdk = 30
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }
    flavorDimensions += "source"
    productFlavors {
        create("alpha") { dimension = "source"; applicationIdSuffix = ".alpha"; resValue("string", "src_name", "QA 播放器 A") }
        create("beta") { dimension = "source"; applicationIdSuffix = ".beta"; resValue("string", "src_name", "QA 播放器 B") }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}
