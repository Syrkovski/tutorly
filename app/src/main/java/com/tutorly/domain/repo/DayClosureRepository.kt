package com.tutorly.domain.repo

import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface DayClosureRepository {
    fun observeDayClosed(date: LocalDate): Flow<Boolean>
    suspend fun setDayClosed(date: LocalDate, isClosed: Boolean)
}
