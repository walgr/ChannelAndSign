plugins {
    id("com.android.application")
}

android {
    namespace = "com.wpf.jiagu.demo"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.wpf.jiagu"
        minSdk = 19
        targetSdk = 33
        versionCode = 1
        versionName = "1.0.0"

        ndk {
            this.abiFilters.add("armeabi")
            this.abiFilters.add("armeabi-v7a")
            this.abiFilters.add("arm64-v8a")
        }
    }

    signingConfigs {
        getByName("debug") {
            storeFile = file("../jiagu.jks")
            storePassword = "abc12345678"
            keyAlias = "jiagu"
            keyPassword = "abc12345678"
            enableV1Signing = true
            enableV2Signing = true
        }
        create("release") {
            storeFile = file("../jiagu.jks")
            storePassword = "abc12345678"
            keyAlias = "jiagu"
            keyPassword = "abc12345678"
            enableV1Signing = true
            enableV2Signing = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    lint.checkReleaseBuilds = false
    lint.abortOnError = false
}

dependencies {
    implementation("androidx.annotation:annotation:1.7.0")
    implementation(project(":jiagulibrary"))
}