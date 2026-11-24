package dev.qtremors.eonslate.feature.entry

import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.insights.Parts
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.eonslate.core.tracker.domain.model.Entry
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.repository.EntryRepository
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationError
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationResult
import dev.qtremors.eonslate.core.tracker.ui.fieldId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject

data class LogState(
    val loading: Boolean = true,
    val missing: Boolean = false,
    val tracker: Tracker? = null,
    val draft: EntryDraft? = null,
    /** The tracker's latest entry, offered as "Same as last time" while a new entry is empty. */
    val lastEntry: Entry? = null,
    val errors: List<ValidationError> = emptyList(),
    val unreadable: Set<String> = emptySet(),
    val dirty: Boolean = false,
    val saving: Boolean = false,
    val done: Boolean = false,
) {
    /** True when "Same as last time" can fill this entry: it is new and nothing is entered yet. */
    val canUseLast: Boolean
        get() = lastEntry != null && draft?.isNew == true && draft.inputs.isEmpty()

    fun errorFor(fieldId: String): ValidationError? = errors.firstOrNull { it.fieldId == fieldId }
    val entryErrors: List<ValidationError> get() = errors.filter { it.fieldId == null }

    /** When the entry happened, in the zone it was logged in. */
    val occurred: ZonedDateTime?
        get() = draft?.let { ZonedDateTime.ofInstant(Instant.ofEpochMilli(it.occurredAt), zoneOf(it.zoneId)) }
}

internal fun zoneOf(id: String): ZoneId = runCatching { ZoneId.of(id) }.getOrDefault(ZoneId.systemDefault())

@HiltViewModel
class LogEntryViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val trackers: TrackerRepository,
    private val entries: EntryRepository,
    private val clock: AppClock,
    private val ids: IdGenerator,
) : ViewModel() {
    private val trackerId: String = checkNotNull(savedState[ARG_TRACKER_ID]) { "LogEntry needs a tracker" }
    private val entryId: String? = savedState[ARG_ENTRY_ID]
    private val optionId: String? = savedState[ARG_OPTION_ID]

    private val _state = MutableStateFlow(LogState())
    val state: StateFlow<LogState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val tracker = trackers.getTracker(trackerId)
            if (tracker == null) {
                _state.value = LogState(loading = false, missing = true)
                return@launch
            }
            val restored = savedState.get<String>(KEY_DRAFT)
                ?.let { runCatching { EntryJson.decodeFromString(EntryDraft.serializer(), it) }.getOrNull() }
            val draft = restored ?: entryId?.let { id ->
                entries.getEntry(id)?.takeIf { it.trackerId == trackerId }?.let { EntryInputs.draftOf(it, tracker) }
            } ?: if (entryId != null) {
                _state.value = LogState(loading = false, missing = true)
                return@launch
            } else {
                // Logged from one of its parts: that part is already picked.
                val part = optionId?.let { id -> Parts.field(tracker)?.takeIf { field -> (field.config as FieldConfig.SingleChoice).options.any { it.id == id } } }
                EntryDraft(
                    id = ids.newId(), trackerId = trackerId, occurredAt = clock.nowMillis(), zoneId = clock.zoneId(),
                    inputs = part?.let { mapOf(it.id to FieldInput(choices = setOf(optionId!!))) }.orEmpty(),
                )
            }
            val last = if (draft.isNew) entries.getLatestEntry(trackerId) else null
            _state.value = LogState(
                loading = false,
                tracker = tracker,
                draft = draft,
                lastEntry = last,
                dirty = restored != null && (savedState[KEY_DIRTY] ?: false),
            )
        }
    }

    private fun edit(change: (EntryDraft) -> EntryDraft) {
        _state.update { s -> s.draft?.let { s.copy(draft = change(it), dirty = true) } ?: s }
        _state.value.draft?.let { savedState[KEY_DRAFT] = EntryJson.encodeToString(EntryDraft.serializer(), it) }
        savedState[KEY_DIRTY] = true
    }

    fun setInput(fieldId: String, change: (FieldInput) -> FieldInput) {
        edit { d -> d.copy(inputs = d.inputs + (fieldId to change(d.inputs[fieldId] ?: FieldInput()))) }
        _state.update { s ->
            s.copy(
                unreadable = s.unreadable - fieldId,
                errors = s.errors.filterNot { it.fieldId == fieldId || it == ValidationError.EntryEmpty },
            )
        }
    }

    /** Fills every field with the latest entry's values; the time and note stay this entry's own. */
    fun useLastValues() {
        val s = _state.value
        val last = s.lastEntry ?: return
        val tracker = s.tracker ?: return
        val inputs = EntryInputs.draftOf(last, tracker).inputs
        edit { it.copy(inputs = inputs) }
        _state.update { it.copy(unreadable = emptySet(), errors = emptyList()) }
    }

    fun clearInput(fieldId: String) {
        edit { d -> d.copy(inputs = d.inputs - fieldId) }
        _state.update { s -> s.copy(unreadable = s.unreadable - fieldId, errors = s.errors.filterNot { it.fieldId == fieldId }) }
    }

    fun setNote(note: String) {
        edit { it.copy(note = note) }
        _state.update { s -> s.copy(errors = s.errors.filterNot { it == ValidationError.EntryEmpty || it is ValidationError.NoteTooLong }) }
    }

    /** Moves the entry to [date], keeping its time of day. */
    fun setDate(date: LocalDate) = edit { d ->
        val at = ZonedDateTime.ofInstant(Instant.ofEpochMilli(d.occurredAt), zoneOf(d.zoneId))
        d.copy(occurredAt = at.with(date).toInstant().toEpochMilli())
    }

    /** Moves the entry to [time] on its day. */
    fun setTime(time: LocalTime) = edit { d ->
        val at = ZonedDateTime.ofInstant(Instant.ofEpochMilli(d.occurredAt), zoneOf(d.zoneId))
        d.copy(occurredAt = at.with(time).toInstant().toEpochMilli())
    }

    fun save() {
        val s = _state.value
        val draft = s.draft ?: return
        val tracker = s.tracker ?: return
        if (s.saving) return
        val build = draft.build(tracker, clock.nowMillis())
        val entry = build.entry
        if (entry == null) {
            _state.update { it.copy(unreadable = build.unreadable) }
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            when (val result = entries.saveEntry(entry)) {
                ValidationResult.Valid -> {
                    savedState.remove<String>(KEY_DRAFT)
                    _state.update { it.copy(saving = false, done = true, dirty = false) }
                }
                is ValidationResult.Invalid -> _state.update { it.copy(saving = false, errors = result.errors) }
            }
        }
    }

    fun delete() {
        val draft = _state.value.draft ?: return
        viewModelScope.launch {
            if (!draft.isNew) entries.deleteEntry(draft.id)
            savedState.remove<String>(KEY_DRAFT)
            _state.update { it.copy(done = true, dirty = false) }
        }
    }

    companion object {
        const val ARG_TRACKER_ID = "trackerId"
        const val ARG_ENTRY_ID = "entryId"
        const val ARG_OPTION_ID = "optionId"
        private const val KEY_DRAFT = "log.draft"
        private const val KEY_DIRTY = "log.dirty"
    }
}
