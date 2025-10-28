package com.tutorly.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.DayOfWeek
import java.time.Instant

@Entity(
    tableName = "recurrence_rules",
    indices = [
        Index("baseLessonId")
    ],
    foreignKeys = [
        ForeignKey(
            entity = Lesson::class,
            parentColumns = ["id"],
            childColumns = ["baseLessonId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RecurrenceRule(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val baseLessonId: Long,
    val frequency: RecurrenceFrequency,
    val interval: Int,
    val daysOfWeek: List<DayOfWeek>,
    val startDateTime: Instant,
    val untilDateTime: Instant?,
    val timezone: String
)

@Entity(
    tableName = "recurrence_exceptions",
    indices = [
        Index(value = ["seriesId", "originalDateTime"], unique = true)
    ],
    foreignKeys = [
        ForeignKey(
            entity = RecurrenceRule::class,
            parentColumns = ["id"],
            childColumns = ["seriesId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class RecurrenceException(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val seriesId: Long,
    val originalDateTime: Instant,
    val type: RecurrenceExceptionType,
    val overrideStartDateTime: Instant?,
    val overrideDurationMinutes: Int?,
    val overrideNotes: String?,
    val overridePrice: Int?
)

enum class RecurrenceFrequency {
    WEEKLY,
    BIWEEKLY,
    MONTHLY_BY_DOW
}

enum class RecurrenceExceptionType {
    CANCELLED,
    OVERRIDDEN
}
