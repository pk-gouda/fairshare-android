package com.prathik.fairshare.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Utility object for parsing and formatting timestamps for a global app.
 *
 * Why this matters:
 * - Backend sends UTC timestamps as ISO strings: "2026-03-25T14:30:00"
 * - A user in Mumbai (UTC+5:30) should see "7:00 PM" not "1:30 PM"
 * - A user in New York (UTC-5) should see "9:30 AM" not "2:30 PM"
 * - Date formats differ: US uses Mar 25, 2026 — Europe uses 25.03.2026
 *
 * Always use ZoneId.systemDefault() for display so timestamps
 * reflect the user's local time automatically.
 */
object DateFormatter {

    /**
     * Parses an ISO 8601 timestamp string from the backend to ZonedDateTime
     * in the user's local timezone.
     *
     * Input:  "2026-03-25T14:30:00" (UTC from backend)
     * Output: ZonedDateTime in device's local timezone
     */
    fun parseTimestamp(isoString: String): ZonedDateTime? {
        return try {
            val instant = Instant.parse(
                if (isoString.endsWith("Z") || isoString.contains("+"))
                    isoString
                else
                    "${isoString}Z"
            )
            instant.atZone(ZoneId.systemDefault())
        } catch (e: Exception) {
            try {
                // Try without Z suffix for backends that omit it
                ZonedDateTime.parse(isoString, DateTimeFormatter.ISO_DATE_TIME)
                    .withZoneSameInstant(ZoneId.systemDefault())
            } catch (e2: Exception) {
                null
            }
        }
    }

    /**
     * Parses a date-only string from the backend (used for tripStartDate/tripEndDate).
     *
     * Input:  "2026-03-25"
     * Output: LocalDate
     */
    fun parseDate(dateString: String): LocalDate? {
        return try {
            LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns a relative time string for the Activity feed and expense rows.
     * Uses the user's locale for language.
     *
     * Examples:
     * - Just now        (< 1 minute ago)
     * - 5 minutes ago   (< 1 hour ago)
     * - 2 hours ago     (< 24 hours ago)
     * - Yesterday       (1 day ago)
     * - 3 days ago      (< 7 days ago)
     * - Mar 15          (< 1 year ago)
     * - Mar 15, 2025    (> 1 year ago)
     */
    fun toRelativeTime(isoString: String, locale: Locale = Locale.getDefault()): String {
        val dateTime = parseTimestamp(isoString) ?: return ""
        val now = ZonedDateTime.now(ZoneId.systemDefault())
        val seconds = java.time.Duration.between(dateTime, now).seconds

        return when {
            seconds < 60        -> "Just now"
            seconds < 3600      -> "${seconds / 60} ${if (seconds / 60 == 1L) "minute" else "minutes"} ago"
            seconds < 86400     -> "${seconds / 3600} ${if (seconds / 3600 == 1L) "hour" else "hours"} ago"
            seconds < 172800    -> "Yesterday"
            seconds < 604800    -> "${seconds / 86400} days ago"
            seconds < 31536000  -> dateTime.format(
                DateTimeFormatter.ofPattern("MMM d", locale)
            )
            else                -> dateTime.format(
                DateTimeFormatter.ofPattern("MMM d, yyyy", locale)
            )
        }
    }

    /**
     * Formats a timestamp for the expense detail screen.
     * Uses locale-aware medium date format.
     *
     * Example (en-US): "Mar 25, 2026 at 2:30 PM"
     * Example (de-DE): "25. März 2026 um 14:30"
     */
    fun toExpenseDate(isoString: String, locale: Locale = Locale.getDefault()): String {
        val dateTime = parseTimestamp(isoString) ?: return ""
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        val timeFormatter = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)
        return "${dateTime.format(dateFormatter)} at ${dateTime.format(timeFormatter)}"
    }

    /**
     * Formats a date for display in expense rows and section headers.
     *
     * Example (en-US): "Mar 25, 2026"
     * Example (ja-JP): "2026年3月25日"
     */
    fun toDisplayDate(isoString: String, locale: Locale = Locale.getDefault()): String {
        val dateTime = parseTimestamp(isoString) ?: return ""
        return dateTime.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        )
    }

    /**
     * Formats a LocalDate for display (used for tripStartDate/tripEndDate).
     *
     * Example (en-US): "Mar 25, 2026"
     */
    fun toDisplayDate(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        return date.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        )
    }

    /**
     * Returns the section header label for grouping expenses by date.
     * Used in GroupDetailScreen expense list.
     *
     * Examples: "This month", "Last month", "February 2026", "Older"
     */
    fun toSectionHeader(isoString: String, locale: Locale = Locale.getDefault()): String {
        val dateTime = parseTimestamp(isoString) ?: return "Older"
        val now = ZonedDateTime.now(ZoneId.systemDefault())

        return when {
            dateTime.year == now.year && dateTime.month == now.month ->
                "This month"
            dateTime.year == now.year && dateTime.month == now.month.minus(1) ->
                "Last month"
            dateTime.year == now.year ->
                dateTime.format(DateTimeFormatter.ofPattern("MMMM yyyy", locale))
            else ->
                "Older"
        }
    }

    /**
     * Formats a timestamp to ISO 8601 string for sending to the backend.
     * Always sends in UTC.
     *
     * Example: "2026-03-25T14:30:00Z"
     */
    fun toIsoString(dateTime: ZonedDateTime): String {
        return dateTime.withZoneSameInstant(ZoneId.of("UTC"))
            .format(DateTimeFormatter.ISO_INSTANT)
    }

    /**
     * Formats a LocalDate to ISO date string for sending to the backend.
     * Used for tripStartDate/tripEndDate in CreateGroupRequest.
     *
     * Example: "2026-03-25"
     */
    fun toIsoDate(date: LocalDate): String {
        return date.format(DateTimeFormatter.ISO_LOCAL_DATE)
    }
}