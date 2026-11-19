package dev.qtremors.eonslate.core.settings

import androidx.datastore.preferences.core.stringSetPreferencesKey
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** App settings that are not tracker data. Kept apart from the database so a restore never touches them. */
interface SettingsRepository {
    /** True once the welcome pages were finished or skipped. */
    val welcomeSeen: Flow<Boolean>

    suspend fun setWelcomeSeen(seen: Boolean)

    /** How the app looks; emits again on every change, so the whole app follows at once. */
    val appearance: Flow<Appearance>

    /** Changes the appearance in one write, so two quick changes never lose each other. */
    suspend fun updateAppearance(change: (Appearance) -> Appearance)

    /** How tracking works: the week's first day and the currency new money fields start with. */
    val tracking: Flow<Tracking>

    suspend fun updateTracking(change: (Tracking) -> Tracking)

    val display: Flow<DisplayPreferences>

    suspend fun updateDisplay(change: (DisplayPreferences) -> DisplayPreferences)

    val homeTag: Flow<String?>

    suspend fun setHomeTag(tag: String?)

    /** Payment SMS detection and what was dismissed. */
    val sms: Flow<SmsSettings>

    suspend fun updateSms(change: (SmsSettings) -> SmsSettings)
}

/** Whether the welcome pages show at start. */
object WelcomeGate {
    /**
     * Only for someone new: once seen it never shows again, and someone who already has
     * trackers (after an update or a restore) never needs it.
     */
    fun shouldShow(seen: Boolean, hasTrackers: Boolean): Boolean = !seen && !hasTrackers
}

/** The settings, stored with DataStore in `files/datastore/settings.preferences_pb`. */
@Singleton
class DataStoreSettingsRepository @Inject constructor(
    private val store: DataStore<Preferences>,
) : SettingsRepository {

    override val welcomeSeen: Flow<Boolean> = store.data.map { it[WELCOME_SEEN] ?: false }.distinctUntilChanged()

    override suspend fun setWelcomeSeen(seen: Boolean) {
        store.edit { it[WELCOME_SEEN] = seen }
    }

    override val appearance: Flow<Appearance> = store.data.map(::read).distinctUntilChanged()

    override suspend fun updateAppearance(change: (Appearance) -> Appearance) {
        store.edit { prefs ->
            val next = change(read(prefs))
            prefs[THEME] = next.theme.name
            prefs[PURE_BLACK] = next.pureBlack
            prefs[ACCENT] = next.accent.name
            prefs[PRESET] = next.preset
            prefs[HAPTICS] = next.haptics
            prefs[REDUCE_MOTION] = next.reduceMotion
            prefs[BLOCK_SCREENSHOTS] = next.blockScreenshots
        }
    }

    override val tracking: Flow<Tracking> = store.data.map(::readTracking).distinctUntilChanged()

    override suspend fun updateTracking(change: (Tracking) -> Tracking) {
        store.edit { prefs ->
            val next = change(readTracking(prefs))
            prefs[WEEK_START] = next.weekStart.name
            // An empty value means "follow the device", so a cleared choice is not kept as a code.
            prefs[CURRENCY] = next.currency.orEmpty()
        }
    }

    override val display: Flow<DisplayPreferences> = store.data.map(::readDisplay).distinctUntilChanged()

    override suspend fun updateDisplay(change: (DisplayPreferences) -> DisplayPreferences) {
        store.edit { prefs ->
            val next = change(readDisplay(prefs))
            prefs[DISPLAY_NAME] = next.name.trim().take(DisplayPreferences.NAME_LIMIT)
            prefs[NUMBER_LOCALE] = next.numbers.name
            prefs[DATE_DISPLAY] = next.dates.name
        }
    }

    private fun readDisplay(prefs: Preferences) = DisplayPreferences(
        name = prefs[DISPLAY_NAME].orEmpty(),
        numbers = enumOrDefault(prefs[NUMBER_LOCALE], NumberLocale.DEVICE),
        dates = enumOrDefault(prefs[DATE_DISPLAY], DateDisplay.DEVICE),
    )

    override val homeTag: Flow<String?> = store.data.map { it[HOME_TAG] }.distinctUntilChanged()

    override suspend fun setHomeTag(tag: String?) {
        store.edit { prefs ->
            if (tag == null) prefs.remove(HOME_TAG) else prefs[HOME_TAG] = tag
        }
    }

    override val sms: Flow<SmsSettings> = store.data.map(::readSms).distinctUntilChanged()

    override suspend fun updateSms(change: (SmsSettings) -> SmsSettings) {
        store.edit { prefs ->
            val next = change(readSms(prefs))
            prefs[SMS_DETECT] = next.detect
            prefs[SMS_AUTO] = next.autoApprove
            // Bounded, so a years-long habit of dismissing never grows the file without end.
            prefs[SMS_DISCARDED] = next.discarded.toList().takeLast(MAX_DISCARDED).toSet()
        }
    }

    private fun readSms(prefs: Preferences) = SmsSettings(
        detect = prefs[SMS_DETECT] ?: false,
        autoApprove = prefs[SMS_AUTO] ?: false,
        discarded = prefs[SMS_DISCARDED].orEmpty(),
    )

    private fun readTracking(prefs: Preferences): Tracking = Tracking(
        weekStart = enumOrDefault(prefs[WEEK_START], WeekStart.LOCALE),
        currency = prefs[CURRENCY]?.takeIf { it.isNotEmpty() },
    )

    private fun read(prefs: Preferences): Appearance {
        val defaults = Appearance()
        return Appearance(
            theme = enumOrDefault(prefs[THEME], defaults.theme),
            pureBlack = prefs[PURE_BLACK] ?: defaults.pureBlack,
            accent = enumOrDefault(prefs[ACCENT], defaults.accent),
            preset = prefs[PRESET] ?: defaults.preset,
            haptics = prefs[HAPTICS] ?: defaults.haptics,
            reduceMotion = prefs[REDUCE_MOTION] ?: defaults.reduceMotion,
            blockScreenshots = prefs[BLOCK_SCREENSHOTS] ?: defaults.blockScreenshots,
        )
    }

    companion object {
        const val FILE = "settings"
        private val WELCOME_SEEN = booleanPreferencesKey("welcome_seen")
        private val THEME = stringPreferencesKey("theme")
        private val PURE_BLACK = booleanPreferencesKey("pure_black")
        private val ACCENT = stringPreferencesKey("accent")
        private val PRESET = stringPreferencesKey("preset")
        private val HAPTICS = booleanPreferencesKey("haptics")
        private val REDUCE_MOTION = booleanPreferencesKey("reduce_motion")
        private val BLOCK_SCREENSHOTS = booleanPreferencesKey("block_screenshots")
        private val WEEK_START = stringPreferencesKey("week_start")
        private val CURRENCY = stringPreferencesKey("currency")
        private val DISPLAY_NAME = stringPreferencesKey("display_name")
        private val NUMBER_LOCALE = stringPreferencesKey("number_locale")
        private val DATE_DISPLAY = stringPreferencesKey("date_display")
        private val SMS_DETECT = booleanPreferencesKey("sms_detect")
        private val SMS_AUTO = booleanPreferencesKey("sms_auto_approve")
        private val SMS_DISCARDED = stringSetPreferencesKey("sms_discarded")
        private const val MAX_DISCARDED = 2_000
        private val HOME_TAG = stringPreferencesKey("home_tag")
    }
}

@Module
@InstallIn(SingletonComponent::class)
object SettingsModule {
    @Provides
    @Singleton
    fun provideStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(DataStoreSettingsRepository.FILE) }

    @Provides
    @Singleton
    fun provideSettings(store: DataStore<Preferences>): SettingsRepository = DataStoreSettingsRepository(store)
}
