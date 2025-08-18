plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("org.jetbrains.kotlin.plugin.serialization")

}

android {
    namespace = "com.cehpoint.netwin"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.cehpoint.netwin"
        minSdk = 28
        targetSdkVersion(rootProject.extra["defaultTargetSdkVersion"] as Int)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        debug {
            // Debug configuration
        }
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
    buildToolsVersion = rootProject.extra["buildToolsVersion"] as String
}

// Add Room schema directory configuration


dependencies {
    // Core Android
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    
    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation("androidx.compose.material:material:1.8.3")
    implementation("androidx.compose.material3:material3:1.3.2")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose")
    implementation("androidx.navigation:navigation-compose")
    
    // Navigation Serialization
    implementation("androidx.navigation:navigation-compose:2.9.1")
    implementation("androidx.navigation:navigation-runtime-ktx:2.9.1")
    implementation("androidx.navigation:navigation-common-ktx:2.9.1")
    implementation("androidx.navigation:navigation-ui-ktx:2.9.1")
    
    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.9.0")
    
    // Hilt
    implementation("com.google.dagger:hilt-android:2.56.2")
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage.ktx)
    implementation("com.google.firebase:firebase-appcheck-playintegrity:18.0.0")
    implementation("com.google.firebase:firebase-appcheck-debug:18.0.0")
    ksp("com.google.dagger:hilt-android-compiler:2.56.2")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")
    
//    // Firebase
//    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
//    implementation("com.google.firebase:firebase-auth-ktx")
//    implementation("com.google.firebase:firebase-firestore-ktx")
//    implementation("com.google.firebase:firebase-storage-ktx")
//    implementation("com.google.firebase:firebase-messaging-ktx")
//    implementation("com.google.firebase:firebase-analytics")
//
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
    
    
    
    // Coil for image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Datastore preference
    implementation ("androidx.datastore:datastore-preferences:1.1.7")
    
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)


    // Hilt testing
    androidTestImplementation("com.google.dagger:hilt-android-testing:2.56.2")
    kspAndroidTest("com.google.dagger:hilt-compiler:2.56.2")
    // CameraX core library using the camera2 implementation
    val camerax_version = "1.5.0-alpha01"
    // The following line is optional, as the core library is included indirectly by camera-camera2
    implementation("androidx.camera:camera-core:${camerax_version}")
    implementation("androidx.camera:camera-camera2:${camerax_version}")
    // If you want to additionally use the CameraX Lifecycle library
    implementation("androidx.camera:camera-lifecycle:${camerax_version}")
    // If you want to additionally use the CameraX View class
    implementation("androidx.camera:camera-view:${camerax_version}")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.8.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.8.2")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")
    
    // Android instrumentation test dependencies
    androidTestImplementation("com.google.truth:truth:1.1.3")
    androidTestImplementation("io.mockk:mockk-android:1.13.4")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")

    implementation("androidx.core:core-splashscreen:1.0.1")

    implementation("com.google.accompanist:accompanist-swiperefresh:0.36.0")

}