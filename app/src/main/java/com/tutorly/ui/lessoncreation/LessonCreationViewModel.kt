package com.tutorly.ui.lessoncreation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.model.LessonCreateRequest
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.model.RecurrenceCreateRequest
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.domain.repo.SubjectPresetsRepository
import com.tutorly.domain.repo.UserSettingsRepository
import com.tutorly.domain.recurrence.RecurrenceLabelFormatter
import com.tutorly.models.RecurrenceFrequency
import com.tutorly.models.Student
import com.tutorly.models.SubjectPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import kotlin.collections.buildSet
import kotlin.math.max
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@HiltViewModel
class LessonCreationViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val lessonsRepository: LessonsRepository,
    private val studentsRepository: StudentsRepository,
    private val subjectPresetsRepository: SubjectPresetsRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonCreationUiState())
    val uiState: StateFlow<LessonCreationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LessonCreationEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    private val json = Json { encodeDefaults = true }

    private companion object {
        private const val KEY_DRAFT = "lesson_creation:draft"
        private const val KEY_DURATION_EDITED = "lesson_creation:duration_edited"
        private const val KEY_PRICE_EDITED = "lesson_creation:price_edited"
    }

    private var cachedSubjects: List<SubjectPreset> = emptyList()
    private var durationEdited: Boolean = false
    private var priceEdited: Boolean = false
    private var currentZone: ZoneId = ZoneId.systemDefault()
    private var conflictRequest: LessonCreateRequest? = null
    private var pendingStudentConfig: LessonCreationConfig? = null
    private val rateHistoryCache: MutableMap<Long, List<RateUsage>> = mutableMapOf()
    private var currentRateHistory: List<RateUsage> = emptyList()
    private var currentStudentBaseRateCents: Int? = null
    private var currentStudentBaseRateDuration: Int? = null

    private fun loadDraft(): LessonDraft? {
        val payload = savedStateHandle.get<String>(KEY_DRAFT) ?: return null
        return runCatching { json.decodeFromString<LessonDraft>(payload) }.getOrNull()
    }

    private fun saveDraft(state: LessonCreationUiState) {
        val draft = state.toLessonDraft()
        savedStateHandle[KEY_DRAFT] = json.encodeToString(draft)
        savedStateHandle[KEY_DURATION_EDITED] = durationEdited
        savedStateHandle[KEY_PRICE_EDITED] = priceEdited
    }

    private fun clearDraft() {
        savedStateHandle.remove<String>(KEY_DRAFT)
        savedStateHandle.remove<Boolean>(KEY_DURATION_EDITED)
        savedStateHandle.remove<Boolean>(KEY_PRICE_EDITED)
    }

    init {
        viewModelScope.launch {
            cachedSubjects = subjectPresetsRepository.all()
            updateUiState(persist = true) { state ->
                val options = cachedSubjects.map { it.toOption() }
                val sanitizedChips = state.selectedSubjectChips
                    .mapNotNull { chip ->
                        when {
                            chip.id == null -> chip
                            options.any { it.id == chip.id } -> chip
                            else -> null
                        }
                    }
                val selected = state.selectedSubjectId?.takeIf { id -> options.any { it.id == id } }
                val withPrimaryChip = if (selected != null && sanitizedChips.none { it.id == selected }) {
                    val option = options.firstOrNull { it.id == selected }
                    if (option != null) sanitizedChips + option.toChip() else sanitizedChips
                } else {
                    sanitizedChips
                }
                val keepIds = withPrimaryChip.mapNotNull { it.id }.toSet()
                state.copy(
                    subjects = options,
                    availableSubjects = resolveAvailableSubjects(state.selectedStudent, state.locale, keepIds),
                    selectedSubjectId = selected,
                    selectedSubjectChips = withPrimaryChip
                )
            }
            val currentSelected = _uiState.value.selectedSubjectId
            if (currentSelected != null && cachedSubjects.any { it.id == currentSelected }) {
                onSubjectSelected(currentSelected)
            }
        }
    }

    fun start(config: LessonCreationConfig) {
        pendingStudentConfig = null
        viewModelScope.launch {
            cachedSubjects = subjectPresetsRepository.all()
            val settings = userSettingsRepository.get()
            val locale = Locale(settings.locale)
            val currencySymbol = runCatching { Currency.getInstance(settings.currency).getSymbol(locale) }
                .getOrDefault(settings.currency)
            val savedDraft = loadDraft()
            val zone = savedDraft?.zoneId?.let { runCatching(ZoneId::of).getOrNull() }
                ?: config.zoneId
                ?: config.start?.zone
                ?: ZoneId.systemDefault()
            val start = config.start ?: ZonedDateTime.now(zone)
            currentZone = zone
            val slotStep = max(5, settings.slotStepMinutes)
            val roundedStart = roundToStep(start, slotStep)
            val baseDuration = config.duration?.toMinutes()?.toInt()
                ?: settings.defaultLessonDurationMinutes
            val basePrice = settings.defaultLessonPriceCents

            durationEdited = savedStateHandle.get<Boolean>(KEY_DURATION_EDITED) ?: (config.duration != null)
            priceEdited = savedStateHandle.get<Boolean>(KEY_PRICE_EDITED) ?: false
            currentRateHistory = emptyList()
            currentStudentBaseRateCents = null
            currentStudentBaseRateDuration = null

            val students = loadStudents("")
            val subjectOptions = cachedSubjects.map { it.toOption() }
            val baseState = LessonCreationUiState(
                isVisible = true,
                studentQuery = "",
                students = students,
                selectedStudent = null,
                studentGrade = config.studentGrade?.trim().orEmpty(),
                subjects = subjectOptions,
                availableSubjects = subjectOptions,
                selectedSubjectId = null,
                selectedSubjectChips = emptyList(),
                date = roundedStart.toLocalDate(),
                time = roundedStart.toLocalTime(),
                durationMinutes = baseDuration,
                priceCents = basePrice,
                note = config.note.orEmpty(),
                currencySymbol = currencySymbol,
                slotStepMinutes = slotStep,
                origin = config.origin,
                locale = locale,
                zoneId = currentZone,
                recurrenceMode = RecurrenceMode.NONE,
                recurrenceInterval = 1,
                recurrenceDays = emptySet(),
                recurrenceEndEnabled = false,
                recurrenceEndDate = null
            )

            val restored = savedDraft?.restore(baseState, students)
            val refreshed = refreshRecurrence(restored ?: baseState)
            _uiState.value = refreshed
            currentZone = refreshed.zoneId
            saveDraft(refreshed)

            if (restored == null) {
                config.studentId?.let { selectStudent(it, applyDefaults = config.duration == null && config.subjectId == null) }
                config.subjectId?.let { onSubjectSelected(it) }
            } else {
                val keepIds = buildSet {
                    refreshed.selectedSubjectId?.let { add(it) }
                    refreshed.selectedSubjectChips.mapNotNullTo(this) { it.id }
                }
                val available = resolveAvailableSubjects(refreshed.selectedStudent, refreshed.locale, keepIds)
                updateUiState(persist = false) { it.copy(availableSubjects = available) }
            }
        }
    }

    fun dismiss() {
        updateUiState(persist = false) { it.copy(isVisible = false, snackbarMessage = null, showConflictDialog = null) }
        conflictRequest = null
        clearDraft()
    }

    fun prepareForStudentCreation() {
        val state = _uiState.value
        if (!state.isVisible) return
        pendingStudentConfig = LessonCreationConfig(
            start = ZonedDateTime.of(state.date, state.time, currentZone),
            duration = Duration.ofMinutes(state.durationMinutes.toLong()),
            studentId = null,
            studentGrade = state.studentGrade.trim().ifBlank { null },
            subjectId = state.selectedSubjectId,
            note = state.note,
            zoneId = currentZone,
            origin = state.origin
        )
    }

    fun onStudentCreated(studentId: Long): Boolean {
        val pending = pendingStudentConfig ?: return false
        pendingStudentConfig = null
        start(pending.copy(studentId = studentId))
        return true
    }

    fun onStudentQueryChange(query: String) {
        val isCleared = query.isBlank()
        if (isCleared) {
            currentRateHistory = emptyList()
            currentStudentBaseRateCents = null
            currentStudentBaseRateDuration = null
        }

        updateUiState { current ->
            val baseState = current.copy(studentQuery = query)
            if (!isCleared) {
                baseState
            } else {
                val keepSubjectIds = baseState.selectedSubjectChips.mapNotNull { it.id }.toSet()
                baseState.copy(
                    selectedStudent = null,
                    availableSubjects = resolveAvailableSubjects(null, baseState.locale, keepSubjectIds),
                    pricePresets = emptyList()
                )
            }
        }
        viewModelScope.launch {
            val students = loadStudents(query)
            updateUiState(persist = false) { current ->
                if (current.studentQuery == query) {
                    current.copy(students = students)
                } else {
                    current
                }
            }
        }
    }

    fun onStudentGradeChanged(value: String) {
        updateUiState { it.copy(studentGrade = value) }
    }

    fun onStudentSelected(studentId: Long) {
        viewModelScope.launch {
            selectStudent(studentId, applyDefaults = true)
        }
    }

    private suspend fun ensureStudentSelected(state: LessonCreationUiState): StudentOption? {
        val name = state.studentQuery.trim()
        if (name.isEmpty()) return null

        state.students.firstOrNull { it.name.equals(name, ignoreCase = true) }?.let { existing ->
            currentRateHistory = loadRateHistory(existing.id)
            currentStudentBaseRateCents = existing.rateCents
            currentStudentBaseRateDuration = existing.rateCents?.let { _ ->
                state.durationMinutes.takeIf { it > 0 }
            }
            val pricePresets = computePricePresets(state.durationMinutes)
            updateUiState {
                it.copy(
                    selectedStudent = existing,
                    students = mergeStudentOption(it.students, existing),
                    studentQuery = existing.name,
                    studentGrade = existing.grade.orEmpty(),
                    pricePresets = pricePresets
                )
            }
            return existing
        }

        val grade = state.studentGrade.trim().takeIf { it.isNotEmpty() }
        val subjectNames = mutableListOf<String>().apply {
            state.selectedSubjectChips.forEach { chip ->
                val trimmed = chip.name.trim()
                if (trimmed.isNotEmpty()) add(trimmed)
            }
            val pending = state.subjectInput.trim()
            if (pending.isNotEmpty()) add(pending)
            state.selectedSubjectId?.let { subjectId ->
                val option = state.subjects.firstOrNull { it.id == subjectId }
                if (option != null) {
                    val trimmed = option.name.trim()
                    if (trimmed.isNotEmpty()) add(trimmed)
                }
            }
        }
        val distinctSubjects = subjectNames
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinctBy { it.lowercase(state.locale) }
        val subjectsValue = distinctSubjects.joinToString(separator = ", ").takeIf { it.isNotEmpty() }

        val newStudent = Student(
            name = name,
            grade = grade,
            subject = subjectsValue
        )

        val newId = runCatching { studentsRepository.upsert(newStudent) }.getOrElse { error ->
            updateUiState(persist = false) {
                it.copy(
                    snackbarMessage = error.message ?: "Не удалось сохранить ученика"
                )
            }
            return null
        }

        val persisted = studentsRepository.getByIdSafe(newId) ?: newStudent.copy(id = newId)
        val option = persisted.toOption()
        currentRateHistory = emptyList()
        currentStudentBaseRateCents = null
        currentStudentBaseRateDuration = null
        updateUiState {
            it.copy(
                selectedStudent = option,
                students = mergeStudentOption(it.students, option),
                studentQuery = option.name,
                studentGrade = option.grade.orEmpty(),
                pricePresets = emptyList()
            )
        }
        return option
    }

    private suspend fun selectStudent(studentId: Long, applyDefaults: Boolean) {
        val state = _uiState.value
        val selected = state.students.firstOrNull { it.id == studentId }
            ?: studentsRepository.getByIdSafe(studentId)?.toOption()
        if (selected == null) return

        durationEdited = durationEdited && !applyDefaults
        priceEdited = false

        val latest = lessonsRepository.latestLessonForStudent(studentId)
        val subjectId = latest?.subjectId ?: state.selectedSubjectId
        val duration = when {
            durationEdited -> state.durationMinutes
            latest != null -> Duration.between(latest.startAt, latest.endAt).toMinutes().toInt()
            else -> state.durationMinutes
        }
        val studentRate = selected.rateCents?.takeIf { it > 0 }
        val price = when {
            priceEdited -> state.priceCents
            studentRate != null -> studentRate
            latest != null -> latest.priceCents
            else -> state.priceCents
        }

        currentRateHistory = loadRateHistory(studentId)
        currentStudentBaseRateCents = studentRate
        currentStudentBaseRateDuration = studentRate?.let { duration.takeIf { it > 0 } }
        val pricePresets = computePricePresets(duration)

        val keepSubjectIds = mutableSetOf<Long>().apply {
            state.selectedSubjectChips.mapNotNullTo(this) { it.id }
            subjectId?.let { add(it) }
        }
        val availableSubjectOptions = resolveAvailableSubjects(selected, state.locale, keepSubjectIds)
        val resolvedSubjectId = when {
            subjectId != null && availableSubjectOptions.any { it.id == subjectId } -> subjectId
            applyDefaults && availableSubjectOptions.isNotEmpty() -> availableSubjectOptions.first().id
            else -> subjectId
        }
        val firstStudentSubject = selected.subjects
            .map { it.trim() }
            .firstOrNull { it.isNotEmpty() }
            .orEmpty()
        val resolvedSubjectName = resolvedSubjectId?.let { id ->
            availableSubjectOptions.firstOrNull { it.id == id }?.name
        } ?: firstStudentSubject

        updateUiState {
            val sanitizedChips = it.selectedSubjectChips.filter { chip ->
                chip.id == null || availableSubjectOptions.any { option -> option.id == chip.id }
            }
            val nextChips = if (applyDefaults) emptyList() else sanitizedChips
            it.copy(
                selectedStudent = selected,
                students = mergeStudentOption(it.students, selected),
                studentQuery = selected.name,
                studentGrade = selected.grade.orEmpty(),
                selectedSubjectId = resolvedSubjectId,
                selectedSubjectChips = nextChips,
                durationMinutes = duration,
                priceCents = price,
                pricePresets = pricePresets,
                availableSubjects = availableSubjectOptions,
                subjectInput = if (applyDefaults || it.selectedStudent?.id != selected.id) {
                    ""
                } else {
                    it.subjectInput
                }
            )
        }

        if (applyDefaults) {
            when {
                resolvedSubjectId != null -> onSubjectSelected(resolvedSubjectId)
                resolvedSubjectName.isNotBlank() -> onSubjectSuggestionToggled(resolvedSubjectName)
            }
        }
    }

    private fun resolveAvailableSubjects(
        student: StudentOption?,
        locale: Locale,
        keepSubjectIds: Set<Long> = emptySet()
    ): List<SubjectOption> {
        if (cachedSubjects.isEmpty()) return emptyList()
        val allOptions = cachedSubjects.map { it.toOption() }
        val subjects = student?.subjects.orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (subjects.isEmpty()) return includeSubjectIfNeeded(allOptions, allOptions, keepSubjectIds)

        val normalized = subjects.map { it.lowercase(locale) }.toSet()
        val filtered = cachedSubjects.filter { preset ->
            normalized.contains(preset.name.lowercase(locale))
        }
        val options = (if (filtered.isEmpty()) cachedSubjects else filtered).map { it.toOption() }
        return includeSubjectIfNeeded(options, allOptions, keepSubjectIds)
    }

    private fun includeSubjectIfNeeded(
        primary: List<SubjectOption>,
        allOptions: List<SubjectOption>,
        keepSubjectIds: Set<Long>
    ): List<SubjectOption> {
        if (keepSubjectIds.isEmpty()) return primary
        val missing = keepSubjectIds.filterNot { id -> primary.any { it.id == id } }
        if (missing.isEmpty()) return primary
        val extras = missing.mapNotNull { id -> allOptions.firstOrNull { it.id == id } }
        if (extras.isEmpty()) return primary
        return primary + extras
    }

    fun onSubjectSelected(subjectId: Long) {
        val state = _uiState.value
        val option = state.subjects.firstOrNull { it.id == subjectId } ?: return
        val existingIndex = state.selectedSubjectChips.indexOfFirst { it.id == subjectId }
        if (existingIndex >= 0) {
            val updatedChips = state.selectedSubjectChips.toMutableList().apply { removeAt(existingIndex) }
            val newPrimary = if (state.selectedSubjectId == subjectId) {
                updatedChips.firstOrNull { it.id != null }?.id
            } else {
                state.selectedSubjectId
            }
            updateUiState {
                it.copy(
                    selectedSubjectChips = updatedChips,
                    selectedSubjectId = newPrimary
                )
            }
            return
        }
        addSubjectChip(option, state)
    }

    fun onSubjectSuggestionToggled(name: String) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) {
            updateUiState { it.copy(subjectInput = "") }
            return
        }
        val state = _uiState.value
        val existingIndex = state.selectedSubjectChips.indexOfFirst { chip ->
            chip.id == null && chip.name.equals(trimmed, ignoreCase = true)
        }
        if (existingIndex >= 0) {
            val updated = state.selectedSubjectChips.toMutableList().apply { removeAt(existingIndex) }
            updateUiState { it.copy(selectedSubjectChips = updated, subjectInput = "") }
            return
        }
        if (state.selectedSubjectChips.any { chip -> chip.name.equals(trimmed, ignoreCase = true) }) {
            updateUiState { it.copy(subjectInput = "") }
            return
        }
        updateUiState { current ->
            current.copy(
                selectedSubjectChips = current.selectedSubjectChips + SelectedSubjectChip(
                    id = null,
                    name = trimmed,
                    colorArgb = null
                ),
                subjectInput = ""
            )
        }
    }

    fun onSubjectChipRemoved(id: Long?, name: String) {
        val state = _uiState.value
        val updated = if (id != null) {
            state.selectedSubjectChips.filterNot { it.id == id }
        } else {
            val normalized = name.trim()
            state.selectedSubjectChips.filterNot { chip ->
                chip.id == null && chip.name.equals(normalized, ignoreCase = true)
            }
        }
        val newPrimary = if (id != null && state.selectedSubjectId == id) {
            updated.firstOrNull { it.id != null }?.id
        } else {
            state.selectedSubjectId
        }
        updateUiState { it.copy(selectedSubjectChips = updated, selectedSubjectId = newPrimary) }
    }

    fun onSubjectInputChanged(value: String) {
        updateUiState { it.copy(subjectInput = value) }
    }

    private fun addSubjectChip(option: SubjectOption, state: LessonCreationUiState) {
        if (state.selectedSubjectChips.any { it.id == option.id }) {
            updateUiState { it.copy(subjectInput = "") }
            return
        }
        val newDuration = if (durationEdited) state.durationMinutes else option.durationMinutes
        val newPrice = if (priceEdited) state.priceCents else option.defaultPriceCents
        updateUiState {
            it.copy(
                selectedSubjectId = option.id,
                selectedSubjectChips = it.selectedSubjectChips + option.toChip(),
                subjectInput = "",
                durationMinutes = newDuration,
                priceCents = newPrice,
                pricePresets = computePricePresets(newDuration)
            )
        }
    }

    fun onDateSelected(date: LocalDate) {
        updateUiState(recalculateRecurrence = true) { state ->
            val previousDay = state.date.dayOfWeek
            val updatedDays = when {
                state.isRecurring && state.recurrenceMode != RecurrenceMode.MONTHLY_BY_DOW -> {
                    when {
                        state.recurrenceDays.isEmpty() -> setOf(date.dayOfWeek)
                        state.recurrenceDays.size == 1 && state.recurrenceDays.contains(previousDay) -> setOf(date.dayOfWeek)
                        else -> state.recurrenceDays
                    }
                }
                else -> state.recurrenceDays
            }
            state.copy(date = date, recurrenceDays = updatedDays)
        }
    }

    fun onTimeSelected(time: LocalTime) {
        val rounded = roundTimeToStep(time, _uiState.value.slotStepMinutes)
        updateUiState(recalculateRecurrence = true) { it.copy(time = rounded) }
    }

    fun onDurationChanged(value: Int) {
        durationEdited = true
        val sanitized = value.coerceAtLeast(0)
        updateUiState {
            it.copy(
                durationMinutes = sanitized,
                pricePresets = computePricePresets(sanitized)
            )
        }
    }

    fun onPriceChanged(value: Int) {
        priceEdited = true
        updateUiState { it.copy(priceCents = value.coerceAtLeast(0)) }
    }

    fun onNoteChanged(value: String) {
        updateUiState { it.copy(note = value) }
    }

    fun onRecurrenceEnabledChanged(enabled: Boolean) {
        updateUiState(recalculateRecurrence = true) { state ->
            if (enabled) {
                val targetMode = if (state.recurrenceMode == RecurrenceMode.NONE) {
                    RecurrenceMode.WEEKLY
                } else {
                    state.recurrenceMode
                }
                val days = when (targetMode) {
                    RecurrenceMode.NONE, RecurrenceMode.MONTHLY_BY_DOW -> emptySet()
                    else -> if (state.recurrenceDays.isEmpty()) setOf(state.date.dayOfWeek) else state.recurrenceDays
                }
                state.copy(
                    isRecurring = true,
                    recurrenceMode = targetMode,
                    recurrenceDays = days,
                    recurrenceEndEnabled = true,
                    recurrenceEndDate = state.recurrenceEndDate ?: defaultRecurrenceEnd(state.date)
                )
            } else {
                state.copy(
                    isRecurring = false,
                    recurrenceEndEnabled = false,
                    recurrenceEndDate = null
                )
            }
        }
    }

    fun onRecurrenceModeSelected(mode: RecurrenceMode) {
        updateUiState(recalculateRecurrence = true) { state ->
            val normalizedInterval = when (mode) {
                RecurrenceMode.CUSTOM_WEEKS -> max(2, state.recurrenceInterval)
                RecurrenceMode.MONTHLY_BY_DOW -> state.recurrenceInterval.coerceAtLeast(1)
                else -> state.recurrenceInterval
            }
            val days = when (mode) {
                RecurrenceMode.NONE -> emptySet()
                RecurrenceMode.MONTHLY_BY_DOW -> emptySet()
                else -> if (state.recurrenceDays.isEmpty()) setOf(state.date.dayOfWeek) else state.recurrenceDays
            }
            state.copy(
                isRecurring = mode != RecurrenceMode.NONE,
                recurrenceMode = mode,
                recurrenceInterval = normalizedInterval,
                recurrenceDays = days,
                recurrenceEndEnabled = state.recurrenceEndEnabled && mode != RecurrenceMode.NONE
            )
        }
    }

    fun onRecurrenceDayToggled(day: DayOfWeek) {
        updateUiState(recalculateRecurrence = true) { state ->
            if (!state.isRecurring || state.recurrenceMode == RecurrenceMode.NONE || state.recurrenceMode == RecurrenceMode.MONTHLY_BY_DOW) {
                return@updateUiState state
            }
            val current = state.recurrenceDays
            val updated = if (current.contains(day)) current - day else current + day
            val normalized = if (updated.isEmpty()) setOf(state.date.dayOfWeek) else updated
            state.copy(recurrenceDays = normalized)
        }
    }

    fun onRecurrenceIntervalChanged(value: Int) {
        updateUiState(recalculateRecurrence = true) { state ->
            state.copy(recurrenceInterval = value.coerceAtLeast(0))
        }
    }

    fun onRecurrenceEndEnabledChanged(enabled: Boolean) {
        updateUiState(recalculateRecurrence = true) { state ->
            val effective = enabled && state.isRecurring && state.recurrenceMode != RecurrenceMode.NONE
            val endDate = if (effective) state.recurrenceEndDate ?: defaultRecurrenceEnd(state.date) else null
            state.copy(
                recurrenceEndEnabled = effective,
                recurrenceEndDate = endDate
            )
        }
    }

    fun onRecurrenceEndDateSelected(date: LocalDate) {
        updateUiState(recalculateRecurrence = true) { state ->
            val normalized = if (date.isBefore(state.date)) state.date else date
            state.copy(
                recurrenceEndEnabled = true,
                recurrenceEndDate = normalized
            )
        }
    }

    fun submit() {
        viewModelScope.launch { attemptSubmit(force = false) }
    }

    fun confirmConflict() {
        viewModelScope.launch { conflictRequest?.let { createLesson(it) } }
    }

    fun dismissConflict() {
        conflictRequest = null
        updateUiState(persist = false) { it.copy(showConflictDialog = null) }
    }

    fun consumeSnackbar() {
        updateUiState(persist = false) { it.copy(snackbarMessage = null) }
    }

    private suspend fun attemptSubmit(force: Boolean) {
        var state = _uiState.value
        val errors = mutableMapOf<LessonCreationField, String>()
        val student = state.selectedStudent ?: run {
            val created = ensureStudentSelected(state)
            if (created == null) {
                errors[LessonCreationField.STUDENT] = "Выберите ученика"
                updateUiState(persist = false) { it.copy(errors = errors) }
                return
            }
            created
        }
        state = _uiState.value
        if (state.durationMinutes <= 0) {
            errors[LessonCreationField.DURATION] = "Длительность должна быть больше 0"
        } else if (state.durationMinutes % state.slotStepMinutes != 0) {
            errors[LessonCreationField.DURATION] = "Длительность кратна шагу ${state.slotStepMinutes} мин"
        }
        if (state.priceCents < 0) {
            errors[LessonCreationField.PRICE] = "Стоимость не может быть отрицательной"
        }

        val start = ZonedDateTime.of(state.date, state.time, currentZone)
        val end = start.plusMinutes(state.durationMinutes.toLong())
        if (end.toLocalDate().isAfter(state.date)) {
            errors[LessonCreationField.TIME] = "Урок должен закончиться в тот же день"
        }

        if (errors.isNotEmpty()) {
            updateUiState(persist = false) { it.copy(errors = errors) }
            return
        }

        val normalizedQuery = state.subjectInput.trim()
        val selectedChips = state.selectedSubjectChips
        val primaryName = selectedChips.firstOrNull { it.id == state.selectedSubjectId }?.name
        val additionalNames = selectedChips
            .filterNot { chip -> chip.id != null && chip.id == state.selectedSubjectId }
            .map { it.name }
        val titleParts = mutableListOf<String>().apply {
            additionalNames.mapNotNull { name ->
                name.trim().takeIf { it.isNotEmpty() }
            }.forEach { add(it) }
            normalizedQuery.takeIf { it.isNotEmpty() && (primaryName == null || !primaryName.equals(it, ignoreCase = true)) }?.let { add(it) }
        }
        val distinctTitleParts = titleParts.distinctBy { it.lowercase(state.locale) }
        val title = when {
            distinctTitleParts.isEmpty() -> null
            distinctTitleParts.size == 1 -> distinctTitleParts.first()
            else -> distinctTitleParts.joinToString(separator = ", ")
        }

        saveDraft(state)
        val draft = state.toLessonDraft()
        val recurrence = draft.repeat?.toRecurrenceRequest(
            mode = draft.recurrenceMode,
            baseDate = state.date,
            baseTime = state.time,
            zoneId = state.zoneId
        )
        val request = LessonCreateRequest(
            studentId = student.id,
            subjectId = draft.selectedSubjectId,
            title = title,
            startAt = start.toInstant(),
            endAt = end.toInstant(),
            priceCents = draft.priceCents,
            note = draft.note.takeUnless { it.isBlank() },
            recurrence = recurrence
        )

        if (!force) {
            val conflicts = lessonsRepository.findConflicts(request.startAt, request.endAt)
            if (conflicts.isNotEmpty()) {
                conflictRequest = request
                updateUiState(persist = false) {
                    it.copy(
                        errors = emptyMap(),
                        showConflictDialog = ConflictInfo(
                            conflicts = conflicts.map { conflict -> conflict.toConflictLesson(currentZone) },
                            start = start,
                            end = end
                        )
                    )
                }
                return
            }
        }

        createLesson(request)
    }

    private suspend fun createLesson(request: LessonCreateRequest) {
        updateUiState { it.copy(isSubmitting = true, errors = emptyMap()) }
        runCatching {
            lessonsRepository.create(request)
        }.onSuccess {
            val start = ZonedDateTime.ofInstant(request.startAt, currentZone)
            val end = ZonedDateTime.ofInstant(request.endAt, currentZone)
            val durationMinutes = Duration.between(request.startAt, request.endAt).toMinutes().toInt()
            if (durationMinutes > 0) {
                val usage = RateUsage(
                    priceCents = request.priceCents,
                    durationMinutes = durationMinutes,
                    timestamp = request.startAt
                )
                rateHistoryCache[request.studentId] =
                    (rateHistoryCache[request.studentId] ?: emptyList()) + usage
                if (_uiState.value.selectedStudent?.id == request.studentId) {
                    currentRateHistory = currentRateHistory + usage
                }
            }
            val formatter = DateTimeFormatter.ofPattern("dd MMM HH:mm", _uiState.value.locale)
            val message = "Урок добавлен на ${start.format(formatter)}–${end.toLocalTime().format(DateTimeFormatter.ofPattern("HH:mm"))}"
            updateUiState(persist = false) {
                it.copy(
                    isSubmitting = false,
                    isVisible = false,
                    snackbarMessage = message,
                    showConflictDialog = null,
                    selectedStudent = null,
                    studentQuery = "",
                    pricePresets = emptyList()
                )
            }
            currentRateHistory = emptyList()
            currentStudentBaseRateCents = null
            currentStudentBaseRateDuration = null
            durationEdited = false
            priceEdited = false
            conflictRequest = null
            clearDraft()
            _events.tryEmit(
                LessonCreationEvent.Created(
                    start = start,
                    studentId = request.studentId
                )
            )
        }.onFailure { error ->
            updateUiState(persist = false) {
                it.copy(
                    isSubmitting = false,
                    snackbarMessage = error.message ?: "Не удалось сохранить занятие"
                )
            }
        }
    }

    private fun updateUiState(
        recalculateRecurrence: Boolean = false,
        persist: Boolean = true,
        block: (LessonCreationUiState) -> LessonCreationUiState
    ) {
        _uiState.update { current ->
            val updated = block(current)
            val finalState = if (recalculateRecurrence) refreshRecurrence(updated) else updated
            if (persist) {
                saveDraft(finalState)
            }
            finalState
        }
    }

    private fun refreshRecurrence(state: LessonCreationUiState): LessonCreationUiState {
        val isRecurring = state.isRecurring && state.recurrenceMode != RecurrenceMode.NONE
        val baseDay = state.date.dayOfWeek
        val sanitizedInterval = state.recurrenceInterval.coerceAtLeast(1)
        val effectiveInterval = when (state.recurrenceMode) {
            RecurrenceMode.CUSTOM_WEEKS -> sanitizedInterval
            RecurrenceMode.MONTHLY_BY_DOW -> sanitizedInterval
            RecurrenceMode.WEEKLY -> 1
            RecurrenceMode.NONE -> sanitizedInterval
        }
        val normalizedDays = when {
            !isRecurring -> emptySet()
            state.recurrenceMode == RecurrenceMode.MONTHLY_BY_DOW -> emptySet()
            else -> {
                val candidate = if (state.recurrenceDays.isEmpty()) setOf(baseDay) else state.recurrenceDays
                if (candidate.isEmpty()) setOf(baseDay) else candidate
            }
        }
        val endEnabled = state.recurrenceEndEnabled && isRecurring
        val untilDate = if (endEnabled) {
            val candidate = state.recurrenceEndDate ?: defaultRecurrenceEnd(state.date)
            if (candidate.isBefore(state.date)) state.date else candidate
        } else {
            null
        }
        val label = if (isRecurring) {
            val start = ZonedDateTime.of(state.date, state.time, currentZone)
            val frequency = when (state.recurrenceMode) {
                RecurrenceMode.MONTHLY_BY_DOW -> RecurrenceFrequency.MONTHLY_BY_DOW
                RecurrenceMode.WEEKLY, RecurrenceMode.CUSTOM_WEEKS -> RecurrenceFrequency.WEEKLY
                RecurrenceMode.NONE -> RecurrenceFrequency.WEEKLY
            }
            val days = if (state.recurrenceMode == RecurrenceMode.MONTHLY_BY_DOW) {
                emptyList()
            } else {
                normalizedDays.toList().sortedBy { it.value }
            }
            RecurrenceLabelFormatter.format(frequency, effectiveInterval, days, start)
        } else {
            null
        }
        val nextInterval = when (state.recurrenceMode) {
            RecurrenceMode.CUSTOM_WEEKS, RecurrenceMode.MONTHLY_BY_DOW -> effectiveInterval
            else -> sanitizedInterval
        }
        return state.copy(
            isRecurring = isRecurring,
            recurrenceDays = normalizedDays,
            recurrenceEndEnabled = endEnabled,
            recurrenceEndDate = untilDate,
            recurrenceLabel = label,
            recurrenceInterval = nextInterval
        )
    }

    private fun LessonCreationUiState.toLessonDraft(): LessonDraft {
        val normalizedDays = when {
            !isRecurring || recurrenceMode == RecurrenceMode.NONE -> emptySet()
            recurrenceMode == RecurrenceMode.MONTHLY_BY_DOW -> emptySet()
            recurrenceDays.isEmpty() -> setOf(date.dayOfWeek)
            else -> recurrenceDays
        }
        val untilEpochDay = if (recurrenceEndEnabled && recurrenceMode != RecurrenceMode.NONE) {
            recurrenceEndDate?.toEpochDay()
        } else {
            null
        }
        val repeatRule = if (isRecurring && recurrenceMode != RecurrenceMode.NONE) {
            RepeatRule(
                frequency = when (recurrenceMode) {
                    RecurrenceMode.MONTHLY_BY_DOW -> RecurrenceFrequency.MONTHLY_BY_DOW
                    RecurrenceMode.WEEKLY, RecurrenceMode.CUSTOM_WEEKS -> RecurrenceFrequency.WEEKLY
                    RecurrenceMode.NONE -> RecurrenceFrequency.WEEKLY
                },
                interval = when (recurrenceMode) {
                    RecurrenceMode.CUSTOM_WEEKS -> recurrenceInterval.coerceAtLeast(1)
                    RecurrenceMode.MONTHLY_BY_DOW -> recurrenceInterval.coerceAtLeast(1)
                    RecurrenceMode.WEEKLY -> 1
                    RecurrenceMode.NONE -> recurrenceInterval
                },
                daysOfWeek = normalizedDays.map { it.value }.toSet(),
                untilEpochDay = untilEpochDay
            )
        } else {
            null
        }

        return LessonDraft(
            isVisible = isVisible,
            studentQuery = studentQuery,
            selectedStudentId = selectedStudent?.id,
            studentGrade = studentGrade,
            selectedSubjectId = selectedSubjectId,
            selectedSubjectChips = selectedSubjectChips.map { it.toDraft() },
            subjectInput = subjectInput,
            dateEpochDay = date.toEpochDay(),
            timeMinutes = time.toSecondOfDay() / 60,
            durationMinutes = durationMinutes,
            priceCents = priceCents,
            note = note,
            origin = origin,
            zoneId = zoneId.id,
            recurrenceMode = recurrenceMode,
            recurrenceInterval = recurrenceInterval,
            recurrenceDays = recurrenceDays.map { it.value }.toSet(),
            recurrenceEndEnabled = recurrenceEndEnabled,
            recurrenceEndDateEpochDay = recurrenceEndDate?.toEpochDay(),
            repeat = repeatRule
        )
    }

    private fun LessonDraft.restore(
        base: LessonCreationUiState,
        students: List<StudentOption>
    ): LessonCreationUiState {
        val resolvedZone = runCatching { ZoneId.of(zoneId) }.getOrDefault(base.zoneId)
        val resolvedDate = runCatching { LocalDate.ofEpochDay(dateEpochDay) }.getOrDefault(base.date)
        val resolvedTime = runCatching { LocalTime.ofSecondOfDay(timeMinutes.toLong() * 60) }.getOrDefault(base.time)
        val student = selectedStudentId?.let { id -> students.firstOrNull { it.id == id } }
        val repeatDays = repeat?.daysOfWeek?.mapNotNull { value -> runCatching(DayOfWeek::of).getOrNull(value) }?.toSet()
        val draftDays = recurrenceDays.mapNotNull { value -> runCatching(DayOfWeek::of).getOrNull(value) }.toSet()
        val resolvedDays = repeatDays?.takeIf { it.isNotEmpty() } ?: draftDays
        val untilDate = recurrenceEndDateEpochDay?.let(LocalDate::ofEpochDay)
        val recurring = repeat != null && recurrenceMode != RecurrenceMode.NONE

        return base.copy(
            isVisible = isVisible,
            studentQuery = studentQuery,
            students = students,
            selectedStudent = student,
            studentGrade = studentGrade,
            selectedSubjectId = selectedSubjectId,
            selectedSubjectChips = selectedSubjectChips.map { it.toChip() },
            subjectInput = subjectInput,
            date = resolvedDate,
            time = resolvedTime,
            durationMinutes = durationMinutes,
            priceCents = priceCents,
            note = note,
            origin = origin,
            zoneId = resolvedZone,
            isRecurring = recurring,
            recurrenceMode = recurrenceMode,
            recurrenceInterval = recurrenceInterval,
            recurrenceDays = resolvedDays,
            recurrenceEndEnabled = recurring && recurrenceEndEnabled,
            recurrenceEndDate = untilDate
        )
    }

    private fun RepeatRule.toRecurrenceRequest(
        mode: RecurrenceMode,
        baseDate: LocalDate,
        baseTime: LocalTime,
        zoneId: ZoneId
    ): RecurrenceCreateRequest {
        val start = ZonedDateTime.of(baseDate, baseTime, zoneId)
        val normalizedDays = if (mode == RecurrenceMode.MONTHLY_BY_DOW) {
            emptyList()
        } else {
            val selection = daysOfWeek.mapNotNull { value -> runCatching(DayOfWeek::of).getOrNull(value) }
                .toSet()
                .takeIf { it.isNotEmpty() }
                ?: setOf(start.dayOfWeek)
            selection.toList().sortedBy { it.value }
        }
        val untilInstant = untilEpochDay?.let { epochDay ->
            val candidate = LocalDate.ofEpochDay(epochDay)
            val normalized = if (candidate.isBefore(baseDate)) baseDate else candidate
            ZonedDateTime.of(normalized, baseTime, zoneId).toInstant()
        }

        return RecurrenceCreateRequest(
            frequency = frequency,
            interval = when (mode) {
                RecurrenceMode.CUSTOM_WEEKS -> interval.coerceAtLeast(1)
                RecurrenceMode.MONTHLY_BY_DOW -> interval.coerceAtLeast(1)
                RecurrenceMode.WEEKLY -> 1
                RecurrenceMode.NONE -> interval
            },
            daysOfWeek = normalizedDays,
            until = untilInstant,
            timezone = zoneId
        )
    }

    private fun SelectedSubjectChip.toDraft(): SelectedSubjectChipDraft =
        SelectedSubjectChipDraft(id = id, name = name, colorArgb = colorArgb)

    private fun SelectedSubjectChipDraft.toChip(): SelectedSubjectChip =
        SelectedSubjectChip(id = id, name = name, colorArgb = colorArgb)

    private fun defaultRecurrenceEnd(startDate: LocalDate): LocalDate {
        val currentYear = startDate.year
        val thisYearEnd = LocalDate.of(currentYear, Month.JUNE, 30)
        return if (startDate.isAfter(thisYearEnd)) {
            LocalDate.of(currentYear + 1, Month.JUNE, 30)
        } else {
            thisYearEnd
        }
    }

    private suspend fun loadStudents(query: String): List<StudentOption> {
        val normalized = query.trim()
        val list = runCatching {
            if (normalized.isEmpty()) {
                studentsRepository.allActive()
            } else {
                studentsRepository.searchActive(normalized)
            }
        }.getOrDefault(emptyList())
        return list.sortedBy { it.name.lowercase(Locale.getDefault()) }.map { it.toOption() }
    }

    private suspend fun loadRateHistory(studentId: Long): List<RateUsage> {
        rateHistoryCache[studentId]?.let { return it }
        val lessons = runCatching { lessonsRepository.observeByStudent(studentId).first() }
            .getOrDefault(emptyList())
        val history = lessons.mapNotNull { lesson ->
            val durationMinutes = Duration.between(lesson.startAt, lesson.endAt).toMinutes().toInt()
            if (durationMinutes <= 0) {
                null
            } else {
                RateUsage(
                    priceCents = lesson.priceCents,
                    durationMinutes = durationMinutes,
                    timestamp = lesson.startAt
                )
            }
        }
        rateHistoryCache[studentId] = history
        return history
    }

    private fun computePricePresets(durationMinutes: Int): List<Int> {
        if (durationMinutes <= 0) return emptyList()
        val aggregates = mutableMapOf<Int, RateAggregate>()
        currentRateHistory.forEach { usage ->
            val scaled = scalePrice(usage.priceCents, usage.durationMinutes, durationMinutes) ?: return@forEach
            if (scaled <= 0) return@forEach
            val aggregate = aggregates.getOrPut(scaled) { RateAggregate() }
            aggregate.count += 1
            if (usage.timestamp > aggregate.lastUsed) {
                aggregate.lastUsed = usage.timestamp
            }
        }
        val baseRate = currentStudentBaseRateCents
        val baseDuration = currentStudentBaseRateDuration
        if (baseRate != null && baseDuration != null && baseDuration > 0) {
            val scaled = scalePrice(baseRate, baseDuration, durationMinutes)
            if (scaled != null && scaled > 0) {
                val aggregate = aggregates.getOrPut(scaled) { RateAggregate() }
                aggregate.count += 1
                if (aggregate.lastUsed < Instant.MAX) {
                    aggregate.lastUsed = Instant.MAX
                }
            }
        }
        if (aggregates.isEmpty()) return emptyList()
        return aggregates.entries
            .sortedWith(
                compareByDescending<Map.Entry<Int, RateAggregate>> { it.value.count }
                    .thenByDescending { it.value.lastUsed }
                    .thenBy { it.key }
            )
            .map { it.key }
            .take(4)
    }

    private fun scalePrice(priceCents: Int, fromMinutes: Int, toMinutes: Int): Int? {
        if (priceCents <= 0 || fromMinutes <= 0 || toMinutes <= 0) return null
        val scaledRub = BigDecimal(priceCents)
            .divide(BigDecimal(100))
            .multiply(BigDecimal(toMinutes))
            .divide(BigDecimal(fromMinutes), 0, RoundingMode.HALF_UP)
        return runCatching { scaledRub.multiply(BigDecimal(100)).intValueExact() }.getOrNull()
    }
}

private data class RateUsage(
    val priceCents: Int,
    val durationMinutes: Int,
    val timestamp: Instant
)

private data class RateAggregate(
    var count: Int = 0,
    var lastUsed: Instant = Instant.MIN
)

private fun LessonDetails.toConflictLesson(zoneId: ZoneId): ConflictLesson {
    val start = ZonedDateTime.ofInstant(startAt, zoneId)
    val end = ZonedDateTime.ofInstant(endAt, zoneId)
    return ConflictLesson(
        id = id,
        studentName = studentName,
        start = start,
        end = end
    )
}

private fun SubjectPreset.toOption(): SubjectOption = SubjectOption(
    id = id,
    name = name,
    colorArgb = colorArgb,
    durationMinutes = durationMinutes,
    defaultPriceCents = defaultPriceCents
)

private fun Student.toOption(): StudentOption {
    val subjects = subject
        ?.split(',', ';', '\n')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.distinct()
        ?: emptyList()

    return StudentOption(
        id = id,
        name = name,
        rateCents = rateCents,
        grade = grade?.takeIf { it.isNotBlank() }?.trim(),
        subjects = subjects
    )
}


private fun roundToStep(start: ZonedDateTime, stepMinutes: Int): ZonedDateTime {
    val minute = start.minute
    val remainder = minute % stepMinutes
    val adjust = if (remainder == 0) 0 else stepMinutes - remainder
    return start.plusMinutes(adjust.toLong()).withSecond(0).withNano(0)
}

private fun roundTimeToStep(time: LocalTime, stepMinutes: Int): LocalTime {
    val remainder = time.minute % stepMinutes
    val adjust = if (remainder == 0) 0 else stepMinutes - remainder
    val adjusted = time.plusMinutes(adjust.toLong())
    return adjusted.withSecond(0).withNano(0)
}

private suspend fun StudentsRepository.getByIdSafe(id: Long): Student? = runCatching { getById(id) }.getOrNull()

private fun mergeStudentOption(options: List<StudentOption>, selected: StudentOption): List<StudentOption> {
    val filtered = options.filterNot { it.id == selected.id }
    return listOf(selected) + filtered
}

private fun SubjectOption.toChip(): SelectedSubjectChip =
    SelectedSubjectChip(id = id, name = name, colorArgb = colorArgb)
