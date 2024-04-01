plugins {
    id("com.android.application")
}

android {
    namespace = "com.wpf.jiagu"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.wpf.jiagu"
        minSdk = 21
        targetSdk = 33
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation(project(":jiagulibrary"))
}