package dev.qtremors.eonslate.core.settings

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class DataStoreSettingsRepositoryTest {
    private fun store(scope: TestScope, file: File) = PreferenceDataStoreFactory.create(scope = scope.backgroundScope) { file }

    private fun newFile(): File =
        File(ApplicationProvider.getApplicationContext<android.content.Context>().filesDir, "test-${System.nanoTime()}.preferences_pb")

    @Test
    fun theWelcomeIsForNewcomersOnly() {
        assertTrue(WelcomeGate.shouldShow(seen = false, hasTrackers = false))
        assertFalse(WelcomeGate.shouldShow(seen = true, hasTrackers = false))
        assertFalse(WelcomeGate.shouldShow(seen = false, hasTrackers = true))
    }

    @Test
    fun welcomeStartsUnseenAndRemembersBeingSeen() = runTest(UnconfinedTestDispatcher()) {
        val settings = DataStoreSettingsRepository(store(this, newFile()))
        assertFalse(settings.welcomeSeen.first())
        settings.setWelcomeSeen(true)
        assertTrue(settings.welcomeSeen.first())
    }

    @Test
    fun appearanceStartsWithEonslatesOwnLook() = runTest(UnconfinedTestDispatcher()) {
        val settings = DataStoreSettingsRepository(store(this, newFile()))
        assertEquals(Appearance(), settings.appearance.first())
        assertEquals(AccentChoice.PRESET, settings.appearance.first().accent)
        assertEquals("MONO", settings.appearance.first().preset)
        assertEquals(true, settings.appearance.first().pureBlack)
    }

    @Test
    fun changesAreKeptTogether() = runTest(UnconfinedTestDispatcher()) {
        val settings = DataStoreSettingsRepository(store(this, newFile()))
        settings.updateAppearance { it.copy(theme = ThemeChoice.DARK) }
        settings.updateAppearance { it.copy(pureBlack = false, preset = "TIDE") }
        settings.updateAppearance { it.copy(haptics = false, reduceMotion = true) }
        settings.updateAppearance { it.copy(blockScreenshots = true) }
        assertEquals(
            Appearance(theme = ThemeChoice.DARK, pureBlack = false, preset = "TIDE", haptics = false, reduceMotion = true, blockScreenshots = true),
            settings.appearance.first(),
        )
    }

    @Test
    fun trackingStartsByFollowingTheDevice() = runTest(UnconfinedTestDispatcher()) {
        val settings = DataStoreSettingsRepository(store(this, newFile()))
        assertEquals(Tracking(), settings.tracking.first())
        settings.updateTracking { it.copy(weekStart = WeekStart.MONDAY, currency = "EUR") }
        assertEquals(Tracking(WeekStart.MONDAY, "EUR"), settings.tracking.first())
        settings.updateTracking { it.copy(currency = null) }
        assertEquals(null, settings.tracking.first().currency)
    }

    @Test
    fun theHomeTagSurvivesReopeningTheStore() {
        val file = newFile()
        runTest(UnconfinedTestDispatcher()) {
            val settings = DataStoreSettingsRepository(store(this, file))
            assertEquals(null, settings.homeTag.first())
            settings.setHomeTag("Health")
        }
        runTest(UnconfinedTestDispatcher()) {
            val settings = DataStoreSettingsRepository(store(this, file))
            assertEquals("Health", settings.homeTag.first())
            settings.setHomeTag(null)
            assertEquals(null, settings.homeTag.first())
        }
    }

    @Test
    fun theWeekStartsWhereTheLocaleOrTheChoiceSays() {
        assertEquals(java.time.DayOfWeek.SUNDAY, Tracking().firstDayOfWeek(java.util.Locale.US))
        assertEquals(java.time.DayOfWeek.MONDAY, Tracking().firstDayOfWeek(java.util.Locale.GERMANY))
        assertEquals(java.time.DayOfWeek.SATURDAY, Tracking(WeekStart.SATURDAY).firstDayOfWeek(java.util.Locale.US))
    }

    @Test
    fun theCurrencyIsChosenElseTheDevicesElseDollars() {
        assertEquals("EUR", Tracking(currency = "EUR").currencyCode(java.util.Locale("en", "IN")))
        assertEquals("INR", Tracking().currencyCode(java.util.Locale("en", "IN")))
        assertEquals("INR", Tracking(currency = "ZZZ").currencyCode(java.util.Locale("en", "IN")))
        assertEquals("USD", Tracking.deviceCurrency(java.util.Locale("en")))
        assertTrue(Tracking.isCurrency("JPY"))
        assertFalse(Tracking.isCurrency("jpy"))
        assertFalse(Tracking.isCurrency("ABCD"))
    }

    @Test
    fun aNameFromANewerVersionFallsBack() = runTest(UnconfinedTestDispatcher()) {
        val prefs = store(this, newFile())
        prefs.edit {
            it[stringPreferencesKey("theme")] = "SEPIA"
            it[stringPreferencesKey("accent")] = "PHOTO"
        }
        val appearance = DataStoreSettingsRepository(prefs).appearance.first()
        assertEquals(ThemeChoice.SYSTEM, appearance.theme)
        assertEquals(AccentChoice.PRESET, appearance.accent)
    }
}
