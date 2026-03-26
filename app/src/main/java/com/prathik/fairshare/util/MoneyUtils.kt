package com.prathik.fairshare.util

import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Utility object for formatting monetary amounts correctly for a global app.
 *
 * Why not just prepend "$":
 * - India uses ₹1,23,456.78 (different grouping)
 * - Germany uses 1.234,56 € (symbol after amount, comma decimal)
 * - Japan uses ¥1,234 (no decimal places)
 * - Switzerland uses CHF 1'234.56 (apostrophe separator)
 *
 * Always use these functions for display — never format money manually.
 */
object MoneyUtils {

    /**
     * Formats a Double amount to a locale-aware currency string.
     *
     * Examples:
     * - format(1234.5, "USD") → "$1,234.50" (en-US)
     * - format(1234.5, "EUR") → "1.234,50 €" (de-DE)
     * - format(1234.5, "INR") → "₹1,234.50" (en-IN)
     * - format(1234.5, "JPY") → "¥1,235" (ja-JP, no decimals)
     */
    fun format(amount: Double, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        return try {
            val currency = Currency.getInstance(currencyCode)
            val formatter = NumberFormat.getCurrencyInstance(locale).apply {
                this.currency = currency
                maximumFractionDigits = currency.defaultFractionDigits
                minimumFractionDigits = currency.defaultFractionDigits
                roundingMode = RoundingMode.HALF_UP
            }
            formatter.format(amount)
        } catch (e: Exception) {
            // Fallback if currency code is invalid
            "$currencyCode ${formatPlain(amount)}"
        }
    }

    /**
     * Formats amount without currency symbol — used for input fields
     * where we show the symbol separately.
     *
     * Example: formatPlain(1234.56) → "1,234.56"
     */
    fun formatPlain(amount: Double, locale: Locale = Locale.getDefault()): String {
        return try {
            val formatter = NumberFormat.getNumberInstance(locale).apply {
                minimumFractionDigits = 2
                maximumFractionDigits = 2
                roundingMode = RoundingMode.HALF_UP
            }
            formatter.format(amount)
        } catch (e: Exception) {
            "%.2f".format(amount)
        }
    }

    /**
     * Returns the currency symbol for a given currency code.
     * Example: getSymbol("USD") → "$"
     *          getSymbol("EUR") → "€"
     *          getSymbol("INR") → "₹"
     */
    fun getSymbol(currencyCode: String, locale: Locale = Locale.getDefault()): String {
        return try {
            Currency.getInstance(currencyCode).getSymbol(locale)
        } catch (e: Exception) {
            currencyCode
        }
    }

    /**
     * Returns the default currency code for the device's locale.
     * Used as the initial currency suggestion on first launch.
     *
     * Example: device set to en-IN → "INR"
     *          device set to ja-JP → "JPY"
     *          device set to en-US → "USD"
     */
    fun getDefaultCurrencyCode(locale: Locale = Locale.getDefault()): String {
        return try {
            Currency.getInstance(locale).currencyCode
        } catch (e: Exception) {
            "USD" // safe fallback
        }
    }

    /**
     * Parses a user-entered string to a Double amount.
     * Handles locale-specific decimal separators.
     *
     * Example: "1,234.56" → 1234.56 (en-US)
     *          "1.234,56" → 1234.56 (de-DE)
     */
    fun parse(input: String, locale: Locale = Locale.getDefault()): Double? {
        return try {
            val formatter = NumberFormat.getNumberInstance(locale)
            formatter.parse(input.trim())?.toDouble()
        } catch (e: Exception) {
            // Try plain parsing as fallback
            input.trim().replace(",", "").toDoubleOrNull()
        }
    }

    /**
     * Formats the sign-aware balance for display.
     * Positive = green "you are owed"
     * Negative = orange "you owe"
     *
     * Example: formatBalance(50.0, "USD") → "+$50.00"
     *          formatBalance(-30.0, "USD") → "-$30.00"
     */
    fun formatBalance(amount: Double, currencyCode: String, locale: Locale = Locale.getDefault()): String {
        val formatted = format(kotlin.math.abs(amount), currencyCode, locale)
        return when {
            amount > 0 -> "+$formatted"
            amount < 0 -> "-$formatted"
            else -> formatted
        }
    }

    /**
     * Returns true if the amount is effectively zero
     * (within floating point tolerance).
     */
    fun isZero(amount: Double): Boolean = kotlin.math.abs(amount) < 0.001
}