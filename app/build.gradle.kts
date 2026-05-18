import java.text.SimpleDateFormat
import java.util.Date
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(keystorePropertiesFile.inputStream())
}

android {
    namespace = "orinasa.njarasoa.maripanatokana"
    compileSdk = 36

    defaultConfig {
        applicationId = "orinasa.njarasoa.maripanatokana"
        minSdk = 24
        targetSdk = 36
        versionCode = 30
        versionName = "1.0.29"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        val gitHash = providers.exec { commandLine("git", "rev-parse", "--short", "HEAD") }.standardOutput.asText.get().trim()
        buildConfigField("String", "GIT_HASH", ""$gitHash"")
    }
    
    signingConfigs {
        create("release") {
            storeFile = if (System.getenv("KEYSTORE_FILE") != null) file(System.getenv("KEYSTORE_FILE"))
            else rootProject.file(keystoreProperties.getProperty("storeFile", "release.keystore"))
            storePassword = System.getenv("KEYSTORE_PASSWORD") ?: keystoreProperties.getProperty("storePassword")
            keyAlias = System.getenv("KEY_ALIAS") ?: keystoreProperties.getProperty("keyAlias")
            keyPassword = System.getenv("KEY_PASSWORD") ?: keystoreProperties.getProperty("keyPassword")
        }
    }

    flavorDimensions.add("distribution")
    productFlavors {
        create("standard") {
            dimension = "distribution"
            buildConfigField("String", "BUILD_TIME", ""${SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date())}"")
        }
        create("fdroid") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION", ""fdroid"")
            buildConfigField("String", "BUILD_TIME", ""reproducible"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isCrunchPngs = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
        }
    }
    
    packaging {
        resources.excludes.add("/META-INF/{AL2.0,LGPL2.1}")
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.retrofit)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization)
    implementation(libs.androidx.work.runtime.ktx)
    "standardImplementation"(libs.play.services.location)
    "standardImplementation"(libs.accompanist.permissions)
    "standardImplementation"(libs.kotlinx.coroutines.play.services)
    testImplementation("io.mockk:mockk:1.13.12")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
