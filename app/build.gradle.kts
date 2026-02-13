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

val buildTime: String = SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date())

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) load(keystorePropertiesFile.inputStream())
}

// Generate Android strings.xml from canonical JSON before resource merging
val generateStrings by tasks.registering(Exec::class) {
    workingDir = rootProject.projectDir
    commandLine("sh", "-c", "node shared/i18n/generate-android-strings.js")
}
tasks.configureEach {
    if (name.startsWith("merge") && name.endsWith("Resources")) {
        dependsOn(generateStrings)
    }
}

android {
    namespace = "orinasa.njarasoa.maripanatokana"
    compileSdk {
        version = release(36)
    }

    if (keystorePropertiesFile.exists()) {
        signingConfigs {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    defaultConfig {
        applicationId = "orinasa.njarasoa.maripanatokana"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    flavorDimensions.add("distribution")
    productFlavors {
        create("standard") {
            dimension = "distribution"
            buildConfigField("String", "BUILD_TIME", "\"${SimpleDateFormat("yyyy-MM-dd HH:mm").format(Date())}\"")
        }

        create("fdroid") {
            dimension = "distribution"
            buildConfigField("String", "DISTRIBUTION", "\"fdroid\"")
            buildConfigField("String", "BUILD_TIME", "\"reproducible\"")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
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
    defaultConfig {
        val gitHash = providers.exec { commandLine("git", "rev-parse", "--short", "HEAD") }.standardOutput.asText.get().trim()
        buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
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

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit.kotlinx.serialization)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Location - standard flavor only
    "standardImplementation"(libs.play.services.location)
    "standardImplementation"(libs.accompanist.permissions)
    "standardImplementation"(libs.kotlinx.coroutines.play.services)

    // F-Droid uses native Android LocationManager (no extra dependencies)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}