package com.tutorly.models

import java.time.DayOfWeek

/**
 * Stores personalized application preferences that should persist across sessions.
 */
data class UserProfile(
    val workDayStartMinutes: Int = DEFAULT_WORK_DAY_START,
    val workDayEndMinutes: Int = DEFAULT_WORK_DAY_END,
    val weekendDays: Set<DayOfWeek> = emptySet(),
    val theme: AppThemePreset = AppThemePreset.ORIGINAL,
    val onboardingCompleted: Boolean = false
) {
    companion object {
        const val DEFAULT_WORK_DAY_START: Int = 9 * 60
        const val DEFAULT_WORK_DAY_END: Int = 22 * 60
    }
}

enum class AppThemePreset {
    ORIGINAL,
    PLUM,
    ROYAL
}
