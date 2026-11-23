import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.mapsplatform.secrets)
}

val secretsProperties = Properties()
val secretsFile = rootProject.file("secrets.properties")
if (secretsFile.exists()) {
    secretsFile.inputStream().use { secretsProperties.load(it) }
}
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

val mapsApiKey = secretsProperties.getProperty("MAPS_API_KEY")
    ?: localProperties.getProperty("MAPS_API_KEY")
    ?: "DEFAULT_MAPS_API_KEY"

val clientId = secretsProperties.getProperty("TRAFFIC_SCOTLAND_CLIENT_ID")
    ?: localProperties.getProperty("TRAFFIC_SCOTLAND_CLIENT_ID")
    ?: ""

val clientKey = secretsProperties.getProperty("TRAFFIC_SCOTLAND_CLIENT_KEY")
    ?: localProperties.getProperty("TRAFFIC_SCOTLAND_CLIENT_KEY")
    ?: ""

secrets {
    propertiesFileName = "secrets.properties"
    defaultPropertiesFileName = "local.defaults.properties"
}

android {
    namespace = "com.example.cw1trafficapp"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.cw1trafficapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["MAPS_API_KEY"] = mapsApiKey

        buildConfigField("String", "TRAFFIC_SCOTLAND_CLIENT_ID", "\"$clientId\"")
        buildConfigField("String", "TRAFFIC_SCOTLAND_CLIENT_KEY", "\"$clientKey\"")
        buildConfigField("String", "MAPS_API_KEY", "\"$mapsApiKey\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            optimization {
                enable = false
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.recyclerview)
    implementation(libs.cardview)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.fragment)
    implementation(libs.play.services.maps)
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")

    testImplementation(libs.junit)
    testImplementation("net.sf.kxml:kxml2:2.3.0")
    testImplementation("xmlpull:xmlpull:1.1.3.1")
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
