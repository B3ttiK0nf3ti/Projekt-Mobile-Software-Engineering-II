plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.iu.boardgamerapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.iu.boardgamerapp"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation("androidx.compose.material:material-icons-core:1.7.2")
    implementation("androidx.compose.material:material-icons-extended:1.7.2")
    implementation("androidx.compose.runtime:runtime-livedata:1.7.2") // oder die neueste Version
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.6")  // oder die neueste Version
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.accompanist:accompanist-swiperefresh:0.30.1")
    implementation(libs.firebase.firestore.ktx)
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
    implementation(libs.generativeai)
    implementation(libs.androidx.navigation.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.foundation.android)  // oder die neueste Version
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
    implementation(libs.androidx.appcompat)
    implementation("androidx.appcompat:appcompat:1.6.1") // Aktuelle Version verwenden
    implementation("androidx.compose.ui:ui:1.4.0") // Beispiel für Compose
    implementation("androidx.activity:activity-compose:1.7.0")
    implementation(libs.firebase.database.ktx)
    implementation(libs.androidx.foundation.layout.android)
    implementation(libs.androidx.foundation.layout.android)
    implementation(libs.androidx.ui.test.android) // Beispiel für Activity Compose
    implementation("com.google.firebase:firebase-firestore:24.0.0")
    implementation(libs.androidx.foundation.android)
    implementation(libs.androidx.work.runtime.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}


