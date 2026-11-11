plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }

android {
    namespace = "com.ryzemc.pequenomundo"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "com.ryzemc.pequenomundo.noah"
        minSdk = 23
        targetSdk = 35
        versionCode = 3
        versionName = "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    androidResources { noCompress += "wav" }
    buildTypes {
        release { isMinifyEnabled = false }
    }
    val ownerStore = System.getenv("NOAH_SIGNING_STORE")
    if (!ownerStore.isNullOrBlank()) {
        signingConfigs {
            create("ownerRelease") {
                storeFile = file(ownerStore)
                storePassword = System.getenv("NOAH_SIGNING_PASSWORD")
                keyAlias = "noah-release"
                keyPassword = System.getenv("NOAH_SIGNING_PASSWORD")
            }
        }
        buildTypes.getByName("release").signingConfig = signingConfigs.getByName("ownerRelease")
    }
    testOptions { animationsDisabled = true }
}

dependencies {
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
