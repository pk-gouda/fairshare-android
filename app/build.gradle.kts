import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
}

android {
    namespace = "com.prathik.fairshare"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.prathik.fairshare"
        minSdk = 28
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Load local.properties once — shared by both signingConfigs and buildTypes below.
    val localProps = Properties()
    val localPropsFile = rootProject.file("local.properties")
    if (localPropsFile.exists()) {
        localPropsFile.inputStream().use { localProps.load(it) }
    }

    signingConfigs {
        create("release") {
            // All values read from local.properties or CI env vars.
            // local.properties must NEVER be committed or included in shared zips.
            val storeFilePath = localProps.getProperty("RELEASE_STORE_FILE")
                ?: System.getenv("RELEASE_STORE_FILE")
            val storePass = localProps.getProperty("RELEASE_STORE_PASSWORD")
                ?: System.getenv("RELEASE_STORE_PASSWORD")
            val keyAliasVal = localProps.getProperty("RELEASE_KEY_ALIAS")
                ?: System.getenv("RELEASE_KEY_ALIAS")
            val keyPass = localProps.getProperty("RELEASE_KEY_PASSWORD")
                ?: System.getenv("RELEASE_KEY_PASSWORD")

            if (storeFilePath != null) {
                storeFile = file(storeFilePath)
            }
            storePassword = storePass ?: ""
            keyAlias = keyAliasVal ?: ""
            keyPassword = keyPass ?: ""
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            // Uses the default Android debug signing key.
            // Do NOT apply release signingConfig here — release key must never be used on debug builds.

            val baseUrl = localProps.getProperty("BASE_URL")
                ?: "https://api.fairshareapp.app/"

            buildConfigField("String", "BASE_URL", "\"$baseUrl\"")
        }

        release {
            isMinifyEnabled = true
            isDebuggable = false

            signingConfig = signingConfigs.getByName("release")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            buildConfigField("String", "BASE_URL", "\"https://api.fairshareapp.app/\"")
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
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.splashscreen)
    implementation(libs.androidx.material3)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.extended)

    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.retrofit)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.kotlinx.serialization.json)

    implementation("com.google.zxing:core:3.5.2")

    // CameraX 1.4+ fixed libimage_processing_util_jni.so 16 KB ELF alignment (broken in 1.3.4).
    implementation("androidx.camera:camera-core:1.5.0")
    implementation("androidx.camera:camera-camera2:1.5.0")
    implementation("androidx.camera:camera-lifecycle:1.5.0")
    implementation("androidx.camera:camera-view:1.5.0")

    // Switched from bundled mlkit:barcode-scanning to the GMS (unbundled) variant.
    // The bundled variant ships libbarhopper_v3.so with 4 KB ELF alignment, which
    // fails the Play Store 16 KB page-size check on API 35+ devices.
    // The GMS variant delegates to Google Play Services — no .so in the APK.
    implementation("com.google.android.gms:play-services-mlkit-barcode-scanning:18.3.1")

    implementation(libs.kotlinx.coroutines.android)
    implementation("androidx.compose.material:material-icons-extended")

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.coil.compose)

    // Upgraded from 16.0.0-beta1 (4 KB ELF alignment) to stable 16.0.0 release.
    // libimage_processing_util_jni.so in beta1 had 4 KB alignment; stable is 16 KB-compatible.
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0")

    implementation(libs.androidx.security.crypto)
    implementation(libs.androidx.biometric)

    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.hilt.work)
    ksp(libs.hilt.work.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}