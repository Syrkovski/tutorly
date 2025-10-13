package com.tutorly.ui.lessoncreation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tutorly.domain.model.LessonCreateRequest
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.domain.repo.SubjectPresetsRepository
import com.tutorly.domain.repo.UserSettingsRepository
import com.tutorly.models.Student
import com.tutorly.models.SubjectPreset
import dagger.hilt.android.lifecycle.HiltViewModel
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import kotlin.math.max
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LessonCreationViewModel @Inject constructor(
    private val lessonsRepository: LessonsRepository,
    private val studentsRepository: StudentsRepository,
    private val subjectPresetsRepository: SubjectPresetsRepository,
    private val userSettingsRepository: UserSettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LessonCreationUiState())
    val uiState: StateFlow<LessonCreationUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<LessonCreationEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

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

    init {
        viewModelScope.launch {
            cachedSubjects = subjectPresetsRepository.all()
            _uiState.update { state ->
                val options = cachedSubjects.map { it.toOption() }
                val selected = state.selectedSubjectId?.takeIf { id -> options.any { it.id == id } }
                state.copy(
                    subjects = options,
                    availableSubjects = resolveAvailableSubjects(state.selectedStudent, state.locale, selected),
                    selectedSubjectId = selected
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
            val zone = config.zoneId ?: config.start?.zone ?: ZoneId.systemDefault()
            val start = config.start ?: ZonedDateTime.now(zone)
            currentZone = zone
            val slotStep = max(5, settings.slotStepMinutes)
            val roundedStart = roundToStep(start, slotStep)
            val baseDuration = config.duration?.toMinutes()?.toInt()
                ?: settings.defaultLessonDurationMinutes
            val basePrice = settings.defaultLessonPriceCents

            durationEdited = config.duration != null
            priceEdited = false
            currentRateHistory = emptyList()
            currentStudentBaseRateCents = null
            currentStudentBaseRateDuration = null

            val students = loadStudents("")
            val subjectOptions = cachedSubjects.map { it.toOption() }

            _uiState.value = LessonCreationUiState(
                isVisible = true,
                studentQuery = "",
                students = students,
                selectedStudent = null,
                subjects = subjectOptions,
                availableSubjects = subjectOptions,
                selectedSubjectId = null,
                date = roundedStart.toLocalDate(),
                time = roundedStart.toLocalTime(),
                durationMinutes = baseDuration,
                priceCents = basePrice,
                note = config.note.orEmpty(),
                currencySymbol = currencySymbol,
                slotStepMinutes = slotStep,
                origin = config.origin,
                locale = locale,
                zoneId = currentZone
            )

            config.studentId?.let { selectStudent(it, applyDefaults = config.duration == null && config.subjectId == null) }
            config.subjectId?.let { onSubjectSelected(it) }
        }
    }

    fun dismiss() {
        _uiState.update { it.copy(isVisible = false, snackbarMessage = null, showConflictDialog = null) }
        conflictRequest = null
    }

    fun prepareForStudentCreation() {
        val state = _uiState.value
        if (!state.isVisible) return
        pendingStudentConfig = LessonCreationConfig(
            start = ZonedDateTime.of(state.date, state.time, currentZone),
            duration = Duration.ofMinutes(state.durationMinutes.toLong()),
            studentId = null,
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
        _uiState.update { it.copy(studentQuery = query) }
        viewModelScope.launch {
            val students = loadStudents(query)
            _uiState.update { current ->
                if (current.studentQuery == query) {
                    current.copy(students = students)
                } else {
                    current
                }
            }
        }
    }

    fun onStudentSelected(studentId: Long) {
        viewModelScope.launch {
            selectStudent(studentId, applyDefaults = true)
        }
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

        val availableSubjectOptions = resolveAvailableSubjects(selected, state.locale, subjectId)
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

        _uiState.update {
            it.copy(
                selectedStudent = selected,
                students = mergeStudentOption(it.students, selected),
                studentQuery = selected.name,
                selectedSubjectId = resolvedSubjectId,
                durationMinutes = duration,
                priceCents = price,
                pricePresets = pricePresets,
                availableSubjects = availableSubjectOptions,
                subjectInput = if (applyDefaults || it.selectedStudent?.id != selected.id) {
                    resolvedSubjectName
                } else {
                    it.subjectInput.ifBlank { resolvedSubjectName }
                }
            )
        }

        if (applyDefaults) {
            if (resolvedSubjectId != null) {
                onSubjectSelected(resolvedSubjectId)
            } else if (resolvedSubjectName.isNotBlank()) {
                onSubjectInputChanged(resolvedSubjectName)
            }
        }
    }

    private fun resolveAvailableSubjects(
        student: StudentOption?,
        locale: Locale,
        keepSubjectId: Long? = null
    ): List<SubjectOption> {
        if (cachedSubjects.isEmpty()) return emptyList()
        val allOptions = cachedSubjects.map { it.toOption() }
        val subjects = student?.subjects.orEmpty()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (subjects.isEmpty()) return includeSubjectIfNeeded(allOptions, allOptions, keepSubjectId)

        val normalized = subjects.map { it.lowercase(locale) }.toSet()
        val filtered = cachedSubjects.filter { preset ->
            normalized.contains(preset.name.lowercase(locale))
        }
        val options = (if (filtered.isEmpty()) cachedSubjects else filtered).map { it.toOption() }
        return includeSubjectIfNeeded(options, allOptions, keepSubjectId)
    }

    private fun includeSubjectIfNeeded(
        primary: List<SubjectOption>,
        allOptions: List<SubjectOption>,
        keepSubjectId: Long?
    ): List<SubjectOption> {
        if (keepSubjectId == null || primary.any { it.id == keepSubjectId }) {
            return primary
        }
        val fallback = allOptions.firstOrNull { it.id == keepSubjectId } ?: return primary
        return primary + fallback
    }

    fun onSubjectSelected(subjectId: Long?) {
        val state = _uiState.value
        if (subjectId == null) {
            _uiState.update { it.copy(selectedSubjectId = null) }
            return
        }
        val subject = state.subjects.firstOrNull { it.id == subjectId }
        if (subject == null) {
            _uiState.update { it.copy(selectedSubjectId = null) }
            return
        }
        applySubjectOption(subject, state)
    }

    fun onSubjectInputChanged(value: String) {
        val state = _uiState.value
        val normalized = value.trim()
        if (normalized.isEmpty()) {
            _uiState.update { it.copy(subjectInput = value, selectedSubjectId = null) }
            return
        }
        val match = state.availableSubjects.firstOrNull { it.name.equals(normalized, ignoreCase = true) }
            ?: state.subjects.firstOrNull { it.name.equals(normalized, ignoreCase = true) }
        if (match != null) {
            applySubjectOption(match, state)
        } else {
            _uiState.update {
                it.copy(
                    subjectInput = value,
                    selectedSubjectId = null
                )
            }
        }
    }

    private fun applySubjectOption(option: SubjectOption, state: LessonCreationUiState) {
        val newDuration = if (durationEdited) state.durationMinutes else option.durationMinutes
        val newPrice = if (priceEdited) state.priceCents else option.defaultPriceCents
        _uiState.update {
            it.copy(
                selectedSubjectId = option.id,
                subjectInput = option.name,
                durationMinutes = newDuration,
                priceCents = newPrice,
                pricePresets = computePricePresets(newDuration)
            )
        }
    }

    fun onDateSelected(date: LocalDate) {
        _uiState.update { it.copy(date = date) }
    }

    fun onTimeSelected(time: LocalTime) {
        val rounded = roundTimeToStep(time, _uiState.value.slotStepMinutes)
        _uiState.update { it.copy(time = rounded) }
    }

    fun onDurationChanged(value: Int) {
        durationEdited = true
        val sanitized = value.coerceAtLeast(0)
        _uiState.update {
            it.copy(
                durationMinutes = sanitized,
                pricePresets = computePricePresets(sanitized)
            )
        }
    }

    fun onPriceChanged(value: Int) {
        priceEdited = true
        _uiState.update { it.copy(priceCents = value.coerceAtLeast(0)) }
    }

    fun onNoteChanged(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    fun submit() {
        viewModelScope.launch { attemptSubmit(force = false) }
    }

    fun confirmConflict() {
        viewModelScope.launch { conflictRequest?.let { createLesson(it) } }
    }

    fun dismissConflict() {
        conflictRequest = null
        _uiState.update { it.copy(showConflictDialog = null) }
    }

    fun consumeSnackbar() {
        _uiState.update { it.copy(snackbarMessage = null) }
    }

    private suspend fun attemptSubmit(force: Boolean) {
        val state = _uiState.value
        val errors = mutableMapOf<LessonCreationField, String>()
        val student = state.selectedStudent
        if (student == null) {
            errors[LessonCreationField.STUDENT] = "Выберите ученика"
        }
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
            _uiState.update { it.copy(errors = errors) }
            return
        }

        val normalizedSubjectInput = state.subjectInput.trim()
        val selectedSubjectName = state.subjects.firstOrNull { it.id == state.selectedSubjectId }?.name
        val title = normalizedSubjectInput.takeUnless {
            it.isEmpty() || (selectedSubjectName != null && selectedSubjectName.equals(it, ignoreCase = true))
        }

        val request = LessonCreateRequest(
            studentId = student!!.id,
            subjectId = state.selectedSubjectId,
            title = title,
            startAt = start.toInstant(),
            endAt = end.toInstant(),
            priceCents = state.priceCents,
            note = state.note.takeUnless { it.isBlank() }
        )

        if (!force) {
            val conflicts = lessonsRepository.findConflicts(request.startAt, request.endAt)
            if (conflicts.isNotEmpty()) {
                conflictRequest = request
                _uiState.update {
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
        _uiState.update { it.copy(isSubmitting = true, errors = emptyMap()) }
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
            _uiState.update {
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
            conflictRequest = null
            _events.tryEmit(
                LessonCreationEvent.Created(
                    start = start,
                    studentId = request.studentId
                )
            )
        }.onFailure { error ->
            _uiState.update {
                it.copy(
                    isSubmitting = false,
                    snackbarMessage = error.message ?: "Не удалось сохранить занятие"
                )
            }
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
