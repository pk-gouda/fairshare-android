package com.prathik.fairshare.util

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Utility for detecting the user's country and suggesting a default currency.
 *
 * Why coarse location and not GPS:
 * - We only need country-level accuracy for currency detection
 * - Coarse location uses cell towers / WiFi — less battery drain
 * - Fine location (GPS) would be invasive and unnecessary
 *
 * Permission strategy:
 * - On first launch: use device locale (no permission needed)
 * - When creating a TRIP group: ask for coarse location permission
 *   The ask feels contextual — "to auto-detect local currency while traveling"
 * - Never ask upfront before any action
 *
 * Fallback chain:
 * 1. Physical location (if permission granted)
 * 2. Device locale
 * 3. "USD" hardcoded default
 */
@Singleton
class LocationHelper @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    /**
     * Returns true if coarse location permission has been granted.
     */
    fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Returns the ISO 3166-1 alpha-2 country code for the user's
     * current physical location.
     *
     * Requires ACCESS_COARSE_LOCATION permission.
     * Returns null if permission denied, location unavailable,
     * or Geocoder fails.
     *
     * Example: "US", "IN", "JP", "DE"
     *
     * Lint note: getLastKnownLocation triggers a [MissingPermission] check that
     * lint cannot satisfy statically because the permission gate is our own
     * hasLocationPermission() helper (lint only recognises inline
     * checkSelfPermission calls). The permission IS verified before any location
     * API is touched, and the whole body is wrapped in a try/catch that also
     * absorbs SecurityException, so the call is safe. The suppression is scoped
     * to this single function only.
     */
    @SuppressLint("MissingPermission")
    fun getCurrentCountryCode(): String? {
        if (!hasLocationPermission()) return null

        return try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE)
                    as LocationManager

            val providers = listOf(
                LocationManager.NETWORK_PROVIDER,
                LocationManager.GPS_PROVIDER,
                LocationManager.PASSIVE_PROVIDER,
            )

            var location: android.location.Location? = null
            for (provider in providers) {
                if (locationManager.isProviderEnabled(provider)) {
                    location = locationManager.getLastKnownLocation(provider)
                    if (location != null) break
                }
            }

            if (location == null) return null

            val geocoder = Geocoder(context, Locale.getDefault())

            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            addresses?.firstOrNull()?.countryCode
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Returns the suggested currency code based on the user's location.
     *
     * Fallback chain:
     * 1. Physical location country → currency mapping
     * 2. Device locale → currency
     * 3. "USD" default
     */
    fun getSuggestedCurrencyCode(): String {
        val countryCode = getCurrentCountryCode()
            ?: Locale.getDefault().country.takeIf { it.isNotBlank() }

        return if (countryCode != null) {
            getCurrencyForCountry(countryCode)
        } else {
            MoneyUtils.getDefaultCurrencyCode()
        }
    }

    /**
     * Maps ISO country codes to their primary currency code.
     * Covers the most common countries FairShare users are likely in.
     * Falls back to device locale currency for unmapped countries.
     */
    private fun getCurrencyForCountry(countryCode: String): String {
        return countryToCurrency[countryCode.uppercase()]
            ?: MoneyUtils.getDefaultCurrencyCode()
    }

    companion object {
        /**
         * Maps ISO 3166-1 alpha-2 country codes to ISO 4217 currency codes.
         * Covers 60+ countries representing the vast majority of global users.
         */
        val countryToCurrency = mapOf(
            // Americas
            "US" to "USD", "CA" to "CAD", "MX" to "MXN",
            "BR" to "BRL", "AR" to "ARS", "CO" to "COP",
            "CL" to "CLP", "PE" to "PEN", "VE" to "VES",

            // Europe
            "DE" to "EUR", "FR" to "EUR", "IT" to "EUR",
            "ES" to "EUR", "PT" to "EUR", "NL" to "EUR",
            "BE" to "EUR", "AT" to "EUR", "GR" to "EUR",
            "FI" to "EUR", "IE" to "EUR", "LU" to "EUR",
            "GB" to "GBP", "CH" to "CHF", "SE" to "SEK",
            "NO" to "NOK", "DK" to "DKK", "PL" to "PLN",
            "CZ" to "CZK", "HU" to "HUF", "RO" to "RON",
            "RU" to "RUB", "UA" to "UAH", "TR" to "TRY",

            // Asia Pacific
            "JP" to "JPY", "CN" to "CNY", "IN" to "INR",
            "KR" to "KRW", "AU" to "AUD", "NZ" to "NZD",
            "SG" to "SGD", "HK" to "HKD", "TW" to "TWD",
            "TH" to "THB", "MY" to "MYR", "ID" to "IDR",
            "PH" to "PHP", "VN" to "VND", "PK" to "PKR",
            "BD" to "BDT", "LK" to "LKR", "NP" to "NPR",

            // Middle East & Africa
            "AE" to "AED", "SA" to "SAR", "QA" to "QAR",
            "KW" to "KWD", "BH" to "BHD", "OM" to "OMR",
            "IL" to "ILS", "EG" to "EGP", "ZA" to "ZAR",
            "NG" to "NGN", "KE" to "KES", "GH" to "GHS",
            "MA" to "MAD", "TZ" to "TZS", "ET" to "ETB",
        )
    }
}