plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.overfin.android"
    compileSdk = 37
    defaultConfig {
        applicationId = "io.github.overfin.android"
        minSdk = 24
        targetSdk = 37
        versionCode = 5
        versionName = "0.3.2"
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20260814")
}
