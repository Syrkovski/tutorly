package com.tutorly.domain.repo

import com.tutorly.models.AppThemePreset
import com.tutorly.models.UserProfile
import java.time.DayOfWeek
import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {
    val profile: Flow<UserProfile>

    suspend fun updateWorkDay(startMinutes: Int, endMinutes: Int)

    suspend fun setWeekendDays(days: Set<DayOfWeek>)

    suspend fun setTheme(theme: AppThemePreset)

    suspend fun setOnboardingCompleted(completed: Boolean)
}
