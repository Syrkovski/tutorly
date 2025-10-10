package com.tutorly.di


import android.content.Context
import androidx.room.Room
import com.tutorly.data.db.AppDatabase
import com.tutorly.data.db.dao.*
import com.tutorly.data.repo.memory.StaticUserSettingsRepository
import com.tutorly.data.repo.room.RoomLessonsRepository
import com.tutorly.data.repo.room.RoomPaymentsRepository
import com.tutorly.data.repo.room.RoomStudentsRepository
import com.tutorly.data.repo.room.RoomSubjectPresetsRepository
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.domain.repo.PaymentsRepository
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.domain.repo.SubjectPresetsRepository
import com.tutorly.domain.repo.UserSettingsRepository
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
    @Provides fun providePaymentDao(db: AppDatabase): PaymentDao = db.paymentDao()

    @Provides @Singleton
    fun provideStudentsRepo(
        studentDao: StudentDao,
        paymentDao: PaymentDao,
        lessonDao: LessonDao
    ): StudentsRepository = RoomStudentsRepository(studentDao, paymentDao, lessonDao)

    @Provides @Singleton
    fun providePaymentsRepo(
        paymentDao: PaymentDao
    ): PaymentsRepository = RoomPaymentsRepository(paymentDao)

    @Provides @Singleton
    fun provideSubjectsRepo(dao: SubjectPresetDao): SubjectPresetsRepository = RoomSubjectPresetsRepository(dao)

    @Provides @Singleton
    fun provideLessonsRepo(
        lessonDao: LessonDao,
        paymentDao: PaymentDao
    ): LessonsRepository = RoomLessonsRepository(lessonDao, paymentDao)

    @Provides @Singleton
    fun provideUserSettingsRepo(): UserSettingsRepository = StaticUserSettingsRepository()
}
