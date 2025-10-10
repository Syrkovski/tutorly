package com.tutorly.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tutorly.data.db.converters.EnumConverters
import com.tutorly.data.db.converters.InstantConverter
import com.tutorly.data.db.dao.*
import com.tutorly.models.*

@Database(
    entities = [
        Student::class,
        Lesson::class,
        Payment::class,
        SubjectPreset::class
    ],
    version = 3, // ↑ увеличь версию
    exportSchema = true
)
@TypeConverters(InstantConverter::class, EnumConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun studentDao(): StudentDao
    abstract fun lessonDao(): LessonDao
    abstract fun paymentDao(): PaymentDao
    abstract fun subjectPresetDao(): SubjectPresetDao

    /**
     * Backwards-compatible accessor for subject presets DAO.
     * `subjectPresetDao()` is the Room-generated method, but a shorter name is still
     * referenced across the codebase (e.g. in DI modules).
     */
    fun subjectDao(): SubjectPresetDao = subjectPresetDao()
}



//@Database(
//    entities = [
//        Student::class,
//        SubjectPreset::class,
//        Lesson::class,
//        Payment::class,       // можно убрать на MVP, если не используешь
//        UserSettings::class
//    ],
//    version = 1,
//    exportSchema = true
//)
//@TypeConverters(InstantConverter::class, EnumConverters::class)
//abstract class AppDatabase : RoomDatabase() {
//    abstract fun studentDao(): StudentDao
//    abstract fun subjectDao(): SubjectPresetDao
//    abstract fun lessonDao(): LessonDao
//    abstract fun paymentDao(): PaymentDao
//    abstract fun settingsDao(): UserSettingsDao
//}
