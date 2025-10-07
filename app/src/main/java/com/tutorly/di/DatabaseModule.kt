package com.tutorly.di


import android.content.Context
import androidx.room.Room
import com.tutorly.data.db.AppDatabase
import com.tutorly.data.db.dao.*
import com.tutorly.data.repo.room.RoomLessonsRepository
import com.tutorly.data.repo.room.RoomStudentsRepository
import com.tutorly.data.repo.room.RoomSubjectPresetsRepository
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.domain.repo.SubjectPresetsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "tutorly.db")
            .fallbackToDestructiveMigration() // на MVP ок
            .build()

    @Provides fun provideStudentDao(db: AppDatabase): StudentDao = db.studentDao()
    @Provides fun provideSubjectDao(db: AppDatabase): SubjectPresetDao = db.subjectDao()
    @Provides fun provideLessonDao(db: AppDatabase): LessonDao = db.lessonDao()

    @Provides @Singleton
    fun provideStudentsRepo(dao: StudentDao): StudentsRepository = RoomStudentsRepository(dao)

    @Provides @Singleton
    fun provideSubjectsRepo(dao: SubjectPresetDao): SubjectPresetsRepository = RoomSubjectPresetsRepository(dao)

    @Provides @Singleton
    fun provideLessonsRepo(dao: LessonDao): LessonsRepository = RoomLessonsRepository(dao)
}
