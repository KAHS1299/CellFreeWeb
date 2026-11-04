import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

// Load local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(localPropertiesFile.inputStream())
}

// Helper function to get property with default value
fun getLocalProperty(key: String, defaultValue: String = ""): String {
    return localProperties.getProperty(key) ?: defaultValue
}

android {
    namespace = "com.example.cellfreeapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.cellfreeapp"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Add BuildConfig fields from local.properties
        buildConfigField("String", "API_BASE_URL", "\"${getLocalProperty("API_BASE_URL", "http://10.0.2.2:8000/api/")}\"")
        buildConfigField("int", "API_TIMEOUT_SECONDS", getLocalProperty("API_TIMEOUT_SECONDS", "30"))
        buildConfigField("boolean", "API_ENABLE_LOGGING", getLocalProperty("API_ENABLE_LOGGING", "true"))
        buildConfigField("boolean", "APP_DEBUG_MODE", getLocalProperty("APP_DEBUG_MODE", "true"))
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Override for production
            buildConfigField("boolean", "API_ENABLE_LOGGING", "false")
            buildConfigField("boolean", "APP_DEBUG_MODE", "false")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        buildConfig = true  // Enable BuildConfig generation
        viewBinding = true  // Enable ViewBinding for layouts
        // compose = true  // Comentado ya que usamos XML layouts
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    
    // Networking dependencies
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    
    // RecyclerView and other UI components
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.constraintlayout)
    
    // WorkManager para tareas en background
    implementation(libs.androidx.work.runtime.ktx)
    
    // Para notificaciones y permisos
    implementation(libs.androidx.core)

    // Exclude conflicting dependencies
    configurations.all {
        exclude(group = "com.intellij", module = "annotations")
    }

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}