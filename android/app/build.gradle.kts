import java.io.FileInputStream
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

// Load keystore properties if available
val keystorePropertiesFile: File = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.rockyriverapps.tabletorch"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.rockyriverapps.tabletorch"
        minSdk = 24
        targetSdk = 35
        // Versioning strategy:
        // - versionCode: increment by 1 for every Play Store release (monotonically increasing)
        // - versionName: use semantic versioning (MAJOR.MINOR.PATCH)
        //   MAJOR = breaking / redesign, MINOR = new features, PATCH = bug fixes
        // CI/CD can automate versionCode via: (MAJOR*10000 + MINOR*100 + PATCH)
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Locale filtering (replaces deprecated resourceConfigurations)
    androidResources {
        localeFilters += listOf("en", "es")
    }

    signingConfigs {
        // Debug signing config uses default debug keystore
        getByName("debug") {
            // Uses default debug keystore
        }

        // Release signing config - requires keystore.properties file
        create("release") {
            if (keystorePropertiesFile.exists()) {
                keyAlias = keystoreProperties.getProperty("keyAlias")
                    ?: error("keyAlias missing in keystore.properties")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                    ?: error("keyPassword missing in keystore.properties")
                storeFile = keystoreProperties.getProperty("storeFile")?.let { file(it) }
                    ?: error("storeFile missing in keystore.properties")
                storePassword = keystoreProperties.getProperty("storePassword")
                    ?: error("storePassword missing in keystore.properties")
            }
        }
    }

    buildTypes {
        debug {
            isDebuggable = true
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }

        release {
            isDebuggable = false
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
            // Only sign release builds when keystore is configured
            signingConfig = if (keystorePropertiesFile.exists()) {
                signingConfigs.getByName("release")
            } else {
                null // Release builds without keystore will fail at signing step
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    buildToolsVersion = "36.1.0"
}

// Kotlin compiler options (replaces deprecated kotlinOptions block)
kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.collections.immutable)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
