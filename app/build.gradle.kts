plugins {
    // Use the standard plugin declaration (not alias)
    id("com.android.application")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.fraudulens"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.fraudulens"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "CLOUD_SCAM_ENDPOINT",
            "\"https://asia-southeast1-fraudulense.cloudfunctions.net/scoreScamText\""
        )
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
        }
    }
}

dependencies {
    // ✅ Core Android libraries
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.9.3")
    implementation("androidx.constraintlayout:constraintlayout:2.2.0")

    // ✅ UI & Image Loading
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("com.squareup.picasso:picasso:2.8")

    // ✅ Firebase (BoM handles versioning automatically)
    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-firestore")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-storage")
    implementation("com.google.firebase:firebase-messaging")
    
    // ✅ Google Sign-In
    implementation("com.google.android.gms:play-services-auth:21.2.0")
    
    // ✅ Facebook Login
    implementation("com.facebook.android:facebook-login:17.0.0")

    // ✅ On-device ML (TensorFlow Lite)
    implementation("org.tensorflow:tensorflow-lite:2.16.1")

    // ✅ On-device OCR (ML Kit)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // ✅ In-app image cropper
    implementation("com.github.yalantis:ucrop:2.2.8")

    // ✅ Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
