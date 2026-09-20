package com.example.util

import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeUtils {
    enum class DateState {
        TODAY,
        PAST,
        FUTURE
    }

    fun getUserZoneId(configuredZone: String? = null): ZoneId {
        return try {
            if (!configuredZone.isNullOrBlank()) {
                ZoneId.of(configuredZone)
            } else {
                ZoneId.systemDefault()
            }
        } catch (_: Exception) {
            ZoneId.systemDefault()
        }
    }

    fun getToday(configuredZone: String? = null): LocalDate {
        return LocalDate.now(getUserZoneId(configuredZone))
    }

    fun classifyDate(date: LocalDate, today: LocalDate): DateState {
        return when {
            date == today -> DateState.TODAY
            date.isBefore(today) -> DateState.PAST
            else -> DateState.FUTURE
        }
    }

    fun isAllowedForJournal(date: LocalDate, today: LocalDate): Boolean {
        return !date.isAfter(today)
    }

    fun toIsoDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }

    fun parseIsoDate(dateStr: String): LocalDate? {
        return try {
            LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            null
        }
    }

    fun formatDisplayDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"))
    }

    fun formatShortDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("MMMM d, yyyy"))
    }

    fun formatDayBucketLabel(date: LocalDate, today: LocalDate): String {
        return when {
            date == today -> "Today"
            date == today.minusDays(1) -> "Yesterday"
            date.year == today.year -> date.format(DateTimeFormatter.ofPattern("MMM d"))
            else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy"))
        }
    }

    fun formatFullDayDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy"))
    }
}
