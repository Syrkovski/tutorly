package com.tutorly.data.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE lessons ADD COLUMN markedAt INTEGER")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE lessons ADD COLUMN seriesId INTEGER")
        database.execSQL("ALTER TABLE lessons ADD COLUMN isInstance INTEGER NOT NULL DEFAULT 0")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recurrence_rules` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `baseLessonId` INTEGER NOT NULL,
                `frequency` TEXT NOT NULL,
                `interval` INTEGER NOT NULL,
                `daysOfWeek` TEXT NOT NULL,
                `startDateTime` INTEGER NOT NULL,
                `untilDateTime` INTEGER,
                `timezone` TEXT NOT NULL,
                FOREIGN KEY(`baseLessonId`) REFERENCES `lessons`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS `index_recurrence_rules_baseLessonId` ON `recurrence_rules` (`baseLessonId`)")

        database.execSQL(
            """
            CREATE TABLE IF NOT EXISTS `recurrence_exceptions` (
                `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                `seriesId` INTEGER NOT NULL,
                `originalDateTime` INTEGER NOT NULL,
                `type` TEXT NOT NULL,
                `overrideStartDateTime` INTEGER,
                `overrideDurationMinutes` INTEGER,
                `overrideNotes` TEXT,
                `overridePrice` INTEGER,
                FOREIGN KEY(`seriesId`) REFERENCES `recurrence_rules`(`id`) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        database.execSQL(
            "CREATE UNIQUE INDEX IF NOT EXISTS `index_recurrence_exceptions_seriesId_originalDateTime` ON `recurrence_exceptions` (`seriesId`, `originalDateTime`)"
        )
    }
}

val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE lessons ADD COLUMN recurrence TEXT")
    }
}
