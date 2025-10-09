package com.tutorly.data.db.projections

import androidx.room.Embedded
import androidx.room.Relation
import com.tutorly.domain.model.LessonDetails
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

fun LessonWithStudent.toLessonDetails(): LessonDetails {
    val normalizedDuration = resolveDuration(
        startAt = lesson.startAt,
        endAt = lesson.endAt,
        subjectDurationMinutes = subject?.durationMinutes
    )
    val normalizedEnd = lesson.startAt.plus(normalizedDuration)

    return LessonDetails(
        id = lesson.id,
        studentId = lesson.studentId,
        startAt = lesson.startAt,
        endAt = normalizedEnd,
        duration = normalizedDuration,
        studentName = student.name,
        studentNote = student.note,
        subjectName = subject?.name,
        subjectColorArgb = subject?.colorArgb,
        paymentStatus = lesson.paymentStatus,
        paymentStatusIcon = lesson.paymentStatus.asIcon(),
        priceCents = lesson.priceCents,
        paidCents = lesson.paidCents,
        lessonTitle = lesson.title
    )
}

data class StudentWithLessons(
    @Embedded val student: Student,
    @Relation(parentColumn = "id", entityColumn = "studentId")
    val lessons: List<Lesson>
)
