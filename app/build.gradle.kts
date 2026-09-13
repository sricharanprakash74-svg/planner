import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.ksp)
}

val localProps = Properties()
val localPropsFile = rootProject.file("local.properties")
if (localPropsFile.exists()) {
    localProps.load(localPropsFile.inputStream())
}
val rawBackendBaseUrl = localProps.getProperty("BACKEND_BASE_URL") ?: System.getenv("BACKEND_BASE_URL") ?: "http://10.0.2.2:3000/"
val backendBaseUrl = if (rawBackendBaseUrl.endsWith("/")) rawBackendBaseUrl else "$rawBackendBaseUrl/"
val clientAppSecret = localProps.getProperty("CLIENT_APP_SECRET") ?: System.getenv("CLIENT_APP_SECRET") ?: "shipaton_hackathon_token"

android {
    namespace = "com.example.plannerapp"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.example.plannerapp"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        multiDexEnabled = true
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")
        buildConfigField("String", "CLIENT_APP_SECRET", "\"$clientAppSecret\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    flavorDimensions += "distribution"
    productFlavors {
        create("googlePlay") {
            dimension = "distribution"
            buildConfigField("String", "RC_API_KEY", "\"goog_placeholder_key\"")
            buildConfigField("String", "STORE_NAME", "\"Google Play\"")
        }
        create("galaxyStore") {
            dimension = "distribution"
            buildConfigField("String", "RC_API_KEY", "\"galx_placeholder_key\"")
            buildConfigField("String", "STORE_NAME", "\"Samsung Galaxy Store\"")
        }
    }

    buildFeatures {
      compose = true
      aidl = false
      buildConfig = true
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}


dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)
  
  coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.4")

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation("androidx.compose.material:material-icons-core")
  implementation("androidx.compose.material:material-icons-extended")
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // Room Database
  implementation(libs.androidx.room.runtime)
  implementation(libs.androidx.room.ktx)
  "ksp"(libs.androidx.room.compiler)

  // Glance Widget
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)

  // Coroutines
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)

  // WorkManager
  implementation("androidx.work:work-runtime-ktx:2.9.0")

  // Retrofit & OkHttp
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
  implementation("com.squareup.retrofit2:retrofit:2.11.0")
  implementation("com.squareup.retrofit2:converter-gson:2.11.0")

  // Supabase Auth & Compose Auth
  val supabaseBom = platform("io.github.jan-tennert.supabase:bom:3.0.3")
  implementation(supabaseBom)
  implementation("io.github.jan-tennert.supabase:auth-kt")
  implementation("io.github.jan-tennert.supabase:compose-auth")

  // Ktor Client (Engine for Supabase)
  implementation("io.ktor:ktor-client-android:3.0.3")

  // Android Credential Manager & Google ID (for native One-Tap Sign In)
  implementation("androidx.credentials:credentials:1.3.0")
  implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

  // Stripe SDK
  implementation("com.stripe:stripe-android:20.44.0")

  // Multi-Store In-App Purchases & Subscriptions (RevenueCat)
  val revenueCatVersion = "10.20.0"
  compileOnly("com.revenuecat.purchases:purchases:$revenueCatVersion")
  "googlePlayImplementation"("com.revenuecat.purchases:purchases:$revenueCatVersion")
  "galaxyStoreImplementation"("com.revenuecat.purchases:purchases-store-galaxy:$revenueCatVersion")
}
