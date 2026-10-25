plugins { id("com.android.application") }
android {
    namespace = "com.skypier.capri"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.skypier.capri"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
