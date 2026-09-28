import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.example.ai_based_medical_chatbot"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    // =========================================================
    // LOCAL API URL
    // =========================================================

    val localProperties = Properties()

    val localPropertiesFile =
        rootProject.file("local.properties")

    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use {
            localProperties.load(it)
        }
    }

    val medassistApiUrl =
        localProperties.getProperty(
            "MEDASSIST_API_URL",
            "http://10.0.2.2:5000/"
        )

    defaultConfig {
        applicationId = "com.example.ai_based_medical_chatbot"

        minSdk = 24
        targetSdk = 36

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "MEDASSIST_API_URL",
            "\"$medassistApiUrl\""
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {

    // =========================================================
    // ANDROID
    // =========================================================

    implementation(libs.androidx.core.ktx)

    implementation(
        libs.androidx.lifecycle.runtime.ktx
    )

    implementation(
        libs.androidx.activity.compose
    )

    implementation(
        "com.squareup.okhttp3:okhttp:4.12.0"
    )


    // =========================================================
    // BIOMETRIC / DEVICE AUTHENTICATION
    // =========================================================

    implementation(
        "androidx.biometric:biometric:1.1.0"
    )


    // =========================================================
    // COMPOSE
    // =========================================================

    implementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    implementation(
        libs.androidx.compose.ui
    )

    implementation(
        libs.androidx.compose.ui.graphics
    )

    implementation(
        libs.androidx.compose.ui.tooling.preview
    )

    implementation(
        libs.androidx.compose.material3
    )


    // =========================================================
    // SUPABASE
    // =========================================================

    implementation(
        platform(
            "io.github.jan-tennert.supabase:bom:3.2.0"
        )
    )

    implementation(
        "io.github.jan-tennert.supabase:auth-kt"
    )

    implementation(
        "io.github.jan-tennert.supabase:postgrest-kt"
    )

    implementation(
        "io.github.jan-tennert.supabase:storage-kt"
    )


    // =========================================================
    // KTOR
    // =========================================================

    implementation(
        "io.ktor:ktor-client-android:3.2.1"
    )

    implementation(
        "io.ktor:ktor-client-core:3.2.1"
    )


    // =========================================================
    // GOOGLE ML KIT - LANGUAGE IDENTIFICATION
    // =========================================================

    implementation(
        "com.google.mlkit:language-id:17.0.6"
    )


    // =========================================================
    // RETROFIT
    // =========================================================

    implementation(
        "com.squareup.retrofit2:retrofit:3.0.0"
    )

    implementation(
        "com.squareup.retrofit2:converter-gson:3.0.0"
    )


    // =========================================================
    // GOOGLE ML KIT - TEXT RECOGNITION / OCR
    // =========================================================

    implementation(
        "com.google.mlkit:text-recognition:16.0.1"
    )

    implementation(
        "com.google.mlkit:translate:17.0.3"
    )


    // =========================================================
    // TESTS
    // =========================================================

    testImplementation(
        libs.junit
    )

    androidTestImplementation(
        libs.androidx.junit
    )

    androidTestImplementation(
        libs.androidx.espresso.core
    )

    androidTestImplementation(
        platform(
            libs.androidx.compose.bom
        )
    )

    androidTestImplementation(
        libs.androidx.compose.ui.test.junit4
    )


    // =========================================================
    // DEBUG
    // =========================================================

    debugImplementation(
        libs.androidx.compose.ui.tooling
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )
}