import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services")
    id("kotlin-parcelize")
    id("androidx.navigation.safeargs.kotlin")
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.example.fitlifesmarthealthlifestyleapp"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fitlifesmarthealthlifestyleapp"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "CLOUD_NAME", "\"${localProperties.getProperty("CLOUDINARY_CLOUD_NAME")}\"")
        buildConfigField("String", "API_KEY", "\"${localProperties.getProperty("CLOUDINARY_API_KEY")}\"")
        buildConfigField("String", "API_SECRET", "\"${localProperties.getProperty("CLOUDINARY_API_SECRET")}\"")
        buildConfigField("String", "API_KEY_GEMINI", "\"${localProperties.getProperty("API_KEY_GEMINI")}\"")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.googleid)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.generativeai)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:34.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")

    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
    implementation("com.google.android.gms:play-services-auth:21.0.0")

    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("com.cloudinary:cloudinary-android:2.5.0")
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    implementation("com.google.android.gms:play-services-location:21.1.0")
    implementation("androidx.lifecycle:lifecycle-service:2.7.0")
    implementation("org.osmdroid:osmdroid-android:6.1.17")
}