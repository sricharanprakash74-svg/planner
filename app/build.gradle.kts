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
val clientAppSecret = localProps.getProperty("CLIENT_APP_SECRET") ?: System.getenv("CLIENT_APP_SECRET") ?: ""
val rcApiKey = localProps.getProperty("REVENUECAT_API_KEY") ?: System.getenv("REVENUECAT_API_KEY") ?: "goog_placeholder_key"
val useMockBilling = localProps.getProperty("USE_MOCK_BILLING") ?: System.getenv("USE_MOCK_BILLING") ?: "false"
val supabaseUrl = localProps.getProperty("SUPABASE_URL") ?: System.getenv("SUPABASE_URL") ?: "https://your-project.supabase.co"
val supabaseAnonKey = localProps.getProperty("SUPABASE_ANON_KEY") ?: System.getenv("SUPABASE_ANON_KEY") ?: "your-anon-key-placeholder"
val googleServerClientId = localProps.getProperty("GOOGLE_SERVER_CLIENT_ID") ?: System.getenv("GOOGLE_SERVER_CLIENT_ID") ?: "your-google-server-client-id.apps.googleusercontent.com"

val isReleaseTask = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
if (isReleaseTask) {
    val invalidConfigs = mutableListOf<String>()
    if (rcApiKey == "goog_placeholder_key" || rcApiKey.isBlank()) {
        invalidConfigs.add("REVENUECAT_API_KEY (cannot be placeholder or blank in release)")
    }
    if (supabaseUrl == "https://your-project.supabase.co" || supabaseUrl.isBlank()) {
        invalidConfigs.add("SUPABASE_URL (cannot be placeholder or blank in release)")
    }
    if (supabaseAnonKey == "your-anon-key-placeholder" || supabaseAnonKey.isBlank()) {
        invalidConfigs.add("SUPABASE_ANON_KEY (cannot be placeholder or blank in release)")
    }
    if (rawBackendBaseUrl == "http://10.0.2.2:3000/" || rawBackendBaseUrl.startsWith("http://")) {
        invalidConfigs.add("BACKEND_BASE_URL (must use HTTPS in release, emulator HTTP loopback not allowed)")
    }
    if (clientAppSecret.isBlank()) {
        invalidConfigs.add("CLIENT_APP_SECRET (cannot be blank in release)")
    }
    if (invalidConfigs.isNotEmpty()) {
        throw GradleException(
            "Production release build failed validation:\n" +
            invalidConfigs.joinToString("\n - ", prefix = " - ") +
            "\nPlease configure valid credentials in local.properties or environment variables."
        )
    }
}

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
        buildConfigField("String", "REVENUECAT_API_KEY", "\"$rcApiKey\"")
        buildConfigField("Boolean", "USE_MOCK_BILLING", useMockBilling)
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "GOOGLE_SERVER_CLIENT_ID", "\"$googleServerClientId\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
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

    testOptions {
        unitTests.all {
            it.maxHeapSize = "1g"
            it.jvmArgs("-XX:+UseParallelGC", "-XX:TieredStopAtLevel=1")
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
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

  // OkHttp & JSON
  implementation("com.squareup.okhttp3:okhttp:4.12.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
  implementation("com.google.code.gson:gson:2.11.0")

  // Supabase Auth & Compose Auth
  val supabaseBom = platform("io.github.jan-tennert.supabase:bom:3.0.3")
  implementation(supabaseBom)
  implementation("io.github.jan-tennert.supabase:auth-kt")
  implementation("io.github.jan-tennert.supabase:compose-auth")
  implementation("io.github.jan-tennert.supabase:postgrest-kt")
  implementation("io.github.jan-tennert.supabase:realtime-kt")
  implementation("io.github.jan-tennert.supabase:storage-kt")

  // Ktor Client (Engine for Supabase)
  implementation("io.ktor:ktor-client-android:3.0.3")

  // Android Credential Manager & Google ID (for native One-Tap Sign In)
  implementation("androidx.credentials:credentials:1.3.0")
  implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
  implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

  // Stripe SDK
  implementation("com.stripe:stripe-android:20.44.0")

  // RevenueCat In-App Purchases & Subscriptions
  implementation("com.revenuecat.purchases:purchases:10.20.0")

  // Smart Link Image Loading & Chrome Custom Tabs
  implementation("io.coil-kt:coil-compose:2.6.0")
  implementation("androidx.browser:browser:1.8.0")
}

tasks.withType<Test> {
    maxHeapSize = "1g"
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1).coerceAtMost(4)
}
