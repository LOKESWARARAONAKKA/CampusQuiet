plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")

    // ✅ REQUIRED for Kotlin 2.0 + Compose
    id("org.jetbrains.kotlin.plugin.compose")

    // ✅ Firebase / Google services
    id("com.google.gms.google-services")
}

android {
    namespace = "com.lokesh.campusquiet"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.lokesh.campusquiet"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true       // for future Compose screens
        viewBinding = true   // for your current XML screens
    }

    // ❌ REMOVE composeOptions (NOT needed in Kotlin 2.0)

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {

    // -------- Core Android --------
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // -------- Compose (kept minimal & safe) --------
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation(platform("androidx.compose:compose-bom:2024.09.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")

    debugImplementation("androidx.compose.ui:ui-tooling")

    // -------- Firebase (LOGIN + ATTENDANCE BASE) --------
    implementation(platform("com.google.firebase:firebase-bom:33.6.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
}
