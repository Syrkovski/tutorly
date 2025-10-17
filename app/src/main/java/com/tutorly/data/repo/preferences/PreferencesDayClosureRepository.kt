package com.tutorly.data.repo.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.tutorly.domain.repo.DayClosureRepository
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

@Singleton
class PreferencesDayClosureRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : DayClosureRepository {

    override fun observeDayClosed(date: LocalDate): Flow<Boolean> {
        val expected = DATE_FORMATTER.format(date)
        return dataStore.data
            .map { preferences ->
                val stored = preferences[CLOSED_DAY_KEY]
                stored != null && stored == expected
            }
            .distinctUntilChanged()
    }

    override suspend fun setDayClosed(date: LocalDate, isClosed: Boolean) {
        val target = DATE_FORMATTER.format(date)
        dataStore.edit { preferences ->
            if (isClosed) {
                preferences[CLOSED_DAY_KEY] = target
            } else {
                val stored = preferences[CLOSED_DAY_KEY]
                if (stored == target) {
                    preferences.remove(CLOSED_DAY_KEY)
                }
            }
        }
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private val CLOSED_DAY_KEY = stringPreferencesKey("today_closed_day")
    }
}
