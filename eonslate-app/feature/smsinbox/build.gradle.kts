plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "dev.qtremors.eonslate.feature.smsinbox"
    compileSdk = 37
    defaultConfig { minSdk = 26 }
}
