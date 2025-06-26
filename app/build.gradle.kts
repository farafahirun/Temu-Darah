plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    buildFeatures {
        viewBinding = true
    }
    namespace = "com.example.temudarah"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.temudarah"
        minSdk = 26
        targetSdk = 34
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.circleimageview)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.messaging)
    implementation(libs.glide)
    implementation(libs.glide)
    implementation(platform(libs.firebase.bom))
    implementation(libs.google.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation (libs.google.firebase.firestore)
    implementation(libs.play.services.location)
    implementation(libs.geofire.android.common)
    implementation(libs.google.firebase.messaging)
}