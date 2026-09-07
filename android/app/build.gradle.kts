plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

val defaultOrigin = providers.gradleProperty("moatazOrigin")
    .orElse("")
    .get()
    .trim()
    .trimEnd('/')

android {
    namespace = "ai.moataz.app"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "ai.moataz.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 11000
        versionName = "1.1.0"

        buildConfigField("String", "DEFAULT_ORIGIN", "\"${defaultOrigin.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "UPSTREAM_PROJECT", "\"DeerFlow\"")
        buildConfigField("String", "UPSTREAM_REPOSITORY", "\"https://github.com/bytedance/deer-flow\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
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

kotlin {
    jvmToolchain(17)
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2026.06.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.13.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.10.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    val okhttpBom = platform("com.squareup.okhttp3:okhttp-bom:5.5.0")
    implementation(okhttpBom)
    implementation("com.squareup.okhttp3:okhttp")
    debugImplementation("com.squareup.okhttp3:logging-interceptor")

    debugImplementation("androidx.compose.ui:ui-tooling")
    testImplementation("junit:junit:4.13.2")
}
