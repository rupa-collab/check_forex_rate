import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

val apiKeyFromLocal = localProperties.getProperty("EXCHANGE_RATE_API_KEY")
val apiKeyFromGradle = project.findProperty("EXCHANGE_RATE_API_KEY") as String?
val exchangeRateApiKey = (apiKeyFromLocal ?: apiKeyFromGradle ?: "").trim()\r\n\r\nval authBaseFromLocal = localProperties.getProperty("AUTH_API_BASE_URL")\r\nval authBaseFromGradle = project.findProperty("AUTH_API_BASE_URL") as String?\r\nval authApiBaseUrl = (authBaseFromLocal ?: authBaseFromGradle ?: "").trim()\r\n
android {
    namespace = "com.checkrate.app"
    compileSdk = libs.versions.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.checkrate.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"
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

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

androidComponents {
    onVariants { variant ->
        variant.buildConfigFields?.put(\r\n            "EXCHANGE_RATE_API_KEY",\r\n            com.android.build.api.variant.BuildConfigField("String", "\"$exchangeRateApiKey\"", "")\r\n        )\r\n        variant.buildConfigFields?.put(\r\n            "AUTH_API_BASE_URL",\r\n            com.android.build.api.variant.BuildConfigField("String", "\"$authApiBaseUrl\"", "")\r\n        )
    }
}

dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    debugImplementation(libs.compose.ui.tooling)

    implementation(libs.compose.material.icons.extended)

    implementation(libs.core.ktx)
    implementation(libs.activity.compose)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.work.runtime.ktx)
    implementation(libs.datastore.preferences)
    implementation(libs.material)
}
