package com.tutorly.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.tutorly.models.RecurrenceRule
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurrenceRuleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(rule: RecurrenceRule): Long

    @Query(
        """
        SELECT * FROM recurrence_rules
        WHERE startDateTime < :rangeEnd
          AND (:rangeStart IS NULL OR untilDateTime IS NULL OR untilDateTime >= :rangeStart)
        """
    )
    suspend fun findIntersecting(rangeStart: Instant?, rangeEnd: Instant): List<RecurrenceRule>

    @Query("SELECT * FROM recurrence_rules WHERE id = :id")
    suspend fun findById(id: Long): RecurrenceRule?

    @Query("SELECT * FROM recurrence_rules")
    fun observeAll(): Flow<List<RecurrenceRule>>

    @Transaction
    @Query("DELETE FROM recurrence_rules WHERE id = :id")
    suspend fun deleteById(id: Long)
}
