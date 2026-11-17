plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.example.watchhealth"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.example.watchhealth"
        minSdk = 30; targetSdk = 33; versionCode = 1; versionName = "1.0"
        buildConfigField("String", "SUPA_URL", "\"${System.getenv("SUPA_URL") ?: ""}\"")
        buildConfigField("String", "SUPA_KEY", "\"${System.getenv("SUPA_KEY") ?: ""}\"")
    }
    buildFeatures { buildConfig = true }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
