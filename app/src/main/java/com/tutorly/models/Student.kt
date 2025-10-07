package com.tutorly.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

@Entity(
    tableName = "students",
    indices = [Index("name")]
)
data class Student(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val phone: String? = null,
    val messenger: String? = null,   // "Telegram: @user" (по желанию)
    val note: String? = null,
    val isArchived: Boolean = false,
    val createdAt: Instant = Instant.now(),
    val updatedAt: Instant = Instant.now(),
    val active: Boolean = true
)
