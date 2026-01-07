// Root-level build.gradle.kts for FrauduLens

buildscript {
    repositories {
        // 👇 These are required for Gradle to find the Android and Google Services plugins
        google()
        mavenCentral()
        gradlePluginPortal()
    }

    dependencies {
        classpath("com.android.tools.build:gradle:8.11.2")
        classpath("com.google.gms:google-services:4.4.0")
    }
}

// ✅ No need for 'allprojects' — deprecated in Gradle 8+
