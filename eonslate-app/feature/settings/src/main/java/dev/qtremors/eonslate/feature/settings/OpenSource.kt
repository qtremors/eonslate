package dev.qtremors.eonslate.feature.settings

import androidx.annotation.RawRes

/** A license an open source part of Eonslate is used under. */
enum class LicenseKind(val title: String, @param:RawRes val text: Int?, val url: String) {
    APACHE_2_0("Apache License 2.0", R.raw.license_apache_2_0, "https://www.apache.org/licenses/LICENSE-2.0"),
    MIT("MIT License", R.raw.license_mit, "https://opensource.org/license/mit"),
    OFL_1_1("SIL Open Font License 1.1", R.raw.license_ofl_1_1, "https://openfontlicense.org"),
}

/**
 * One open source part of the app, credited by its authors' own name for themselves.
 * [notice] is shown above the license text, such as a font's copyright line.
 */
data class OpenSourcePart(
    val name: String,
    val authors: String,
    val license: LicenseKind,
    val url: String,
    val notice: String? = null,
)

object OpenSource {
    /**
     * Only what Eonslate actually ships: its libraries, the language and the font. Design
     * references and projects it does not use are never listed.
     */
    val parts: List<OpenSourcePart> = listOf(
        OpenSourcePart("Jetpack Compose", "The Android Open Source Project", LicenseKind.APACHE_2_0, "https://developer.android.com/jetpack/compose"),
        OpenSourcePart("Material 3 for Compose", "The Android Open Source Project", LicenseKind.APACHE_2_0, "https://developer.android.com/jetpack/androidx/releases/compose-material3"),
        OpenSourcePart("Material Symbols", "Google", LicenseKind.APACHE_2_0, "https://fonts.google.com/icons"),
        OpenSourcePart("AndroidX (Activity, Core, Lifecycle, Navigation, Splash Screen)", "The Android Open Source Project", LicenseKind.APACHE_2_0, "https://developer.android.com/jetpack/androidx"),
        OpenSourcePart("Room", "The Android Open Source Project", LicenseKind.APACHE_2_0, "https://developer.android.com/jetpack/androidx/releases/room"),
        OpenSourcePart("DataStore", "The Android Open Source Project", LicenseKind.APACHE_2_0, "https://developer.android.com/jetpack/androidx/releases/datastore"),
        OpenSourcePart("Dagger and Hilt", "The Dagger Authors", LicenseKind.APACHE_2_0, "https://dagger.dev"),
        OpenSourcePart("Kotlin", "JetBrains", LicenseKind.APACHE_2_0, "https://kotlinlang.org"),
        OpenSourcePart("Kotlin Coroutines", "JetBrains", LicenseKind.APACHE_2_0, "https://github.com/Kotlin/kotlinx.coroutines"),
        OpenSourcePart("Kotlin Serialization", "JetBrains", LicenseKind.APACHE_2_0, "https://github.com/Kotlin/kotlinx.serialization"),
        OpenSourcePart("MaterialKolor", "Jordon de Hoog", LicenseKind.MIT, "https://github.com/jordond/MaterialKolor"),
        // Shipped inside MaterialKolor, generated from Google's source and kept under its license.
        OpenSourcePart("Material Color Utilities", "Google", LicenseKind.APACHE_2_0, "https://github.com/material-foundation/material-color-utilities"),
        OpenSourcePart(
            "Roboto Flex",
            "The Roboto Flex Project Authors",
            LicenseKind.OFL_1_1,
            "https://github.com/TypeNetwork/Roboto-Flex",
            notice = "Copyright 2017 The Roboto Flex Project Authors (https://github.com/TypeNetwork/Roboto-Flex)\n\n" +
                "This Font Software is licensed under the SIL Open Font License, Version 1.1. " +
                "This license is available with a FAQ at: https://openfontlicense.org",
        ),
    )
}
