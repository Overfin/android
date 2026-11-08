import java.util.Properties

plugins {
    id("com.android.application")
}

// Releases are signed with the maintainer's key, kept outside the repository:
// a properties file (storeFile, password, alias) named by the
// overfin.signing property, else ~/.config/overfin-android/release.properties.
// Without it, release builds are left unsigned; debug builds are unchanged.
val signing = Properties().apply {
    val file = file((findProperty("overfin.signing") as String?)
        ?: "${System.getProperty("user.home")}/.config/overfin-android/release.properties")
    if (file.isFile) file.inputStream().use { load(it) }
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
    signingConfigs {
        if (signing.containsKey("storeFile")) {
            create("release") {
                storeFile = file(signing.getProperty("storeFile"))
                storePassword = signing.getProperty("password")
                keyAlias = signing.getProperty("alias")
                keyPassword = signing.getProperty("password")
            }
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
        }
    }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20250517")
}
