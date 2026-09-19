import org.gradle.api.tasks.testing.Test

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    kotlin("kapt")
}

tasks.withType<Test>().configureEach {
    failFast = true
}

// The release workflow computes the version from the latest git tag and passes it
// in; F-Droid passes it as a Gradle property. A local build with neither just gets
// 0.0.0 and never pretends otherwise.
val appVersionName: String = providers.gradleProperty("VERSION_NAME").orNull?.takeIf { it.isNotBlank() }
    ?: System.getenv("VERSION_NAME")?.takeIf { it.isNotBlank() }
    ?: "0.0.0"
val semver = appVersionName.split(".")
val vMajor = semver.getOrNull(0)?.toIntOrNull() ?: 0
val vMinor = semver.getOrNull(1)?.toIntOrNull() ?: 0
val vPatch = semver.getOrNull(2)?.toIntOrNull() ?: 0
// No "+ 1" here, unlike weather-android's scheme: this app's versionCode was a
// small literal (7) before this change, and major*1_000_000 alone already lands
// the next release (1.0.7 -> 1000007) safely above it. coerceAtLeast(1) only
// matters for the unset-everything default (0.0.0 -> 0): AGP 8.13.2 validates
// defaultConfig.versionCode at configuration time, so a 0 fails every task,
// not just packaging -- this floor keeps a plain local build usable.
val appVersionCode: Int = (vMajor * 1_000_000 + vMinor * 1_000 + vPatch).coerceAtLeast(1)

// Signing material only ever arrives through the environment. A missing keystore
// is not an error — it just means this is a local build, which stays unsigned.
val keystorePath = System.getenv("KEYSTORE_PATH")?.takeIf { it.isNotBlank() }
val keystorePassword = System.getenv("KEYSTORE_PASSWORD")?.takeIf { it.isNotBlank() }
val keyAliasEnv = System.getenv("KEY_ALIAS")?.takeIf { it.isNotBlank() }
val keyPasswordEnv = System.getenv("KEY_PASSWORD")?.takeIf { it.isNotBlank() }
val keystoreFile = keystorePath?.let { rootProject.file(it).absoluteFile }?.takeIf { it.isFile }
val hasSigningConfig = keystoreFile != null && keystorePassword != null &&
    keyAliasEnv != null && keyPasswordEnv != null

android {
    namespace = "com.cocode.linkqrwallet"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.cocode.linkqrwallet"
        minSdk = 24
        targetSdk = 36
        versionCode = appVersionCode
        versionName = appVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasSigningConfig) {
            create("release") {
                storeFile = keystoreFile
                storePassword = keystorePassword
                keyAlias = keyAliasEnv
                keyPassword = keyPasswordEnv
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        languageVersion = "1.9"
        allWarningsAsErrors = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    lint {
        baseline = file("lint-baseline.xml")
    }

    // AGP otherwise adds a Google-encrypted dependency list to the APK signing
    // block, and F-Droid rejects any release APK that carries it.
    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.runtime.saveable)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    kapt(libs.androidx.room.compiler)
    implementation(libs.jsoup)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.zxing.core)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
