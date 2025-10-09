package com.tutorly.domain.model

/**
 * Aggregated statistics for lessons and related payments within a time interval.
 *
 * All monetary amounts are represented in cents to preserve precision and
 * shifted to rubles only on the presentation layer.
 */
data class LessonsRangeStats(
    val totalLessons: Int,
    val paidLessons: Int,
    val debtLessons: Int,
    val earnedCents: Long
) {
    companion object {
        val EMPTY = LessonsRangeStats(
            totalLessons = 0,
            paidLessons = 0,
            debtLessons = 0,
            earnedCents = 0L
        )
    }
}
