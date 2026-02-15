plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.vtp.smartattendanceapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.vtp.smartattendanceapp"
        minSdk = 24
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
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // Firebase
    implementation("com.google.firebase:firebase-auth-ktx:23.2.1")
    implementation("com.google.firebase:firebase-firestore-ktx:25.1.4")
    implementation("com.google.firebase:firebase-analytics-ktx:22.5.0")

    // Location Services
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // ML Kit Barcode Scanning
    implementation("com.google.mlkit:barcode-scanning:17.3.0")

    // CameraX - use 1.3.4 (compatible with AGP 8.2.2)
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
}

//dependencies {
//    implementation("androidx.core:core-ktx:1.17.0")
//    implementation("androidx.appcompat:appcompat:1.7.1")
//    implementation("com.google.android.material:material:1.13.0")
//    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
//
//    // Use explicit versions (no BOM)
//    implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
//    implementation("com.google.firebase:firebase-firestore-ktx:24.10.3")
//    implementation("com.google.firebase:firebase-analytics-ktx:21.5.1")
//    implementation("com.google.android.gms:play-services-location:21.3.0")
//    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")
//    implementation("androidx.camera:camera-lifecycle:1.5.3")
//
//    testImplementation("junit:junit:4.13.2")
//    androidTestImplementation("androidx.test.ext:junit:1.3.0")
//    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
//}