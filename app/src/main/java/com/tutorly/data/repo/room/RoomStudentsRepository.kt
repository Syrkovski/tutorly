package com.tutorly.data.repo.room

import com.tutorly.data.db.dao.PaymentDao
import com.tutorly.data.db.dao.StudentDao
import com.tutorly.models.Student
import com.tutorly.domain.repo.StudentsRepository
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class RoomStudentsRepository @Inject constructor(
    private val studentDao: StudentDao,
    private val paymentDao: PaymentDao
) : StudentsRepository {

    override suspend fun allActive(): List<Student> =
        studentDao.getAllActive()

    override suspend fun searchActive(query: String): List<Student> =
        studentDao.searchActive(query)

    override suspend fun getById(id: Long): Student? =
        studentDao.getById(id)

    override fun observeStudents(query: String): Flow<List<Student>> =
        studentDao.observeStudents(query)

    override fun observeStudent(id: Long): Flow<Student?> =
        studentDao.observeStudent(id)

    override suspend fun upsert(student: Student): Long =
        studentDao.upsert(student)

    override suspend fun delete(student: Student) =
        studentDao.delete(student)

    override suspend fun hasDebt(studentId: Long): Boolean =
        paymentDao.hasDebt(studentId)

    override fun observeHasDebt(studentId: Long): Flow<Boolean> =
        paymentDao.observeHasDebt(studentId)
}


