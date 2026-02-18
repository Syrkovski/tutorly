package com.tutorly.data.repo.room

import com.tutorly.data.db.dao.LessonDao
import com.tutorly.data.db.dao.PaymentDao
import com.tutorly.data.db.dao.StudentDao
import com.tutorly.data.db.projections.LessonWithSubject
import com.tutorly.domain.model.StudentProfile
import com.tutorly.domain.model.StudentProfileLesson
import com.tutorly.domain.model.StudentProfileLessonRate
import com.tutorly.domain.model.StudentProfileMetrics
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.models.PaymentStatus
import com.tutorly.models.Student
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import java.time.Duration
import java.time.Instant

@Singleton
class RoomStudentsRepository @Inject constructor(
    private val studentDao: StudentDao,
    private val paymentDao: PaymentDao,
    private val lessonDao: LessonDao
) : StudentsRepository {

    override suspend fun allActive(): List<Student> =
        studentDao.getAllActive()

    override suspend fun searchActive(query: String): List<Student> =
        studentDao.searchActive(query)

    override suspend fun getById(id: Long): Student? =
        studentDao.getById(id)

    override suspend fun findByName(name: String): Student? =
        studentDao.findByName(name)

    override fun observeStudents(query: String): Flow<List<Student>> =
        studentDao.observeStudents(query)

    override fun observeArchivedStudents(query: String): Flow<List<Student>> =
        studentDao.observeArchivedStudents(query)

    override fun observeStudent(id: Long): Flow<Student?> =
        studentDao.observeStudent(id)

    override fun observeStudentProfile(
        studentId: Long
    ): Flow<StudentProfile?> {
        val studentFlow = studentDao.observeStudent(studentId)
        val hasDebtFlow = paymentDao.observeHasDebt(studentId, PaymentStatus.outstandingStatuses)
        val lessonsFlow = lessonDao.observeByStudent(studentId)
        val lessonsWithSubjectFlow = lessonDao.observeWithSubjectByStudent(studentId)
        val prepaymentFlow = combine(
            paymentDao.observePrepaymentDeposits(studentId, PaymentStatus.PAID),
            paymentDao.observePrepaymentAllocations(studentId, PaymentStatus.PAID, PREPAYMENT_METHOD)
        ) { deposits, allocations -> deposits - allocations }

        return combine(
            studentFlow,
            hasDebtFlow,
            lessonsFlow,
            lessonsWithSubjectFlow,
            prepaymentFlow
        ) { student, hasDebt, lessons, lessonsWithSubject, prepaymentCents ->
            student ?: return@combine null

            val now = Instant.now()
            val occurredLessons = lessons.filter { it.startAt <= now }
            val occurredLessonsWithSubject = lessonsWithSubject.filter { it.lesson.startAt <= now }

            val metrics = buildMetrics(occurredLessons, prepaymentCents)
            val rate = occurredLessonsWithSubject.firstOrNull()?.lesson?.let(::buildRate)
            val recentSubject = occurredLessonsWithSubject.firstOrNull()?.subject?.name?.takeIf { it.isNotBlank() }?.trim()
            val primarySubject = student.subject?.takeIf { it.isNotBlank() }?.trim()
                ?: recentSubject
            val profileLessons = occurredLessonsWithSubject.map { projection ->
                toProfileLesson(projection, primarySubject)
            }

            StudentProfile(
                student = student,
                subject = primarySubject,
                grade = student.grade,
                rate = rate,
                hasDebt = hasDebt,
                metrics = metrics,
                recentLessons = profileLessons
            )
        }.distinctUntilChanged()
    }

    override suspend fun upsert(student: Student): Long {
        return if (student.id == 0L) {
            studentDao.insert(student)
        } else {
            studentDao.update(student)
            student.id
        }
    }

    override suspend fun delete(student: Student) =
        studentDao.delete(student)

    override suspend fun hasDebt(studentId: Long): Boolean =
        paymentDao.hasDebt(studentId, PaymentStatus.outstandingStatuses)

    override fun observeHasDebt(studentId: Long): Flow<Boolean> =
        paymentDao.observeHasDebt(studentId, PaymentStatus.outstandingStatuses)
}

private fun buildMetrics(
    lessons: List<com.tutorly.models.Lesson>,
    prepaymentCents: Long
): StudentProfileMetrics {
    if (lessons.isEmpty()) {
        return StudentProfileMetrics(
            totalLessons = 0,
            paidLessons = 0,
            debtLessons = 0,
            totalPaidCents = 0,
            averagePriceCents = null,
            outstandingCents = 0,
            prepaymentCents = prepaymentCents
        )
    }

    val total = lessons.size
    val paid = lessons.count { it.paymentStatus == PaymentStatus.PAID }
    val debt = lessons.count { it.paymentStatus in PaymentStatus.outstandingStatuses }
    val totalPaid = lessons.fold(0L) { acc, lesson -> acc + lesson.paidCents }
    val outstandingCents = lessons
        .filter { it.paymentStatus in PaymentStatus.outstandingStatuses }
        .fold(0L) { acc, lesson ->
            val due = (lesson.priceCents - lesson.paidCents).coerceAtLeast(0)
            acc + due.toLong()
        }
    val pricedLessons = lessons.filter { it.priceCents > 0 }
    val averagePrice = if (pricedLessons.isNotEmpty()) {
        pricedLessons.sumOf { it.priceCents }.toDouble() / pricedLessons.size
    } else {
        null
    }

    return StudentProfileMetrics(
        totalLessons = total,
        paidLessons = paid,
        debtLessons = debt,
        totalPaidCents = totalPaid,
        averagePriceCents = averagePrice?.toInt(),
        outstandingCents = outstandingCents,
        prepaymentCents = prepaymentCents
    )
}

private fun buildRate(lesson: com.tutorly.models.Lesson): StudentProfileLessonRate? {
    if (lesson.priceCents <= 0) return null
    val duration = Duration.between(lesson.startAt, lesson.endAt)
    val minutes = duration.toMinutes().toInt().coerceAtLeast(0)
    if (minutes == 0) return null
    return StudentProfileLessonRate(durationMinutes = minutes, priceCents = lesson.priceCents)
}

private fun toProfileLesson(
    projection: LessonWithSubject,
    fallbackSubject: String?
): StudentProfileLesson {
    val lesson = projection.lesson
    val duration = Duration.between(lesson.startAt, lesson.endAt)
    val minutes = duration.toMinutes().toInt().coerceAtLeast(0)

    return StudentProfileLesson(
        id = lesson.id,
        title = lesson.title,
        subjectName = projection.subject?.name?.takeIf { it.isNotBlank() }?.trim()
            ?: fallbackSubject?.takeIf { it.isNotBlank() }?.trim(),
        startAt = lesson.startAt,
        endAt = lesson.endAt,
        durationMinutes = minutes,
        priceCents = lesson.priceCents,
        paymentStatus = lesson.paymentStatus
    )
}


