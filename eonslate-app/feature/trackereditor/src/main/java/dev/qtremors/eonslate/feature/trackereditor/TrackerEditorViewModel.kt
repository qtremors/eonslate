package dev.qtremors.eonslate.feature.trackereditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.qtremors.eonslate.core.tracker.domain.model.ChoiceOption
import dev.qtremors.eonslate.core.tracker.domain.model.FieldConfig
import dev.qtremors.eonslate.core.tracker.domain.model.FieldType
import dev.qtremors.eonslate.core.tracker.domain.model.Tracker
import dev.qtremors.eonslate.core.tracker.domain.model.TrackerColors
import dev.qtremors.eonslate.core.tracker.domain.repository.TrackerRepository
import dev.qtremors.eonslate.core.tracker.domain.time.AppClock
import dev.qtremors.eonslate.core.tracker.domain.time.IdGenerator
import dev.qtremors.eonslate.core.tracker.domain.validation.TrackerValidator
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationError
import dev.qtremors.eonslate.core.tracker.domain.validation.ValidationResult
import dev.qtremors.eonslate.core.tracker.ui.fieldId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import java.util.Currency
import java.util.Locale
import javax.inject.Inject

/** The field being edited in the sheet, and whether it is new to the tracker. */
@Serializable
data class FieldSheet(val draft: FieldDraft, val isNew: Boolean)

data class EditorState(
    val loading: Boolean = true,
    /** True when the tracker being edited no longer exists. */
    val missing: Boolean = false,
    val draft: TrackerDraft? = null,
    /** Each stored field's type when editing began, to warn before a type change clears values. */
    val originalTypes: Map<String, FieldType> = emptyMap(),
    val errors: List<ValidationError> = emptyList(),
    val badLimits: Map<String, LimitProblems> = emptyMap(),
    val sheet: FieldSheet? = null,
    val sheetErrors: List<ValidationError> = emptyList(),
    val sheetLimits: LimitProblems = LimitProblems(),
    val dirty: Boolean = false,
    val saving: Boolean = false,
    /** Set once the tracker is saved or deleted; the screen then leaves. */
    val done: Boolean = false,
    /** True when [done] came from deleting, so the screen leaves past the tracker's own page. */
    val deleted: Boolean = false,
) {
    fun errorsFor(fieldId: String): List<ValidationError> = errors.filter { it.fieldIdOrNull() == fieldId }

    val trackerErrors: List<ValidationError> get() = errors.filter { it.fieldIdOrNull() == null }
}

@HiltViewModel
class TrackerEditorViewModel @Inject constructor(
    private val savedState: SavedStateHandle,
    private val repository: TrackerRepository,
    private val clock: AppClock,
    private val ids: IdGenerator,
) : ViewModel() {
    private val trackerId: String? = savedState[ARG_TRACKER_ID]
    private val defaultCurrency: String = defaultCurrencyCode()

    private val _state = MutableStateFlow(EditorState())
    val state: StateFlow<EditorState> = _state.asStateFlow()

    init {
        val restored = savedState.get<String>(KEY_DRAFT)?.let { runCatching { DraftJson.decodeFromString(TrackerDraft.serializer(), it) }.getOrNull() }
        if (restored != null) {
            _state.value = EditorState(
                loading = false,
                draft = restored,
                originalTypes = savedState.get<String>(KEY_TYPES)
                    ?.let { runCatching { DraftJson.decodeFromString(typesSerializer, it) }.getOrNull() }
                    .orEmpty()
                    .mapNotNull { (id, key) -> FieldType.fromStorageKey(key)?.let { id to it } }
                    .toMap(),
                sheet = savedState.get<String>(KEY_SHEET)?.let { runCatching { DraftJson.decodeFromString(FieldSheet.serializer(), it) }.getOrNull() },
                dirty = savedState[KEY_DIRTY] ?: false,
            )
        } else if (trackerId == null) {
            val id = ids.newId()
            setLoaded(TrackerDraft(id = id, colorIndex = Math.floorMod(id.hashCode(), TrackerColors.COUNT)), emptyMap())
        } else {
            viewModelScope.launch {
                val tracker = repository.getTracker(trackerId)
                if (tracker == null) {
                    _state.update { it.copy(loading = false, missing = true) }
                } else {
                    setLoaded(tracker.toDraft(defaultCurrency), tracker.fields.associate { it.id to it.type })
                }
            }
        }
    }

    private fun setLoaded(draft: TrackerDraft, types: Map<String, FieldType>) {
        _state.value = EditorState(loading = false, draft = draft, originalTypes = types)
        savedState[KEY_TYPES] = DraftJson.encodeToString(typesSerializer, types.mapValues { it.value.storageKey })
        persist()
    }

    private fun persist() {
        val s = _state.value
        s.draft?.let { savedState[KEY_DRAFT] = DraftJson.encodeToString(TrackerDraft.serializer(), it) }
        savedState[KEY_SHEET] = s.sheet?.let { DraftJson.encodeToString(FieldSheet.serializer(), it) }
        savedState[KEY_DIRTY] = s.dirty
    }

    private fun editDraft(change: (TrackerDraft) -> TrackerDraft) {
        _state.update { s -> s.draft?.let { s.copy(draft = change(it), dirty = true) } ?: s }
        persist()
    }

    fun setName(name: String) = editDraft { it.copy(name = name) }.also { clearTrackerError() }
    fun setDescription(text: String) = editDraft { it.copy(description = text) }
    fun setIcon(name: String) = editDraft { it.copy(iconName = name) }
    fun setColor(index: Int) = editDraft { it.copy(colorIndex = index) }
    fun setArchived(archived: Boolean) = editDraft { it.copy(archived = archived) }

    private fun clearTrackerError() {
        _state.update { s -> s.copy(errors = s.errors.filterNot { it is ValidationError.TrackerNameBlank || it is ValidationError.TrackerNameTooLong }) }
    }

    // Fields

    fun addField() {
        val count = _state.value.draft?.fields?.size ?: return
        if (count >= Tracker.MAX_FIELDS) return
        openSheet(FieldSheet(FieldDraft(id = ids.newId(), currencyCode = defaultCurrency), isNew = true))
    }

    fun editField(fieldId: String) {
        val field = _state.value.draft?.fields?.firstOrNull { it.id == fieldId } ?: return
        openSheet(FieldSheet(field, isNew = false))
    }

    private fun openSheet(sheet: FieldSheet) {
        _state.update { it.copy(sheet = sheet, sheetErrors = emptyList(), sheetLimits = LimitProblems()) }
        persist()
    }

    fun closeSheet() {
        _state.update { it.copy(sheet = null, sheetErrors = emptyList(), sheetLimits = LimitProblems()) }
        persist()
    }

    /** Changes the field in the sheet. Problems clear as the person edits, never mid-typing. */
    fun updateSheet(change: (FieldDraft) -> FieldDraft) {
        _state.update { s ->
            s.sheet?.let { s.copy(sheet = it.copy(draft = change(it.draft)), sheetErrors = emptyList(), sheetLimits = LimitProblems()) } ?: s
        }
        persist()
    }

    fun setFieldType(type: FieldType) = updateSheet { draft ->
        // A choice field starts with two empty options so there is something to type into.
        if ((type == FieldType.SINGLE_CHOICE || type == FieldType.MULTI_CHOICE) && draft.options.isEmpty()) {
            draft.copy(type = type, options = List(2) { ChoiceOption(ids.newId(), "") })
        } else draft.copy(type = type)
    }

    fun addOption() = updateSheet { draft ->
        if (draft.options.size >= FieldConfig.MAX_OPTIONS) draft
        else draft.copy(options = draft.options + ChoiceOption(ids.newId(), ""))
    }

    fun setOptionLabel(optionId: String, label: String) = updateSheet { draft ->
        draft.copy(options = draft.options.map { if (it.id == optionId) it.copy(label = label) else it })
    }

    fun removeOption(optionId: String) = updateSheet { draft ->
        draft.copy(options = draft.options.filterNot { it.id == optionId })
    }

    /** Checks the field in the sheet on its own and against its siblings, then keeps it. */
    fun commitSheet() {
        val s = _state.value
        val sheet = s.sheet ?: return
        val draft = s.draft ?: return
        val build = sheet.draft.build(draft.id)
        val field = build.field
        if (field == null) {
            _state.update { it.copy(sheetLimits = build.problems) }
            return
        }
        val errors = TrackerValidator.validateField(field).toMutableList()
        val name = field.name.lowercase()
        if (name.isNotEmpty() && draft.fields.any { it.id != field.id && it.name.trim().lowercase() == name }) {
            errors += ValidationError.DuplicateFieldName(field.id, field.name)
        }
        if (errors.isNotEmpty()) {
            _state.update { it.copy(sheetErrors = errors) }
            return
        }
        val fields = if (sheet.isNew) draft.fields + sheet.draft
        else draft.fields.map { if (it.id == sheet.draft.id) sheet.draft else it }
        _state.update { st ->
            st.copy(
                draft = draft.copy(fields = fields),
                sheet = null,
                dirty = true,
                errors = st.errors.filterNot { it.fieldIdOrNull() == field.id || it == ValidationError.NoFields },
                badLimits = st.badLimits - field.id,
            )
        }
        persist()
    }

    fun removeField(fieldId: String) {
        editDraft { it.copy(fields = it.fields.filterNot { f -> f.id == fieldId }) }
        _state.update { s -> s.copy(sheet = s.sheet?.takeIf { it.draft.id != fieldId }) }
        persist()
    }

    /** Moves a field [by] places, clamped to the list. */
    fun moveField(fieldId: String, by: Int) = editDraft { draft ->
        val from = draft.fields.indexOfFirst { it.id == fieldId }
        if (from < 0) return@editDraft draft
        val to = (from + by).coerceIn(0, draft.fields.lastIndex)
        val list = draft.fields.toMutableList()
        list.add(to, list.removeAt(from))
        draft.copy(fields = list)
    }

    // Saving

    fun save() {
        val draft = _state.value.draft ?: return
        if (_state.value.saving) return
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val position = draft.position ?: repository.nextPosition()
            val build = draft.build(clock.nowMillis(), position)
            val tracker = build.tracker
            if (tracker == null) {
                _state.update { it.copy(saving = false, badLimits = build.badLimits) }
                return@launch
            }
            when (val result = repository.saveTracker(tracker)) {
                ValidationResult.Valid -> {
                    _state.update { it.copy(saving = false, done = true, dirty = false) }
                    savedState.remove<String>(KEY_DRAFT)
                }
                is ValidationResult.Invalid -> _state.update { it.copy(saving = false, errors = result.errors) }
            }
        }
    }

    fun delete() {
        val draft = _state.value.draft ?: return
        viewModelScope.launch {
            if (!draft.isNew) repository.deleteTracker(draft.id)
            _state.update { it.copy(done = true, deleted = !draft.isNew, dirty = false) }
            savedState.remove<String>(KEY_DRAFT)
        }
    }

    companion object {
        /** The argument name type-safe navigation stores the route's tracker id under. */
        const val ARG_TRACKER_ID = "trackerId"
        private const val KEY_DRAFT = "editor.draft"
        private const val KEY_SHEET = "editor.sheet"
        private const val KEY_DIRTY = "editor.dirty"
        private const val KEY_TYPES = "editor.types"
        /** Types are kept by their stable storage keys. */
        private val typesSerializer = MapSerializer(String.serializer(), String.serializer())

        fun defaultCurrencyCode(locale: Locale = Locale.getDefault()): String =
            runCatching { Currency.getInstance(locale)?.currencyCode }.getOrNull() ?: "USD"
    }
}

/** The field a problem points at, or null for a problem with the whole tracker. */
internal fun ValidationError.fieldIdOrNull(): String? = fieldId
