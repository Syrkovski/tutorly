package com.tutorly.data.repo.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.tutorly.domain.repo.UserProfileRepository
import com.tutorly.models.AppThemePreset
import com.tutorly.models.UserProfile
import java.io.IOException
import java.time.DayOfWeek
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

class PreferencesUserProfileRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : UserProfileRepository {

    override val profile: Flow<UserProfile> = dataStore.data
        .catch { error ->
            if (error is IOException) {
                emit(emptyPreferences())
            } else {
                throw error
            }
        }
        .map { preferences -> preferences.toUserProfile() }

    override suspend fun updateWorkDay(startMinutes: Int, endMinutes: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.WORK_DAY_START] = startMinutes
            prefs[Keys.WORK_DAY_END] = endMinutes
        }
    }

    override suspend fun setWeekendDays(days: Set<DayOfWeek>) {
        dataStore.edit { prefs ->
            if (days.isEmpty()) {
                prefs.remove(Keys.WEEKEND_DAYS)
            } else {
                prefs[Keys.WEEKEND_DAYS] = days.map { it.name }.toSet()
            }
        }
    }

    override suspend fun setTheme(theme: AppThemePreset) {
        dataStore.edit { prefs ->
            prefs[Keys.THEME] = theme.name
        }
    }

    private fun Preferences.toUserProfile(): UserProfile {
        val start = this[Keys.WORK_DAY_START] ?: UserProfile.DEFAULT_WORK_DAY_START
        val end = this[Keys.WORK_DAY_END] ?: UserProfile.DEFAULT_WORK_DAY_END
        val sanitizedStart = start.coerceIn(MIN_START_MINUTE, MAX_START_MINUTE)
        val sanitizedEnd = end.coerceIn(sanitizedStart + SLOT_INCREMENT_MINUTES, MAX_END_MINUTE)
        val weekend = this[Keys.WEEKEND_DAYS]
            ?.mapNotNull { value -> runCatching { DayOfWeek.valueOf(value) }.getOrNull() }
            ?.toSet()
            ?: emptySet()
        val theme = this[Keys.THEME]
            ?.let { value ->
                runCatching { AppThemePreset.valueOf(value) }.getOrNull()
                    ?: legacyTheme(value)
            }
            ?: AppThemePreset.ORIGINAL
        return UserProfile(
            workDayStartMinutes = sanitizedStart,
            workDayEndMinutes = sanitizedEnd,
            weekendDays = weekend,
            theme = theme
        )
    }

    private fun legacyTheme(value: String): AppThemePreset? = when (value) {
        "OCEAN" -> AppThemePreset.ROYAL
        "FOREST" -> AppThemePreset.PLUM
        "SUNSET" -> AppThemePreset.ORIGINAL
        else -> null
    }

    private object Keys {
        val WORK_DAY_START = intPreferencesKey("profile_work_day_start")
        val WORK_DAY_END = intPreferencesKey("profile_work_day_end")
        val WEEKEND_DAYS = stringSetPreferencesKey("profile_weekend_days")
        val THEME = stringPreferencesKey("profile_theme")
    }

    companion object {
        private const val SLOT_INCREMENT_MINUTES: Int = 30
        private const val MINUTES_IN_DAY: Int = 24 * 60
        private const val MAX_END_MINUTE: Int = MINUTES_IN_DAY
        private const val MAX_START_MINUTE: Int = MAX_END_MINUTE - SLOT_INCREMENT_MINUTES
        private const val MIN_START_MINUTE: Int = 0
    }
}
