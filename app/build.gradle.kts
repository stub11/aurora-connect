plugins { alias(libs.plugins.android); alias(libs.plugins.kotlin); alias(libs.plugins.compose) }
android {
 namespace = "dev.aurora.connect"
 compileSdk = 35
 defaultConfig { applicationId = "dev.aurora.connect"; minSdk = 29; targetSdk = 35; versionCode = 1; versionName = "0.1.0" }
 buildFeatures { compose = true }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation("com.journeyapps:zxing-android-embedded:4.3.0")
 implementation(platform(libs.compose.bom))
 implementation(libs.material3)
 implementation(libs.activity)
 implementation(libs.wireguard)
 testImplementation(libs.junit)
}
