import org.gradle.kotlin.dsl.implementation

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
    alias(libs.plugins.google.firebase.crashlytics)
    alias(libs.plugins.google.firebase.firebase.perf)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "com.ecorvi.schmng"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ecorvi.schmng"
        minSdk = 24
        targetSdk = 35
        versionCode = 22
        versionName = "1.19"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }


    signingConfigs {
        create("release") {
            keyAlias = System.getenv("KEY_ALIAS") ?: "defaultAlias"
            keyPassword = System.getenv("KEY_PASSWORD") ?: "defaultKeyPassword"
            storeFile = file(System.getenv("KEYSTORE_PATH") ?: "KEY_STORE/schmng.jks")
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
    implementation("com.google.accompanist:accompanist-placeholder-material:0.32.0")
    implementation("com.google.accompanist:accompanist-placeholder-material:0.32.0")
    implementation("androidx.compose.material3:material3:1.3.2") // Ensure you use the latest stable version
    implementation ("com.google.android.play:app-update:2.1.0")
    implementation ("com.google.android.play:app-update-ktx:2.1.0")
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
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation("com.google.firebase:firebase-firestore-ktx:24.0.0")
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.inappmessaging.display)
    testImplementation(libs.junit)
    implementation(libs.google.play.app.update) // Keep only one instance
    implementation(libs.google.play.app.update.ktx) // Keep only one instance
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.storage)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}

