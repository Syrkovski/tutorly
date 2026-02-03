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
    @SuppressLint("MissingPermission")
    suspend fun importFromGoogleCalendar(
        rangeStart: Instant = defaultRangeStart(),
        rangeEnd: Instant = defaultRangeEnd(),
        defaultDuration: Duration = Duration.ofMinutes(60)
    ): GoogleCalendarImportResult {
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
        val resolver = context.contentResolver

        var createdStudents = 0
        var createdLessons = 0
        var skippedAllDay = 0
        var skippedMissingTitle = 0
        var skippedDuplicates = 0
        var skippedCanceled = 0
        var totalEvents = 0

        val builder = CalendarContract.Instances.CONTENT_URI
            .buildUpon()
        CalendarContract.Instances.appendRange(
            builder,
            rangeStart.toEpochMilli(),
            rangeEnd.toEpochMilli()
        )
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
                totalEvents++
                val status = statusIndex.takeIf { it >= 0 }?.let { cursor.getInt(it) }
                if (status == CalendarContract.Events.STATUS_CANCELED) {
                    skippedCanceled++
                    continue
                }
                val isAllDay = allDayIndex.takeIf { it >= 0 }?.let { cursor.getInt(it) == 1 } ?: false
                if (isAllDay) {
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
                val endMillis = resolveEndMillis(cursor.getLong(endIndex), startMillis, defaultDuration)
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

private data class ParsedEventTitle(
    val studentName: String,
    val lessonTitle: String?
)
