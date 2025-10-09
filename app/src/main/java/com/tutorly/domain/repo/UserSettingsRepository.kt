package com.tutorly.domain.repo

import com.tutorly.models.UserSettings

interface UserSettingsRepository {
    suspend fun get(): UserSettings
}
