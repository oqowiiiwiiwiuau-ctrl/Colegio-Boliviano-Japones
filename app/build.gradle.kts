plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")  // Agrega esta línea
}

android {
    namespace = "com.graficar.colegio"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.graficar.colegio"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    buildFeatures {
        viewBinding = true
    }
}
dependencies {

    implementation(libs.androidx.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.activity)

    // ============================================
    // Firebase: UN solo BoM que gestiona
    // ============================================
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))

    implementation("com.google.firebase:firebase-database")   // sin versión
    implementation("com.google.firebase:firebase-analytics")  // sin versión
    implementation("com.google.firebase:firebase-auth")       // sin versión

    // ============================================
    // Credential Manager (Google Sign-In)
    // ============================================
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // ============================================
    // Multidex
    // ============================================
    implementation("androidx.multidex:multidex:2.0.1")

    // ============================================
    // Tests
    // ============================================
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}