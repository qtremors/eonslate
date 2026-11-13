import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
}

android {
    namespace = "dev.qtremors.eonslate"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.qtremors.eonslate"
        minSdk = 26
        targetSdk = 37
        versionCode = 203
        versionName = "2.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    val keystoreProperties = Properties()
    var keystorePropertiesFile = rootProject.file("signing.properties")
    if (!keystorePropertiesFile.exists()) {
        keystorePropertiesFile = rootProject.file("local.properties")
    }
    if (keystorePropertiesFile.exists()) {
        keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
    }

    val storeFileProp = keystoreProperties["signing.storeFile"]?.toString()
    val storePasswordProp = keystoreProperties["signing.storePassword"]?.toString()
    val keyAliasProp = keystoreProperties["signing.keyAlias"]?.toString()
    val keyPasswordProp = keystoreProperties["signing.keyPassword"]?.toString()
    val hasSigningConfig = storeFileProp != null && storePasswordProp != null &&
        keyAliasProp != null && keyPasswordProp != null

    if (hasSigningConfig) {
        signingConfigs {
            create("release") {
                storeFile = file(storeFileProp!!)
                storePassword = storePasswordProp
                keyAlias = keyAliasProp
                keyPassword = keyPasswordProp
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            manifestPlaceholders["appLabel"] = "Eonslate Debug"
        }
        release {
            if (hasSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            manifestPlaceholders["appLabel"] = "Eonslate"
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
        buildConfig = true
    }
    packaging {
        jniLibs {
            keepDebugSymbols += setOf(
                "**/libandroidx.graphics.path.so",
                "**/libdatastore_shared_counter.so"
            )
        }
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

androidComponents {
    onVariants { variant ->
        variant.outputs.forEach { output ->
            val version = output.versionName.get() ?: "0.0.0"
            output.outputFileName.set("Eonslate-$version.apk")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
        optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
        optIn.add("androidx.compose.material3.ExperimentalMaterial3ExpressiveApi")
    }
}

dependencies {
    implementation(project(":core:navigation:api"))
    implementation(project(":core:tracker:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:home"))
    implementation(project(":feature:trackereditor"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
