package com.tutorly.domain.repo

import com.tutorly.models.Student
import kotlinx.coroutines.flow.Flow

interface StudentsRepository {
    // sync (suspend) — где нужно мгновенно получить сущность
    suspend fun allActive(): List<Student>
    suspend fun getById(id: Long): Student?

    // observable — для UI
    fun observeStudents(query: String): Flow<List<Student>>
    fun observeStudent(id: Long): Flow<Student?>

    // crud
    suspend fun upsert(student: Student): Long
    suspend fun delete(student: Student)

    // агрегаты
    fun observeHasDebt(studentId: Long): Flow<Boolean>
}

