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
        // Optional: Add testInstrumentationRunnerArguments if needed
        // testInstrumentationRunnerArguments = [key: "value"]
    }

    signingConfigs {
        create("release") {
            keyAlias = "key0" // Replace with your key alias
            keyPassword = "955326" // Replace with your key password
            storeFile = file("C:\\Users\\Yeswanth\\OneDrive\\Desktop\\schmng\\schmng.kts") // Path to your keystore file
            storePassword = "955326" // Replace with your store password
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Enable code shrinking for release builds
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release") // Set the signing config for release builds
        }
    }

    compileOptions {
        // Updated to support Java 21
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        // Set JVM target to match Java 21
        jvmTarget = "21"
    }

    buildFeatures {
        viewBinding = true // Enable View Binding if used
    }
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
    // Optional: Add Kotlin Coroutines if needed
    // implementation(libs.kotlinx.coroutines.core)
    // implementation(libs.kotlinx.coroutines.android)
}