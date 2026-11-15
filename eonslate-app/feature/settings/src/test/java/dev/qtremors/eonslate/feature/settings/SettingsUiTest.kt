package dev.qtremors.eonslate.feature.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import dev.qtremors.eonslate.core.settings.AccentChoice
import dev.qtremors.eonslate.core.settings.Appearance
import dev.qtremors.eonslate.core.settings.ThemeChoice
import dev.qtremors.eonslate.core.ui.theme.EonslateTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class SettingsUiTest {
    @get:Rule
    val compose = createComposeRule()

    private val calls = mutableListOf<String>()
    private val actions = object : SettingsActions {
        override fun setTheme(theme: ThemeChoice) { calls += "theme:$theme" }
        override fun setPureBlack(on: Boolean) { calls += "black:$on" }
        override fun setAccent(accent: AccentChoice) { calls += "accent:$accent" }
        override fun setPreset(name: String) { calls += "preset:$name" }
        override fun setHaptics(on: Boolean) { calls += "haptics:$on" }
        override fun setReduceMotion(on: Boolean) { calls += "motion:$on" }
        override fun setWeekStart(start: dev.qtremors.eonslate.core.settings.WeekStart) { calls += "week:$start" }
        override fun setCurrency(code: String?) { calls += "currency:$code" }
    }
    private var about = 0

    private fun show(appearance: Appearance?, wallpaper: Boolean = true) = compose.setContent {
        EonslateTheme {
            SettingsContent(appearance, actions, onBack = {}, onAbout = { about++ }, wallpaperAvailable = wallpaper)
        }
    }

    @Test
    fun loadingShowsNoDefaultsFirst() {
        show(null)
        assertEquals(0, compose.onAllNodes(androidx.compose.ui.test.hasText("Theme")).fetchSemanticsNodes().size)
    }

    @Test
    fun everyRowChangesItsSettingAtOnce() {
        show(Appearance())
        compose.onNodeWithText("Dark").performClick()
        compose.onNodeWithText("Pure black").performClick()
        compose.onNodeWithText("Wallpaper").performClick()
        compose.onNodeWithText("Tide").performScrollTo().performClick()
        compose.onNodeWithText("Haptics").performScrollTo().performClick()
        compose.onNodeWithText("Reduce motion").performScrollTo().performClick()
        assertEquals(
            listOf("theme:DARK", "black:true", "accent:WALLPAPER", "preset:TIDE", "haptics:false", "motion:true"),
            calls,
        )
    }

    @Test
    fun theWeekAndCurrencyAreChosenHere() {
        show(Appearance())
        compose.onNode(androidx.compose.ui.test.hasScrollAction()).performScrollToNode(androidx.compose.ui.test.hasText("Mon"))
        compose.onNodeWithText("Mon").performClick()
        compose.onNode(androidx.compose.ui.test.hasScrollAction()).performScrollToNode(androidx.compose.ui.test.hasText("Currency"))
        compose.onNodeWithText("Currency").performClick()
        compose.onNode(androidx.compose.ui.test.hasSetTextAction()).performTextReplacement("zzz")
        compose.onNodeWithText("Not a currency this device knows").assertExists()
        compose.onNode(androidx.compose.ui.test.hasSetTextAction()).performTextReplacement("eur")
        compose.onNodeWithText("Save").performClick()
        assertEquals(listOf("week:MONDAY", "currency:EUR"), calls)
    }

    @Test
    fun pureBlackWaitsForADarkTheme() {
        show(Appearance(theme = ThemeChoice.LIGHT))
        compose.onNodeWithText("Only for the dark theme").assertIsDisplayed()
        compose.onNodeWithText("Pure black").assertIsNotEnabled()
    }

    @Test
    fun wallpaperColorsNeedAndroidTwelve() {
        show(Appearance(), wallpaper = false)
        assertEquals(0, compose.onAllNodes(androidx.compose.ui.test.hasText("Wallpaper")).fetchSemanticsNodes().size)
        compose.onNode(androidx.compose.ui.test.hasScrollAction()).performScrollToNode(androidx.compose.ui.test.hasText("Version, source code, licenses and privacy"))
        compose.onNodeWithText("Version, source code, licenses and privacy").performClick()
        assertEquals(1, about)
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "en-rUS-w411dp-h891dp-xxhdpi")
class AboutAndLicensesUiTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aboutShowsTheVersionAndTheWayToLicenses() {
        var licenses = 0
        var privacy = 0
        compose.setContent {
            EonslateTheme { AboutScreen(versionName = "2.0.8", onBack = {}, onLicenses = { licenses++ }, onPrivacy = { privacy++ }) }
        }
        compose.onNodeWithText("Version 2.0.8").assertIsDisplayed()
        compose.onNodeWithText("Tremors Source License 2.0").assertExists()
        compose.onNodeWithText("Open source licenses").performScrollTo().performClick()
        compose.onNodeWithText("Privacy").performScrollTo().performClick()
        assertEquals(1, licenses)
        assertEquals(1, privacy)
        compose.onNodeWithText("This device").performScrollTo().assertExists()
    }

    @Test
    fun aLicenseOpensWithItsFullText() {
        compose.setContent { EonslateTheme { LicensesScreen(onBack = {}) } }
        compose.onNode(androidx.compose.ui.test.hasScrollAction()).performScrollToNode(androidx.compose.ui.test.hasText("Room"))
        compose.onNodeWithText("Room").performClick()
        assertTrue(
            compose.onAllNodes(androidx.compose.ui.test.hasText("TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun theFontCarriesItsOwnNotice() {
        compose.setContent { EonslateTheme { LicensesScreen(onBack = {}) } }
        compose.onNode(androidx.compose.ui.test.hasScrollAction()).performScrollToNode(androidx.compose.ui.test.hasText("Roboto Flex"))
        compose.onNodeWithText("Roboto Flex").performClick()
        assertTrue(
            compose.onAllNodes(androidx.compose.ui.test.hasText("Copyright 2017 The Roboto Flex Project Authors", substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        )
    }

    @Test
    fun everyPartNamesItsLicense() {
        OpenSource.parts.forEach { part ->
            assertTrue(part.name, part.authors.isNotBlank() && part.url.startsWith("https://"))
            assertTrue(part.name, part.license.text != null || part.notice != null)
        }
    }

    @Test
    fun privacyIsReadableOffline() {
        compose.setContent { EonslateTheme { PrivacyScreen(onBack = {}) } }
        compose.onNodeWithText("Offline by design").assertIsDisplayed()
        compose.onNodeWithText("Permissions").performScrollTo().assertExists()
    }
}
