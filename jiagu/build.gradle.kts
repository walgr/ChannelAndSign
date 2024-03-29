plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.wpf.jiagu"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.wpf.jiagu"
        minSdk = 19
        targetSdk = 33
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    kotlin {
        jvmToolchain(11)
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:1.9.21")

}