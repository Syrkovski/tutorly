package com.tutorly.data.db.projections

import androidx.room.Embedded
import androidx.room.Relation
import com.tutorly.domain.model.LessonDetails
import com.tutorly.domain.model.LessonForToday
import com.tutorly.domain.model.asIcon
import com.tutorly.domain.model.resolveDuration
import com.tutorly.models.Lesson
import com.tutorly.models.Payment
import com.tutorly.models.Student
import com.tutorly.models.SubjectPreset

data class LessonWithStudent(
    @Embedded val lesson: Lesson,
    @Relation(parentColumn = "studentId", entityColumn = "id")
    val student: Student,
    @Relation(parentColumn = "subjectId", entityColumn = "id")
    val subject: SubjectPreset?,
    @Relation(parentColumn = "id", entityColumn = "lessonId")
    val payments: List<Payment>
)

data class LessonWithSubject(
    @Embedded val lesson: Lesson,
    @Relation(parentColumn = "subjectId", entityColumn = "id")
    val subject: SubjectPreset?
)

fun LessonWithStudent.toLessonDetails(): LessonDetails {
    val normalizedDuration = resolveDuration(
        startAt = lesson.startAt,
        endAt = lesson.endAt,
        subjectDurationMinutes = subject?.durationMinutes
    )
    val normalizedEnd = lesson.startAt.plus(normalizedDuration)

    val subjectName = subject?.name?.takeIf { it.isNotBlank() }?.trim()
        ?: student.subject?.takeIf { it.isNotBlank() }?.trim()

    return LessonDetails(
        id = lesson.id,
        baseLessonId = lesson.id,
        studentId = lesson.studentId,
        startAt = lesson.startAt,
        endAt = normalizedEnd,
        duration = normalizedDuration,
        studentName = student.name,
        studentNote = student.note,
        subjectName = subjectName,
        studentGrade = student.grade,
        subjectColorArgb = subject?.colorArgb,
        paymentStatus = lesson.paymentStatus,
        paymentStatusIcon = lesson.paymentStatus.asIcon(),
        lessonStatus = lesson.status,
        priceCents = lesson.priceCents,
        paidCents = lesson.paidCents,
        lessonTitle = lesson.title,
        lessonNote = lesson.note,
        isRecurring = lesson.seriesId != null,
        seriesId = lesson.seriesId,
        originalStartAt = lesson.startAt
    )
}

fun LessonWithStudent.toLessonForToday(): LessonForToday {
    val normalizedDuration = resolveDuration(
        startAt = lesson.startAt,
        endAt = lesson.endAt,
        subjectDurationMinutes = subject?.durationMinutes
    )
    val normalizedEnd = lesson.startAt.plus(normalizedDuration)

    val subjectName = subject?.name?.takeIf { it.isNotBlank() }?.trim()
        ?: student.subject?.takeIf { it.isNotBlank() }?.trim()

    return LessonForToday(
        id = lesson.id,
        baseLessonId = lesson.id,
        studentId = lesson.studentId,
        studentName = student.name,
        studentGrade = student.grade,
        subjectName = subjectName,
        lessonTitle = lesson.title,
        startAt = lesson.startAt,
        endAt = normalizedEnd,
        duration = normalizedDuration,
        priceCents = lesson.priceCents,
        studentRateCents = student.rateCents,
        note = lesson.note,
        paymentStatus = lesson.paymentStatus,
        lessonStatus = lesson.status,
        markedAt = lesson.markedAt,
        isRecurring = lesson.seriesId != null,
        seriesId = lesson.seriesId,
        originalStartAt = lesson.startAt,
        recurrenceLabel = null,
        paidCents = TODO(),
    )
}

data class StudentWithLessons(
    @Embedded val student: Student,
    @Relation(parentColumn = "id", entityColumn = "studentId")
    val lessons: List<Lesson>
)
