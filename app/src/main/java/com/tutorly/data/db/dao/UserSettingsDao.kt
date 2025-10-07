package com.tutorly.data.db.dao

import androidx.room.*
import com.tutorly.models.UserSettings

@Dao
interface UserSettingsDao {

    @Query("SELECT * FROM user_settings WHERE singletonId = 1")
    suspend fun get(): UserSettings?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun save(settings: UserSettings)

    @Query("UPDATE user_settings SET currency = :currency WHERE singletonId = 1")
    suspend fun updateCurrency(currency: String)
}
