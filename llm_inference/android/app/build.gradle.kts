import java.util.Properties
        import org.gradle.api.JavaVersion

        plugins {
            id("com.android.application")
            id("org.jetbrains.kotlin.android")
            id("kotlin-kapt")
            id("com.google.gms.google-services")
        }

android {


    namespace = "com.google.mediapipe.examples.llminference"
    compileSdk = 34

    useLibrary("org.apache.http.legacy")



    defaultConfig {
        applicationId = "com.google.mediapipe.examples.llminference"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        manifestPlaceholders["appAuthRedirectScheme"] = ""

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use { properties.load(it) }
        }
        val hfAccessToken = properties.getProperty("HF_ACCESS_TOKEN", "")
        buildConfigField("String", "HF_ACCESS_TOKEN", "\"$hfAccessToken\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.9"
    }

    packaging {
        resources {
            excludes += "LICENSE-EDL-1.0.txt"
            excludes += "LICENSE-EPL-1.0.txt"
            excludes += "META-INF/DEPENDENCIES"
            excludes += "META-INF/LICENSE*"
            excludes += "META-INF/NOTICE*"
        }
    }


}


dependencies {

    // AndroidX Core and Lifecycle
    implementation("androidx.core:core-ktx:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.02.00")) // Or latest version
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("com.google.android.material:material:1.12.0")

    // MediaPipe
    implementation("com.google.mediapipe:tasks-genai:0.10.21") // Keep exclusion if duplicate class error persists AFTER removing firebase-admin
    // { exclude group: 'com.google.api.grpc', module: 'proto-google-common-protos' } // Add this exclusion if needed

    // Networking & Auth
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("net.openid:appauth:0.11.1")
    implementation("androidx.security:security-crypto:1.0.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // Firebase (using BOM)
    implementation(platform("com.google.firebase:firebase-bom:33.1.2"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-analytics")
// Kotlin coroutines for Android (includes core + Android dispatcher)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // Other Libraries
    implementation("com.slack.api:bolt:1.20.1") // Check compatibility if issues arise
    implementation("com.google.code.gson:gson:2.10.1") // Updated
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:1.9.22"))

    // Coroutines (likely included transitively, add explicitly if needed)
    // implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0") // Example if needed

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5") // Consider 1.2.1
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // Consider 3.6.1
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00")) // Align with main Compose BOM
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")


    androidTestImplementation ("androidx.test:runner:1.5.2")

    // If you are using Kotlin coroutines in tests
    androidTestImplementation ("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.6.4")

    // Room Testing library (optional, but helpful for testing Room databases)
    androidTestImplementation ("androidx.room:room-testing:2.5.2")

    // If using JUnit 4 assertions
    testImplementation ("junit:junit:4.13.2")
}

configurations.all {
    exclude(group = "org.mapdb", module = "mapdb")
}

