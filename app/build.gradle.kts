plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
}

// مقدارهای SITE_URL و APP_KEY از «Secrets» گیت‌هاب خوانده می‌شوند و داخل کدها نوشته نمی‌شوند
fun cfg(name: String): String = System.getenv(name) ?: (project.findProperty(name) as String?) ?: ""

android {
    namespace = "com.khanearam.shop"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.khanearam.shop"
        minSdk = 24
        targetSdk = 35
        versionCode = 5
        versionName = "1.5.0"
        buildConfigField("String", "SITE_URL", "\"${cfg("SITE_URL")}\"")
        buildConfigField("String", "APP_KEY", "\"${cfg("APP_KEY")}\"")
    }

    signingConfigs {
        create("release") {
            val f = cfg("KEYSTORE_FILE")
            if (f.isNotEmpty()) {
                storeFile = file(f)
                storePassword = cfg("KEYSTORE_PASSWORD")
                keyAlias = cfg("KEY_ALIAS")
                keyPassword = cfg("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (cfg("KEYSTORE_FILE").isNotEmpty()) signingConfigs.getByName("release") else null
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.navigation:navigation-compose:2.8.4")
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("androidx.biometric:biometric:1.1.0")
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")
    implementation(platform("com.google.firebase:firebase-bom:33.5.1"))
    implementation("com.google.firebase:firebase-messaging")
}
