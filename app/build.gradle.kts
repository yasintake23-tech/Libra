import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
}

android {
    namespace = "com.libra.app"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.libra.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 3
        versionName = "1.0.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // CI uses one repository variable for the fixed signing key.
    // The key itself is committed so every APK uses the same Firebase certificate.
    val libraSigningPath = System.getenv("LIBRA_SIGNING_PATH")
    val libraSigningValue = System.getenv("LIBRA_SIGNING_VALUE")
    val libraSigningAlias = System.getenv("LIBRA_SIGNING_ALIAS")
    val hasLibraSigningKey = listOf(
        libraSigningPath,
        libraSigningValue,
        libraSigningAlias
    ).all { !it.isNullOrBlank() && !it.contains("REPLACE", ignoreCase = true) }

    signingConfigs {
        if (hasLibraSigningKey) {
            create("libra") {
                storeFile = file(requireNotNull(libraSigningPath))
                storePassword = requireNotNull(libraSigningValue)
                keyAlias = requireNotNull(libraSigningAlias)
                keyPassword = requireNotNull(libraSigningValue)
            }
        }
    }

    buildTypes {
        debug {
            if (hasLibraSigningKey) {
                signingConfig = signingConfigs.getByName("libra")
            }
        }
        release {
            isMinifyEnabled = false
            if (hasLibraSigningKey) {
                signingConfig = signingConfigs.getByName("libra")
            }
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    packaging.resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(platform(libs.firebase.bom))

    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.coil.compose)

    implementation(libs.firebase.auth)
    implementation(libs.firebase.database)
    implementation(libs.firebase.firestore)

    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.aws.android.sdk.s3)

    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.compose.ui.test.junit4)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

googleServices {
    missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN
}
