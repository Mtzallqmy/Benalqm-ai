import java.net.URI

plugins {
    id("com.android.application")
}

fun escapedJavaString(value: String): String =
    "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\""

val defaultOrigin = providers.gradleProperty("moatazOrigin").orElse("").get().trim().trimEnd('/')
val trustedOrigin = providers.gradleProperty("moatazTrustedOrigin")
    .orElse("https://app.moataz.ai")
    .get()
    .trim()
    .trimEnd('/')
val trustedHost = runCatching { URI(trustedOrigin).host }.getOrNull() ?: "app.moataz.ai"

android {
    namespace = "ai.moataz.app"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "ai.moataz.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 10000
        versionName = "1.0.0"

        buildConfigField("String", "DEFAULT_ORIGIN", escapedJavaString(defaultOrigin))
        buildConfigField("String", "TRUSTED_ORIGIN", escapedJavaString(trustedOrigin))
        resValue("string", "twa_default_url", "$trustedOrigin/workspace")
        resValue(
            "string",
            "asset_statements",
            "[{\"relation\":[\"delegate_permission/common.handle_all_urls\"],\"target\":{\"namespace\":\"web\",\"site\":\"$trustedOrigin\"}}]",
        )
        manifestPlaceholders["trustedHost"] = trustedHost
    }

    buildFeatures {
        buildConfig = true
        resValues = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isIncludeAndroidResources = false
    }
}

dependencies {
    implementation("androidx.activity:activity:1.10.1")
    implementation("com.google.androidbrowserhelper:androidbrowserhelper:2.7.2")
    testImplementation("junit:junit:4.13.2")
}
