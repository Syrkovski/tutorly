package com.tutorly.di


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.room.Room
import com.tutorly.data.db.AppDatabase
import com.tutorly.data.db.dao.*
import com.tutorly.data.db.migrations.MIGRATION_5_6
import com.tutorly.data.repo.memory.StaticUserSettingsRepository
import com.tutorly.data.repo.preferences.PreferencesDayClosureRepository
import com.tutorly.data.repo.preferences.PreferencesUserProfileRepository
import com.tutorly.data.repo.room.RoomLessonsRepository
import com.tutorly.data.repo.room.RoomPaymentsRepository
import com.tutorly.data.repo.room.RoomStudentsRepository
import com.tutorly.data.repo.room.RoomSubjectPresetsRepository
import com.tutorly.data.repo.room.StudentPrepaymentAllocator
import com.tutorly.domain.repo.DayClosureRepository
import com.tutorly.domain.repo.LessonsRepository
import com.tutorly.domain.repo.PaymentsRepository
import com.tutorly.domain.repo.StudentsRepository
import com.tutorly.domain.repo.SubjectPresetsRepository
import com.tutorly.domain.repo.UserProfileRepository
import com.tutorly.domain.repo.UserSettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides @Singleton
    fun provideDb(@ApplicationContext ctx: Context): AppDatabase =
        Room.databaseBuilder(ctx, AppDatabase::class.java, "tutorly.db")
            .addMigrations(MIGRATION_5_6)
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
    fun providePrepaymentAllocator(
        lessonDao: LessonDao,
        paymentDao: PaymentDao
    ): StudentPrepaymentAllocator = StudentPrepaymentAllocator(lessonDao, paymentDao)

    @Provides @Singleton
    fun providePaymentsRepo(
        paymentDao: PaymentDao,
        prepaymentAllocator: StudentPrepaymentAllocator
    ): PaymentsRepository = RoomPaymentsRepository(paymentDao, prepaymentAllocator)

    @Provides @Singleton
    fun provideSubjectsRepo(dao: SubjectPresetDao): SubjectPresetsRepository = RoomSubjectPresetsRepository(dao)

    @Provides @Singleton
    fun provideLessonsRepo(
        lessonDao: LessonDao,
        paymentDao: PaymentDao,
        prepaymentAllocator: StudentPrepaymentAllocator
    ): LessonsRepository = RoomLessonsRepository(lessonDao, paymentDao, prepaymentAllocator)

    @Provides @Singleton
    fun provideUserSettingsRepo(): UserSettingsRepository = StaticUserSettingsRepository()

    @Provides @Singleton
    fun provideUserProfileRepository(
        dataStore: DataStore<Preferences>
    ): UserProfileRepository = PreferencesUserProfileRepository(dataStore)

    @Provides @Singleton
    fun providePreferencesDataStore(@ApplicationContext ctx: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        ) {
            ctx.preferencesDataStoreFile("today_prefs")
        }

    @Provides @Singleton
    fun provideDayClosureRepository(
        dataStore: DataStore<Preferences>
    ): DayClosureRepository = PreferencesDayClosureRepository(dataStore)
}
