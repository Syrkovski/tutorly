package com.tutorly.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subject_presets")
data class SubjectPreset(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,                 // "Математика", "Стрижка"
    val durationMinutes: Int,         // 60, 90
    val defaultPriceCents: Int,       // 150000 = 1500.00 ₽
    val colorArgb: Int                // для метки в UI
)
