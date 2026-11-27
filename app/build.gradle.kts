import org.cyclonedx.gradle.CyclonedxDirectTask
import org.cyclonedx.model.Component

plugins {
 alias(libs.plugins.android)
 alias(libs.plugins.kotlin)
 alias(libs.plugins.compose)
 alias(libs.plugins.cyclonedx)
}

val auroraVersionName = "0.1.0-" + (System.getenv("GITHUB_SHA")?.take(8) ?: "local")
android {
 namespace = "dev.aurora.connect"
 compileSdk = 35
 defaultConfig {
  applicationId = "dev.aurora.connect"
  minSdk = 29
  targetSdk = 35
  versionCode = System.getenv("GITHUB_RUN_NUMBER")?.toInt() ?: 1
  versionName = auroraVersionName
  val devCertificate = rootProject.file("android/dev-cert.sha256").readText().trim()
  buildConfigField("String", "DEV_CERT_SHA256", "\"$devCertificate\"")
 }
 signingConfigs {
  create("dev") {
   storeFile = rootProject.file("build/aurora-dev.jks")
   storePassword = "aurora-dev-public"
   keyAlias = "aurora-dev"
   keyPassword = "aurora-dev-public"
  }
 }
 buildTypes { getByName("release") { signingConfig = signingConfigs.getByName("dev") }; getByName("debug") { signingConfig = signingConfigs.getByName("dev") } }
 buildFeatures { compose = true; buildConfig = true }
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
 testImplementation("org.json:json:20250107")
}

tasks.named<CyclonedxDirectTask>("cyclonedxDirectBom") {
 includeConfigs = listOf("releaseRuntimeClasspath")
 failOnUnresolvedDependencies = true
 projectType = Component.Type.APPLICATION
 componentName = "Aurora Connect"
 componentVersion = auroraVersionName
}
