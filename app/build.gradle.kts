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
        targetSdk = 33
        versionCode = 2
        versionName = "2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
    }

    signingConfigs {
        create("release") {
            keyAlias = System.getenv("KEY_ALIAS") ?: "defaultAlias"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "defaultKeyPassword"
            // Ensure the keystore file path is correct, based on the GitHub Actions decode step
            storeFile = file("app/KEY_STORE/schmng.jks")
            storePassword = System.getenv("STORE_PASSWORD") ?: "defaultStorePassword"
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
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
        viewBinding = true
    }
}

dependencies {
    // Core dependencies
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase dependencies
    implementation(libs.firebase.auth)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.database)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.perf)
    implementation(libs.firebase.messaging)

    // Unit testing dependencies
    testImplementation(libs.junit) // JUnit for unit testing
    testImplementation("org.mockito:mockito-core:5.5.0") // Mockito for mocking dependencies

    // Android instrumentation testing dependencies
    androidTestImplementation(libs.androidx.junit) // AndroidX JUnit extensions
    androidTestImplementation(libs.androidx.espresso.core) // Espresso for UI testing
    androidTestImplementation("androidx.test:rules:1.5.0") // Test rules for Android tests
    androidTestImplementation("androidx.test:runner:1.5.0") // Test runner for Android tests
}
