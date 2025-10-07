package com.tutorly.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettings(
    @PrimaryKey val singletonId: Int = 1,
    val currency: String = "RUB",
    val workDayStartMinutes: Int = 9 * 60,    // 09:00
    val workDayEndMinutes: Int = 22 * 60,     // 22:00
    val slotStepMinutes: Int = 30,            // шаг слотов
    val locale: String = "ru"
)
