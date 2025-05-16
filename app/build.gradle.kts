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
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ecorvi.schmng"
        minSdk = 24
        targetSdk = 34
        versionCode = 37
        versionName = "1.34"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Add manifest placeholders properly
        manifestPlaceholders["android.max.aspect"] = "2.4"

        // Declare required features
        ndk {
            abiFilters.add("armeabi-v7a")
            abiFilters.add("arm64-v8a")
            abiFilters.add("x86")
            abiFilters.add("x86_64")
        }
    }

    // Add packaging options
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
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
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    // Add core library desugaring for compatibility with older Android versions
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("com.google.accompanist:accompanist-placeholder-material:0.32.0")
    implementation("com.google.accompanist:accompanist-placeholder-material:0.32.0")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("com.google.android.play:app-update:2.1.0")
    implementation("com.google.android.play:app-update-ktx:2.1.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    
    // Add window size compatibility
    implementation("androidx.window:window:1.3.0")
    implementation("androidx.window:window-core:1.3.0")
    
    // Update lifecycle components versions
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.9.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.0")
    
    // Update navigation version
    implementation("androidx.navigation:navigation-compose:2.7.7")
    
    // Update activity compose version
    implementation("androidx.activity:activity-compose:1.8.2")
    
    // Import the Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    
    // Firebase dependencies (don't specify versions when using BoM)
    implementation("com.google.firebase:firebase-analytics-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-inappmessaging-display-ktx")
    implementation("com.google.firebase:firebase-crashlytics-ktx")
    implementation("com.google.firebase:firebase-perf-ktx")
    implementation("com.google.firebase:firebase-database-ktx")

    testImplementation(libs.junit)
    implementation(libs.google.play.app.update)
    implementation(libs.google.play.app.update.ktx)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material:material-icons-extended:1.7.8")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.accompanist:accompanist-permissions:0.34.0")
    implementation("io.coil-kt:coil-compose:2.6.0")
    implementation("com.google.accompanist:accompanist-systemuicontroller:0.34.0")
    implementation(libs.androidx.media3.effect)
    implementation(libs.androidx.storage)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.navigation.runtime.android)
    implementation(libs.androidx.navigation.compose)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    implementation("androidx.compose.runtime:runtime-livedata:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-auth-api-phone:18.0.2")
}

