import java.util.Base64
import java.io.File

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
    alias(libs.plugins.google.firebase.firebase.perf)
}

android {
    namespace = "com.ecorvi.schmng"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ecorvi.schmng"
        minSdk = 24
        targetSdk = 33 // Updated to match compileSdk
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            keyAlias = System.getenv("KEY_ALIAS") ?: "defaultAlias" // Use environment variable or default
            keyPassword = System.getenv("KEY_PASSWORD") ?: "defaultKeyPassword" // Use environment variable or default
            storeFile = file(decodeBase64(System.getenv("KEYSTORE_FILE"))) // Decode the base64 content
            storePassword = System.getenv("STORE_PASSWORD") ?: "defaultStorePassword" // Use environment variable or default
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Enable code shrinking for release builds
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release") // Set the signing config
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        viewBinding = true // Enable View Binding if used
    }
}

// Function to decode base64 encoded keystore file
fun decodeBase64(encoded: String): String {
    val decodedBytes = Base64.getDecoder().decode(encoded)
    val tempFile = File.createTempFile("keystore",".jks")
    tempFile.writeBytes(decodedBytes)
    return tempFile.absolutePath
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.database)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.messaging)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
