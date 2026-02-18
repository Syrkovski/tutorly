package com.tutorly.domain.repo

import com.tutorly.domain.model.StudentProfile
import com.tutorly.models.Student
import kotlinx.coroutines.flow.Flow

interface StudentsRepository {
    // sync (suspend) — где нужно мгновенно получить сущность
    suspend fun allActive(): List<Student>
    suspend fun searchActive(query: String): List<Student>
    suspend fun getById(id: Long): Student?
    suspend fun findByName(name: String): Student?

    // observable — для UI
    fun observeStudents(query: String): Flow<List<Student>>
    fun observeArchivedStudents(query: String): Flow<List<Student>>
    fun observeStudent(id: Long): Flow<Student?>

    fun observeStudentProfile(studentId: Long): Flow<StudentProfile?>

    // crud
    suspend fun upsert(student: Student): Long
    suspend fun delete(student: Student)

    // агрегаты
    suspend fun hasDebt(studentId: Long): Boolean
    fun observeHasDebt(studentId: Long): Flow<Boolean>
}

