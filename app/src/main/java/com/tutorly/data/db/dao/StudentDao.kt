package com.tutorly.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tutorly.models.Student
import kotlinx.coroutines.flow.Flow

@Dao
interface StudentDao {
    @Query(
        """
        SELECT * FROM students
        WHERE active = 1 AND isArchived = 0
        ORDER BY name COLLATE NOCASE
        """
    )
    suspend fun getAllActive(): List<Student>

    @Query(
        """
        SELECT * FROM students
        WHERE active = 1 AND isArchived = 0 AND (
            :q == '' OR name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%'
        )
        ORDER BY name COLLATE NOCASE
        """
    )
    suspend fun searchActive(q: String): List<Student>

    @Query("SELECT * FROM students WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): Student?

    @Query(
        """
        SELECT * FROM students
        WHERE name = :name
        COLLATE NOCASE
        LIMIT 1
        """
    )
    suspend fun findByName(name: String): Student?

    @Query(
        """
        SELECT * FROM students
        WHERE active = 1 AND isArchived = 0 AND (
            :q == '' OR name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%'
        )
        ORDER BY name COLLATE NOCASE
        """
    )
    fun observeStudents(q: String): Flow<List<Student>>

    @Query(
        """
        SELECT * FROM students
        WHERE isArchived = 1 AND (
            :q == '' OR name LIKE '%' || :q || '%' OR phone LIKE '%' || :q || '%'
        )
        ORDER BY name COLLATE NOCASE
        """
    )
    fun observeArchivedStudents(q: String): Flow<List<Student>>

    @Query("SELECT * FROM students WHERE id = :id")
    fun observeStudent(id: Long): Flow<Student?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(student: Student): Long

    @Update
    suspend fun update(student: Student)

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
