plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.chibicompanion"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.chibicompanion"
        minSdk = 26
        targetSdk = 35
        versionCode = 17
        versionName = "0.9.4"
    }

    signingConfigs {
        create("release") {
            val storePath = System.getenv("CHIBI_STORE_FILE")
            if (!storePath.isNullOrBlank()) {
                storeFile = file(storePath)
                storePassword = System.getenv("CHIBI_STORE_PASSWORD")
                keyAlias = System.getenv("CHIBI_KEY_ALIAS")
                keyPassword = System.getenv("CHIBI_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        getByName("release") {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}
