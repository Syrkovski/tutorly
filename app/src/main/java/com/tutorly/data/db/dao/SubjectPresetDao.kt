package com.tutorly.data.db.dao

import androidx.room.*
import com.tutorly.models.SubjectPreset

@Dao
interface SubjectPresetDao {
    @Query("SELECT * FROM subject_presets ORDER BY name")
    suspend fun all(): List<SubjectPreset>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(preset: SubjectPreset): Long
}


//@Dao
//interface SubjectPresetDao {
//
//    // Получить все предметы (для выбора в форме)
//    @Query("SELECT * FROM subject_presets ORDER BY name")
//    suspend fun getAll(): List<SubjectPreset>
//
//    // Найти по id
//    @Query("SELECT * FROM subject_presets WHERE id = :id")
//    suspend fun getById(id: Long): SubjectPreset?
//
//    // Добавить или обновить
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun upsert(preset: SubjectPreset): Long
//
//    // Удалить
//    @Delete
//    suspend fun delete(preset: SubjectPreset)
//
//    // Массовая вставка (например, для дефолтных предметов)
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertAll(list: List<SubjectPreset>)
//}
