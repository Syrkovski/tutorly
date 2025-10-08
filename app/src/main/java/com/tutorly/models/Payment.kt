package com.tutorly.models

import androidx.room.*
import java.time.Instant



@Entity(
    tableName = "payments",
    indices = [Index("lessonId"), Index("studentId"), Index("at")],
    foreignKeys = [
        ForeignKey(
            entity = Lesson::class,
            parentColumns = ["id"],
            childColumns = ["lessonId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Student::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val lessonId: Long?,
    val studentId: Long,
    val amountCents: Int,
    val method: String? = null,       // "cash", "transfer", ...
    val at: Instant = Instant.now(),
    val note: String? = null,
    val status: PaymentStatus = PaymentStatus.UNPAID
)


