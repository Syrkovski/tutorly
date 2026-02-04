package com.tutorly.data.calendar

import android.annotation.SuppressLint
import android.content.Context
import android.provider.CalendarContract
import com.tutorly.domain.model.LessonCreateRequest
import com.tutorly.domain.model.RecurrenceCreateRequest
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.models.LessonRecurrence
import com.tutorly.models.RecurrenceFrequency
import com.tutorly.models.Student
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleCalendarMigrationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val studentsRepository: StudentsRepository,
    private val lessonsRepository: LessonsRepository
) {
    suspend fun fetchImportCandidates(
        rangeStart: Instant = defaultRangeStart(),
        rangeEnd: Instant = defaultRangeEnd(),
        defaultDuration: Duration = Duration.ofMinutes(60)
    ): List<GoogleCalendarImportCandidate> {
        val candidates = linkedMapOf<String, CandidateAccumulator>()
        queryInstances(rangeStart, rangeEnd).forEach { instance ->
            val status = instance.status
            if (status == CalendarContract.Events.STATUS_CANCELED) return@forEach
            if (instance.isAllDay) return@forEach
            val title = instance.title?.trim().orEmpty()
            if (title.isBlank()) return@forEach
            val startMillis = instance.startMillis
            if (startMillis <= 0L) return@forEach
            val endMillis = resolveEndMillis(instance.endMillis, startMillis, defaultDuration)
            if (endMillis <= startMillis) return@forEach
            val parsed = parseEventTitle(title)
            if (parsed.studentName.isBlank()) return@forEach

            val normalized = normalizeStudentName(parsed.studentName)
            val current = candidates[normalized]
            if (current == null) {
                candidates[normalized] = CandidateAccumulator(parsed.studentName.trim(), 1)
            } else {
                current.count++
            }
        }

        return candidates.values
            .map { candidate ->
                GoogleCalendarImportCandidate(
                    studentName = candidate.displayName,
                    lessonsCount = candidate.count
                )
            }
            .sortedBy { it.studentName.lowercase() }
    }

    @SuppressLint("MissingPermission")
    suspend fun importFromGoogleCalendar(
        rangeStart: Instant = defaultRangeStart(),
        rangeEnd: Instant = defaultRangeEnd(),
        defaultDuration: Duration = Duration.ofMinutes(60),
        allowedStudentNames: Set<String>? = null
    ): GoogleCalendarImportResult {
        var createdStudents = 0
        var createdLessons = 0
        var skippedAllDay = 0
        var skippedMissingTitle = 0
        var skippedDuplicates = 0
        var skippedCanceled = 0
        var totalEvents = 0
        val allowedNormalized = allowedStudentNames?.mapTo(mutableSetOf()) { normalizeStudentName(it) }
        val events = mutableListOf<ImportEvent>()

        queryInstances(rangeStart, rangeEnd).forEach { instance ->
            totalEvents++
            val status = instance.status
            if (status == CalendarContract.Events.STATUS_CANCELED) {
                skippedCanceled++
                return@forEach
            }
            if (instance.isAllDay) {
                skippedAllDay++
                return@forEach
            }
            val title = instance.title?.trim().orEmpty()
            if (title.isBlank()) {
                skippedMissingTitle++
                return@forEach
            }
            val startMillis = instance.startMillis
            if (startMillis <= 0L) {
                skippedMissingTitle++
                return@forEach
            }
            val endMillis = resolveEndMillis(instance.endMillis, startMillis, defaultDuration)
            val startAt = Instant.ofEpochMilli(startMillis)
            val endAt = Instant.ofEpochMilli(endMillis)
            if (endAt <= startAt) {
                skippedMissingTitle++
                return@forEach
            }

            val parsed = parseEventTitle(title)
            if (parsed.studentName.isBlank()) {
                skippedMissingTitle++
                return@forEach
            }
            val normalized = normalizeStudentName(parsed.studentName)
            if (allowedNormalized != null && normalized !in allowedNormalized) {
                return@forEach
            }
            val (student, wasCreated) = findOrCreateStudent(
                name = parsed.studentName,
                subject = parsed.subject,
                grade = parsed.grade,
                rateCents = parsed.rateCents
            )
            if (student.id == 0L) {
                skippedMissingTitle++
                return@forEach
            }
            if (wasCreated) {
                createdStudents++
            }
            val existing = lessonsRepository.findExactLesson(student.id, startAt, endAt)
            if (existing != null) {
                skippedDuplicates++
                return@forEach
            }
            val note = buildNote(instance.description, instance.location)
            events += ImportEvent(
                student = student,
                studentName = parsed.studentName,
                normalizedStudentName = normalized,
                lessonTitle = parsed.lessonTitle ?: parsed.subject,
                startAt = startAt,
                endAt = endAt,
                note = note,
                priceCents = parsed.rateCents
            )
        }

        val zone = ZoneId.systemDefault()
        val grouped = events.groupBy { it.seriesKey(zone) }.toSortedMap(compareBy { it.studentName.lowercase() })
        for ((_, group) in grouped) {
            if (group.isEmpty()) continue
            val sorted = group.sortedBy { it.startAt }
            val recurrenceRequest = buildRecurrenceRequest(sorted, zone)
                ?: buildFallbackRecurrenceRequest(sorted, zone)
            val recurrence = recurrenceRequest?.let { request ->
                LessonRecurrence(
                    frequency = request.frequency,
                    interval = request.interval,
                    daysOfWeek = request.daysOfWeek,
                    startDateTime = sorted.first().startAt,
                    untilDateTime = request.until,
                    timezone = request.timezone
                )
            }
            var seriesId: Long? = null
            var anchorCreated = false

            for (event in sorted) {
                val existing = lessonsRepository.findExactLesson(
                    event.student.id,
                    event.startAt,
                    event.endAt
                )
                if (existing != null) {
                    skippedDuplicates++
                    if (!anchorCreated && recurrence != null) {
                        if (existing.seriesId != null) {
                            seriesId = existing.seriesId
                            anchorCreated = true
                        } else {
                            val updatedId = lessonsRepository.upsert(
                                existing.copy(recurrence = recurrence, updatedAt = Instant.now())
                            )
                            val updated = lessonsRepository.getById(updatedId)
                            seriesId = updated?.seriesId
                            anchorCreated = true
                        }
                    } else if (seriesId != null && existing.seriesId == null) {
                        lessonsRepository.upsert(
                            existing.copy(seriesId = seriesId, updatedAt = Instant.now())
                        )
                    }
                    continue
                }

                if (!anchorCreated && recurrenceRequest != null) {
                    val id = lessonsRepository.create(
                        LessonCreateRequest(
                            studentId = event.student.id,
                            subjectId = null,
                            title = event.lessonTitle,
                            startAt = event.startAt,
                            endAt = event.endAt,
                            priceCents = event.priceCents ?: 0,
                            note = event.note,
                            recurrence = recurrenceRequest
                        )
                    )
                    createdLessons++
                    val created = lessonsRepository.getById(id)
                    seriesId = created?.seriesId
                    anchorCreated = true
                    continue
                }

                val id = lessonsRepository.create(
                    LessonCreateRequest(
                        studentId = event.student.id,
                        subjectId = null,
                        title = event.lessonTitle,
                        startAt = event.startAt,
                        endAt = event.endAt,
                        priceCents = event.priceCents ?: 0,
                        note = event.note
                    )
                )
                createdLessons++
                if (seriesId != null) {
                    val created = lessonsRepository.getById(id)
                    if (created != null && created.seriesId == null) {
                        lessonsRepository.upsert(
                            created.copy(seriesId = seriesId, updatedAt = Instant.now())
                        )
                    }
                }
            }
        }

        return GoogleCalendarImportResult(
            totalEvents = totalEvents,
            createdStudents = createdStudents,
            createdLessons = createdLessons,
            skippedAllDay = skippedAllDay,
            skippedMissingTitle = skippedMissingTitle,
            skippedDuplicates = skippedDuplicates,
            skippedCanceled = skippedCanceled,
            rangeStart = rangeStart,
            rangeEnd = rangeEnd
        )
    }

    private suspend fun findOrCreateStudent(
        name: String,
        subject: String?,
        grade: String?,
        rateCents: Int?
    ): Pair<Student, Boolean> {
        val normalized = name.trim()
        val existing = studentsRepository.findByName(normalized)
        if (existing != null) {
            val updated = existing.copy(
                subject = existing.subject ?: subject,
                grade = existing.grade ?: grade,
                rateCents = existing.rateCents ?: rateCents,
                updatedAt = Instant.now()
            )
            val resolved = if (updated != existing) {
                val id = studentsRepository.upsert(updated)
                studentsRepository.getById(id) ?: updated
            } else {
                existing
            }
            return resolved to false
        }
        val id = studentsRepository.upsert(
            Student(
                name = normalized,
                subject = subject,
                grade = grade,
                rateCents = rateCents
            )
        )
        return (studentsRepository.getById(id) ?: Student(id = id, name = normalized)) to true
    }

    private fun buildNote(description: String?, location: String?): String? {
        val parts = listOfNotNull(
            description?.trim()?.takeIf { it.isNotBlank() },
            location?.trim()?.takeIf { it.isNotBlank() }
        )
        return parts.joinToString("\n").takeIf { it.isNotBlank() }
    }

    private fun queryInstances(rangeStart: Instant, rangeEnd: Instant): List<CalendarInstance> {
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.DESCRIPTION,
            CalendarContract.Instances.EVENT_LOCATION,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.STATUS
        )
        val selection = "${CalendarContract.Events.DELETED} = 0"
        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"
        val builder = CalendarContract.Instances.CONTENT_URI
            .buildUpon()
            .appendPath(rangeStart.toEpochMilli().toString())
            .appendPath(rangeEnd.toEpochMilli().toString())
        val resolver = context.contentResolver

        val instances = mutableListOf<CalendarInstance>()
        resolver.query(
            builder.build(),
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            val startIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.END)
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val descriptionIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.DESCRIPTION)
            val locationIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_LOCATION)
            val allDayIndex = cursor.getColumnIndex(CalendarContract.Instances.ALL_DAY)
            val statusIndex = cursor.getColumnIndex(CalendarContract.Instances.STATUS)

            while (cursor.moveToNext()) {
                instances.add(
                    CalendarInstance(
                        title = cursor.getString(titleIndex),
                        description = cursor.getString(descriptionIndex),
                        location = cursor.getString(locationIndex),
                        startMillis = cursor.getLong(startIndex),
                        endMillis = cursor.getLong(endIndex),
                        isAllDay = allDayIndex.takeIf { it >= 0 }?.let { cursor.getInt(it) == 1 } ?: false,
                        status = statusIndex.takeIf { it >= 0 }?.let { cursor.getInt(it) }
                    )
                )
            }
        }
        return instances
    }

    private fun resolveEndMillis(
        rawEndMillis: Long,
        startMillis: Long,
        defaultDuration: Duration
    ): Long {
        if (rawEndMillis > 0L) {
            return rawEndMillis
        }
        return startMillis + defaultDuration.toMillis()
    }

    private fun parseEventTitle(title: String): ParsedEventTitle {
        val normalized = title.trim()
        if (normalized.isBlank()) {
            return ParsedEventTitle(
                studentName = "",
                lessonTitle = null,
                subject = null,
                grade = null,
                rateCents = null
            )
        }
        val rawTokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }
        val cleanedTokens = rawTokens.filterNot { it in setOf("-", "–", "—", "|", ":") }
        val (studentName, detailTokens) = extractStudentName(cleanedTokens)
        val details = parseLessonDetails(detailTokens)
        return ParsedEventTitle(
            studentName = studentName,
            lessonTitle = details.title,
            subject = details.subject,
            grade = details.grade,
            rateCents = details.rateCents
        )
    }

    private fun normalizeStudentName(name: String): String =
        name.trim().lowercase()

    private fun parseLessonDetails(tokens: List<String>): ParsedLessonDetails {
        if (tokens.isEmpty()) {
            return ParsedLessonDetails(title = null, subject = null, grade = null, rateCents = null)
        }
        val mutable = tokens.toMutableList()
        var rateCents: Int? = null
        var grade: String? = null
        val examTags = mutableListOf<String>()

        val rateIndex = mutable.indexOfFirst { it.matches(Regex("\\d{4,5}")) }
        if (rateIndex >= 0) {
            rateCents = mutable.removeAt(rateIndex).toIntOrNull()?.let { it * 100 }
        }

        val gradeIndex = mutable.indexOfFirst { token ->
            val lowered = token.lowercase()
            lowered == "студент" ||
                lowered == "student" ||
                lowered == "себя" ||
                lowered == "длясебя" ||
                lowered == "для" ||
                token.matches(Regex("^(?:[1-9]|1[01])$"))
        }
        if (gradeIndex >= 0) {
            grade = mutable.removeAt(gradeIndex).let { token ->
                when (token.lowercase()) {
                    "student" -> "студент"
                    "длясебя" -> "для себя"
                    "себя" -> "для себя"
                    "для" -> "для себя"
                    else -> token
                }
            }.let { value ->
                if (value.matches(Regex("^(?:[1-9]|1[01])$"))) "$value класс" else value
            }
            if (grade == "для себя") {
                val selfIndex = mutable.indexOfFirst { it.equals("себя", ignoreCase = true) }
                if (selfIndex >= 0) {
                    mutable.removeAt(selfIndex)
                }
            }
        }

        val examTokens = setOf("огэ", "егэ", "гиа", "впр")
        mutable.removeAll { token ->
            val lowered = token.lowercase()
            if (lowered in examTokens) {
                examTags += lowered.uppercase()
                true
            } else {
                false
            }
        }

        val subjectToken = mutable.lastOrNull()?.takeIf { it.isNotBlank() }
        val subjectBase = subjectToken?.let { normalizeSubject(it) }
        val subject = when {
            subjectBase == null && examTags.isEmpty() -> null
            subjectBase == null -> examTags.joinToString(", ")
            examTags.isEmpty() -> subjectBase
            else -> listOf(subjectBase, examTags.joinToString(", ")).joinToString(", ")
        }
        val title = subject

        return ParsedLessonDetails(
            title = title,
            subject = subject,
            grade = grade,
            rateCents = rateCents
        )
    }

    private fun extractStudentName(tokens: List<String>): Pair<String, List<String>> {
        if (tokens.isEmpty()) return "" to emptyList()
        val first = tokens.first()
        if (tokens.size == 1) return first to emptyList()
        val second = tokens[1]
        val isSecondName = second.matches(Regex("^[\\p{L}\\-]+$")) &&
            !second.matches(Regex("\\d{4,5}")) &&
            !second.matches(Regex("^(?:[1-9]|1[01])$")) &&
            !second.equals("студент", ignoreCase = true) &&
            !second.equals("student", ignoreCase = true) &&
            !second.equals("для", ignoreCase = true) &&
            !second.equals("себя", ignoreCase = true)
        return if (isSecondName) {
            "${first} ${second}" to tokens.drop(2)
        } else {
            first to tokens.drop(1)
        }
    }

    private fun normalizeSubject(raw: String): String {
        val normalized = raw.lowercase().trim('.', ',', ';', ':')
        val mapped = when (normalized) {
            "м", "мат", "матем", "математика", "math" -> "Математика"
            "а", "анг", "англ", "английский", "english", "eng" -> "Английский язык"
            "и", "инф", "информ", "информатика", "it" -> "Информатика"
            "р", "рус", "русский", "русск" -> "Русский язык"
            "л", "лит", "литература" -> "Литература"
            "ф", "физ", "физика" -> "Физика"
            "х", "хим", "химия" -> "Химия"
            "б", "био", "биология" -> "Биология"
            "о", "общ", "обществознание", "обществозн" -> "Обществознание"
            "ист", "история" -> "История"
            else -> raw
        }
        return mapped.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    private fun buildRecurrenceRequest(
        events: List<ImportEvent>,
        zone: ZoneId
    ): RecurrenceCreateRequest? {
        if (events.size < 2) return null
        val sorted = events.sortedBy { it.startAt }
        val first = sorted.first()
        val last = sorted.last()
        val startLocal = first.startAt.atZone(zone)
        val days = sorted.map { it.startAt.atZone(zone).dayOfWeek }.distinct().sortedBy { it.value }
        if (days.isEmpty()) return null

        val intervalWeeks = determineIntervalWeeks(sorted, zone) ?: return null
        val expected = generateExpectedStarts(
            startAt = first.startAt,
            endAt = last.startAt,
            intervalWeeks = intervalWeeks,
            daysOfWeek = days,
            zone = zone
        )
        val actualStarts = sorted.map { it.startAt }.toSet()
        if (expected != actualStarts) return null

        val frequency = if (intervalWeeks == 2) {
            RecurrenceFrequency.BIWEEKLY
        } else {
            RecurrenceFrequency.WEEKLY
        }
        val interval = if (intervalWeeks == 2) 1 else intervalWeeks
        val until = last.startAt

        return RecurrenceCreateRequest(
            frequency = frequency,
            interval = interval,
            daysOfWeek = days,
            until = until,
            timezone = startLocal.zone
        )
    }

    private fun buildFallbackRecurrenceRequest(
        events: List<ImportEvent>,
        zone: ZoneId
    ): RecurrenceCreateRequest? {
        if (events.size < 2) return null
        val sorted = events.sortedBy { it.startAt }
        val first = sorted.first()
        val startLocal = first.startAt.atZone(zone)
        val days = sorted.map { it.startAt.atZone(zone).dayOfWeek }.distinct().sortedBy { it.value }
        if (days.isEmpty()) return null
        return RecurrenceCreateRequest(
            frequency = RecurrenceFrequency.WEEKLY,
            interval = 1,
            daysOfWeek = days,
            until = first.startAt,
            timezone = startLocal.zone
        )
    }

    private fun determineIntervalWeeks(
        events: List<ImportEvent>,
        zone: ZoneId
    ): Int? {
        val byDay = events.groupBy { it.startAt.atZone(zone).dayOfWeek }
        var intervalWeeks: Int? = null
        for ((_, dayEvents) in byDay) {
            if (dayEvents.size < 2) continue
            val sorted = dayEvents.sortedBy { it.startAt }
            val diffs = sorted.zipWithNext { a, b ->
                val days = ChronoUnit.DAYS.between(
                    a.startAt.atZone(zone).toLocalDate(),
                    b.startAt.atZone(zone).toLocalDate()
                )
                if (days <= 0 || days % 7L != 0L) return null
                (days / 7L).toInt()
            }
            val first = diffs.firstOrNull() ?: continue
            if (diffs.any { it != first }) return null
            intervalWeeks = when (val current = intervalWeeks) {
                null -> first
                else -> if (current == first) current else return null
            }
        }
        return intervalWeeks ?: 1
    }

    private fun generateExpectedStarts(
        startAt: Instant,
        endAt: Instant,
        intervalWeeks: Int,
        daysOfWeek: List<java.time.DayOfWeek>,
        zone: ZoneId
    ): Set<Instant> {
        if (startAt > endAt) return emptySet()
        val base = startAt.atZone(zone)
        val results = mutableSetOf<Instant>()
        val endZoned = endAt.atZone(zone)
        val stepWeeks = intervalWeeks.coerceAtLeast(1).toLong()
        for (day in daysOfWeek) {
            var occurrence = base.with(TemporalAdjusters.nextOrSame(day))
            if (occurrence.isBefore(base)) {
                occurrence = occurrence.plusWeeks(stepWeeks)
            }
            while (!occurrence.isAfter(endZoned)) {
                results += occurrence.toInstant()
                occurrence = occurrence.plusWeeks(stepWeeks)
            }
        }
        return results
    }

    private fun defaultRangeStart(): Instant {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val year = if (today.month >= Month.SEPTEMBER) today.year else today.year - 1
        return LocalDate.of(year, Month.SEPTEMBER, 1)
            .atStartOfDay(zone)
            .toInstant()
    }

    private fun defaultRangeEnd(): Instant {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val year = if (today.month >= Month.SEPTEMBER) today.year + 1 else today.year
        return LocalDate.of(year, Month.AUGUST, 31)
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()
    }
}

data class GoogleCalendarImportResult(
    val totalEvents: Int,
    val createdStudents: Int,
    val createdLessons: Int,
    val skippedAllDay: Int,
    val skippedMissingTitle: Int,
    val skippedDuplicates: Int,
    val skippedCanceled: Int,
    val rangeStart: Instant,
    val rangeEnd: Instant
)

data class GoogleCalendarImportCandidate(
    val studentName: String,
    val lessonsCount: Int
)

private data class ParsedEventTitle(
    val studentName: String,
    val lessonTitle: String?,
    val subject: String?,
    val grade: String?,
    val rateCents: Int?
)

private data class ParsedLessonDetails(
    val title: String?,
    val subject: String?,
    val grade: String?,
    val rateCents: Int?
)

private data class CalendarInstance(
    val title: String?,
    val description: String?,
    val location: String?,
    val startMillis: Long,
    val endMillis: Long,
    val isAllDay: Boolean,
    val status: Int?
)

private data class CandidateAccumulator(
    val displayName: String,
    var count: Int
)

private data class ImportEvent(
    val student: Student,
    val studentName: String,
    val normalizedStudentName: String,
    val lessonTitle: String?,
    val startAt: Instant,
    val endAt: Instant,
    val note: String?,
    val priceCents: Int?
) {
    fun seriesKey(zone: ZoneId): SeriesKey {
        val startLocal = startAt.atZone(zone)
        val durationMinutes = Duration.between(startAt, endAt).toMinutes().toInt().coerceAtLeast(0)
        return SeriesKey(
            studentName = normalizedStudentName,
            lessonTitle = lessonTitle?.trim()?.lowercase(),
            dayOfWeek = startLocal.dayOfWeek,
            startTime = startLocal.toLocalTime(),
            durationMinutes = durationMinutes,
            priceCents = priceCents
        )
    }
}

private data class SeriesKey(
    val studentName: String,
    val lessonTitle: String?,
    val dayOfWeek: java.time.DayOfWeek,
    val startTime: LocalTime,
    val durationMinutes: Int,
    val priceCents: Int?
)
