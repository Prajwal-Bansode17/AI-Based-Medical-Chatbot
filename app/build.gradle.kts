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

    // Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Compose
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

    // Supabase
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

    // Ktor
    implementation(
        "io.ktor:ktor-client-android:3.2.1"
    )

    implementation(
        "io.ktor:ktor-client-core:3.2.1"
    )


    // Google ML Kit - Language Identification
    implementation(
        "com.google.mlkit:language-id:17.0.6"
    )

    // Retrofit
    implementation(
        "com.squareup.retrofit2:retrofit:3.0.0"
    )

    implementation(
        "com.squareup.retrofit2:converter-gson:3.0.0"
    )

    // Tests
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

    // Debug
    debugImplementation(
        libs.androidx.compose.ui.tooling
    )

    debugImplementation(
        libs.androidx.compose.ui.test.manifest
    )
}