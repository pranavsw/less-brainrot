plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "dev.lessbrainrot"
    compileSdk = 36

    defaultConfig {
        applicationId = "dev.lessbrainrot"
        minSdk = 29
        targetSdk = 35
        versionCode = 1
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            // Local testing signs with the debug key; a release keystore is only
            // needed for store distribution (see README).
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    splits {
        abi {
            // One lean APK for real devices; store distribution should use an
            // App Bundle, which delivers per-device ABIs automatically.
            isEnable = true
            reset()
            include("arm64-v8a")
            isUniversalApk = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.13.0")
    debugImplementation("dev.lessbrainrot.scroll_aware_ui:flutter_debug:1.0")
    releaseImplementation("dev.lessbrainrot.scroll_aware_ui:flutter_release:1.0")
    testImplementation("junit:junit:4.13.2")
}
