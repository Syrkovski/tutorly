package com.tutorly.data.calendar

import android.annotation.SuppressLint
import android.content.Context
import android.provider.CalendarContract
import com.tutorly.domain.model.LessonCreateRequest
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.models.Student
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
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
            val (student, wasCreated) = findOrCreateStudent(parsed.studentName)
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
            val note = buildNote(
                instance.description,
                instance.location
            )
            lessonsRepository.create(
                LessonCreateRequest(
                    studentId = student.id,
                    subjectId = null,
                    title = parsed.lessonTitle,
                    startAt = startAt,
                    endAt = endAt,
                    priceCents = 0,
                    note = note
                )
            )
            createdLessons++
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

    private suspend fun findOrCreateStudent(name: String): Pair<Student, Boolean> {
        val normalized = name.trim()
        val existing = studentsRepository.findByName(normalized)
        if (existing != null) {
            return existing to false
        }
        val id = studentsRepository.upsert(Student(name = normalized))
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
        val separators = listOf(" - ", " – ", " — ", " | ", ": ")
        val normalized = title.trim()
        val split = separators.firstNotNullOfOrNull { separator ->
            val idx = normalized.indexOf(separator)
            if (idx >= 0) idx to separator else null
        }
        if (split != null) {
            val (idx, separator) = split
            val studentName = normalized.substring(0, idx).trim()
            val lessonTitle = normalized.substring(idx + separator.length).trim()
                .takeIf { it.isNotBlank() && !it.equals(studentName, ignoreCase = true) }
            return ParsedEventTitle(studentName = studentName, lessonTitle = lessonTitle)
        }
        return ParsedEventTitle(studentName = normalized, lessonTitle = null)
    }

    private fun normalizeStudentName(name: String): String =
        name.trim().lowercase()

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
    val lessonTitle: String?
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
