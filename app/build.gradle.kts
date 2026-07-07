plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.local.motioncue"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.local.motioncue"
        minSdk = 26
        targetSdk = 33
        versionCode = 1
        versionName = "0.1.0"
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
}
