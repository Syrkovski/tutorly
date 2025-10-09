package com.tutorly.data.repo.memory

import com.tutorly.domain.repo.UserSettingsRepository
import com.tutorly.models.UserSettings

class StaticUserSettingsRepository : UserSettingsRepository {
    override suspend fun get(): UserSettings = UserSettings()
}
