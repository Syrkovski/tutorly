package com.tutorly.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tutorly.models.RecurrenceException
import java.time.Instant
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurrenceExceptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(exception: RecurrenceException): Long

    @Query(
        """
        SELECT * FROM recurrence_exceptions
        WHERE seriesId IN (:seriesIds)
        """
    )
    suspend fun findForSeries(seriesIds: List<Long>): List<RecurrenceException>

    @Query("SELECT * FROM recurrence_exceptions")
    fun observeAll(): Flow<List<RecurrenceException>>

    @Query(
        """
        DELETE FROM recurrence_exceptions
        WHERE seriesId = :seriesId AND originalDateTime = :originalStart
        """
    )
    suspend fun deleteInstance(seriesId: Long, originalStart: Instant)
}
