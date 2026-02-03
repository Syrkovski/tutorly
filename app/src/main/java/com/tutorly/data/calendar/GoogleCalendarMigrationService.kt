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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoogleCalendarMigrationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val studentsRepository: StudentsRepository,
    private val lessonsRepository: LessonsRepository
) {
    @SuppressLint("MissingPermission")
    suspend fun importFromGoogleCalendar(
        rangeStart: Instant = defaultRangeStart(),
        rangeEnd: Instant = defaultRangeEnd(),
        defaultDuration: Duration = Duration.ofMinutes(60)
    ): GoogleCalendarImportResult {
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.DURATION,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DESCRIPTION,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.ALL_DAY,
            CalendarContract.Events.STATUS
        )
        val selection = buildString {
            append("${CalendarContract.Events.DTSTART} >= ?")
            append(" AND ${CalendarContract.Events.DTSTART} <= ?")
            append(" AND ${CalendarContract.Events.DELETED} = 0")
        }
        val selectionArgs = arrayOf(
            rangeStart.toEpochMilli().toString(),
            rangeEnd.toEpochMilli().toString()
        )
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"
        val resolver = context.contentResolver

        var createdStudents = 0
        var createdLessons = 0
        var skippedAllDay = 0
        var skippedMissingTitle = 0
        var skippedDuplicates = 0
        var skippedCanceled = 0
        var totalEvents = 0

        resolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val startIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val endIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTEND)
            val durationIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DURATION)
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val descriptionIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DESCRIPTION)
            val locationIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.EVENT_LOCATION)
            val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)
            val statusIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.STATUS)

            while (cursor.moveToNext()) {
                totalEvents++
                val status = cursor.getInt(statusIndex)
                if (status == CalendarContract.Events.STATUS_CANCELED) {
                    skippedCanceled++
                    continue
                }
                if (cursor.getInt(allDayIndex) == 1) {
                    skippedAllDay++
                    continue
                }
                val title = cursor.getString(titleIndex)?.trim().orEmpty()
                if (title.isBlank()) {
                    skippedMissingTitle++
                    continue
                }
                val startMillis = cursor.getLong(startIndex)
                if (startMillis <= 0L) {
                    skippedMissingTitle++
                    continue
                }
                val endMillis = resolveEndMillis(
                    cursor.getLong(endIndex),
                    cursor.getString(durationIndex),
                    startMillis,
                    defaultDuration
                )
                val startAt = Instant.ofEpochMilli(startMillis)
                val endAt = Instant.ofEpochMilli(endMillis)
                if (endAt <= startAt) {
                    skippedMissingTitle++
                    continue
                }

                val parsed = parseEventTitle(title)
                if (parsed.studentName.isBlank()) {
                    skippedMissingTitle++
                    continue
                }
                val (student, wasCreated) = findOrCreateStudent(parsed.studentName)
                if (student.id == 0L) {
                    skippedMissingTitle++
                    continue
                }
                if (wasCreated) {
                    createdStudents++
                }
                val existing = lessonsRepository.findExactLesson(student.id, startAt, endAt)
                if (existing != null) {
                    skippedDuplicates++
                    continue
                }
                val note = buildNote(
                    cursor.getString(descriptionIndex),
                    cursor.getString(locationIndex)
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

    private fun resolveEndMillis(
        rawEndMillis: Long,
        durationValue: String?,
        startMillis: Long,
        defaultDuration: Duration
    ): Long {
        if (rawEndMillis > 0L) {
            return rawEndMillis
        }
        val parsedDuration = durationValue?.let {
            runCatching { Duration.parse(it) }.getOrNull()
        }
        val resolvedDuration = parsedDuration ?: defaultDuration
        return startMillis + resolvedDuration.toMillis()
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

    private fun defaultRangeStart(): Instant = Instant.now().minus(Duration.ofDays(365))

    private fun defaultRangeEnd(): Instant = Instant.now().plus(Duration.ofDays(365))
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

private data class ParsedEventTitle(
    val studentName: String,
    val lessonTitle: String?
)
