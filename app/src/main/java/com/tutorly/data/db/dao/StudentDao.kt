package com.tutorly.data.db.dao

import androidx.room.*
import com.tutorly.models.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query("SELECT * FROM students WHERE active = 1 ORDER BY name COLLATE NOCASE")
    suspend fun getAllActive(): List<Student>

    @Query(
        """
        SELECT * FROM students
        WHERE active = 1 AND (
            :q == '' OR name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%'
        )
        ORDER BY name COLLATE NOCASE
        """
    )
    suspend fun searchActive(q: String): List<Student>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Student?

    @Query("""
        SELECT * FROM students
        WHERE (:q == '' OR name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%')
        ORDER BY name COLLATE NOCASE
    """)
    fun observeStudents(q: String): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :id")
    fun observeStudent(id: Long): Flow<Student?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(student: Student): Long

    @Delete
    suspend fun delete(student: Student)
}





//@Dao
//interface StudentDao {
//    @Query("SELECT * FROM students WHERE isArchived = 0 ORDER BY name")
//    suspend fun allActive(): List<Student>
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun upsert(student: Student): Long
//}