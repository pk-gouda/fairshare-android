package com.prathik.fairshare.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prathik.fairshare.ui.components.FsTextField
import com.prathik.fairshare.ui.components.FsTopBar
import com.prathik.fairshare.ui.theme.Green400
import com.prathik.fairshare.ui.theme.Spacing
import com.prathik.fairshare.ui.theme.Surface0
import com.prathik.fairshare.ui.theme.Surface2
import com.prathik.fairshare.ui.theme.Surface4
import com.prathik.fairshare.ui.theme.TextPrimary
import com.prathik.fairshare.ui.theme.TextSecondary

/**
 * Currency Select Screen.
 *
 * Full page list of all currencies with country name.
 * Search bar at top filters by currency code or country name.
 * Tapping a currency calls onSelect and pops back.
 */
@Composable
fun CurrencySelectScreen(
    currentCurrency: String,
    onSelect       : (String) -> Unit,
    onBack         : () -> Unit,
) {
    var query by remember { mutableStateOf("") }

    val filtered = remember(query) {
        ALL_CURRENCIES.filter { (code, country) ->
            query.isBlank() ||
                    code.contains(query, ignoreCase = true) ||
                    country.contains(query, ignoreCase = true)
        }
    }

    Scaffold(
        containerColor = Surface0,
        topBar = {
            FsTopBar(
                title  = "Select currency",
                onBack = onBack,
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // Search bar
            FsTextField(
                value         = query,
                onValueChange = { query = it },
                label         = "Search or select a currency",
                imeAction     = ImeAction.Search,
                keyboardType  = KeyboardType.Text,
                modifier      = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
            )

            HorizontalDivider(color = Surface4)

            LazyColumn {
                items(filtered) { (code, country) ->
                    val isSelected = code == currentCurrency
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(code); onBack() }
                            .background(if (isSelected) Surface2 else Surface0)
                            .padding(horizontal = Spacing.lg, vertical = Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text       = code,
                            fontSize   = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color      = if (isSelected) Green400 else TextPrimary,
                            modifier   = Modifier.width(60.sp.value.dp),
                        )
                        Spacer(modifier = Modifier.width(Spacing.lg))
                        Text(
                            text     = country,
                            fontSize = 15.sp,
                            color    = if (isSelected) Green400 else TextSecondary,
                        )
                    }
                    HorizontalDivider(color = Surface4, thickness = 0.5.sp.value.dp)
                }
            }
        }
    }
}

// ── Currency list ─────────────────────────────────────────────────────────────

private val ALL_CURRENCIES = listOf(
    "AED" to "United Arab Emirates Dirham",
    "AFN" to "Afghan Afghani",
    "ALL" to "Albanian Lek",
    "AMD" to "Armenian Dram",
    "ANG" to "Netherlands Antillean Guilder",
    "AOA" to "Angolan Kwanza",
    "ARS" to "Argentine Peso",
    "AUD" to "Australian Dollar",
    "AWG" to "Aruban Florin",
    "AZN" to "Azerbaijani Manat",
    "BAM" to "Bosnia-Herzegovina Convertible Mark",
    "BBD" to "Barbadian Dollar",
    "BDT" to "Bangladeshi Taka",
    "BGN" to "Bulgarian Lev",
    "BHD" to "Bahraini Dinar",
    "BIF" to "Burundian Franc",
    "BMD" to "Bermudian Dollar",
    "BND" to "Brunei Dollar",
    "BOB" to "Bolivian Boliviano",
    "BRL" to "Brazilian Real",
    "BSD" to "Bahamian Dollar",
    "BTN" to "Bhutanese Ngultrum",
    "BWP" to "Botswanan Pula",
    "BYN" to "Belarusian Ruble",
    "BZD" to "Belize Dollar",
    "CAD" to "Canadian Dollar",
    "CDF" to "Congolese Franc",
    "CHF" to "Swiss Franc",
    "CLP" to "Chilean Peso",
    "CNY" to "Chinese Yuan",
    "COP" to "Colombian Peso",
    "CRC" to "Costa Rican Colón",
    "CUP" to "Cuban Peso",
    "CVE" to "Cape Verdean Escudo",
    "CZK" to "Czech Koruna",
    "DJF" to "Djiboutian Franc",
    "DKK" to "Danish Krone",
    "DOP" to "Dominican Peso",
    "DZD" to "Algerian Dinar",
    "EGP" to "Egyptian Pound",
    "ERN" to "Eritrean Nakfa",
    "ETB" to "Ethiopian Birr",
    "EUR" to "Euro",
    "FJD" to "Fijian Dollar",
    "FKP" to "Falkland Islands Pound",
    "GBP" to "British Pound Sterling",
    "GEL" to "Georgian Lari",
    "GHS" to "Ghanaian Cedi",
    "GIP" to "Gibraltar Pound",
    "GMD" to "Gambian Dalasi",
    "GNF" to "Guinean Franc",
    "GTQ" to "Guatemalan Quetzal",
    "GYD" to "Guyanaese Dollar",
    "HKD" to "Hong Kong Dollar",
    "HNL" to "Honduran Lempira",
    "HRK" to "Croatian Kuna",
    "HTG" to "Haitian Gourde",
    "HUF" to "Hungarian Forint",
    "IDR" to "Indonesian Rupiah",
    "ILS" to "Israeli New Shekel",
    "INR" to "Indian Rupee",
    "IQD" to "Iraqi Dinar",
    "IRR" to "Iranian Rial",
    "ISK" to "Icelandic Króna",
    "JMD" to "Jamaican Dollar",
    "JOD" to "Jordanian Dinar",
    "JPY" to "Japanese Yen",
    "KES" to "Kenyan Shilling",
    "KGS" to "Kyrgystani Som",
    "KHR" to "Cambodian Riel",
    "KMF" to "Comorian Franc",
    "KPW" to "North Korean Won",
    "KRW" to "South Korean Won",
    "KWD" to "Kuwaiti Dinar",
    "KYD" to "Cayman Islands Dollar",
    "KZT" to "Kazakhstani Tenge",
    "LAK" to "Laotian Kip",
    "LBP" to "Lebanese Pound",
    "LKR" to "Sri Lankan Rupee",
    "LRD" to "Liberian Dollar",
    "LSL" to "Lesotho Loti",
    "LYD" to "Libyan Dinar",
    "MAD" to "Moroccan Dirham",
    "MDL" to "Moldovan Leu",
    "MGA" to "Malagasy Ariary",
    "MKD" to "Macedonian Denar",
    "MMK" to "Myanmar Kyat",
    "MNT" to "Mongolian Tugrik",
    "MOP" to "Macanese Pataca",
    "MRU" to "Mauritanian Ouguiya",
    "MUR" to "Mauritian Rupee",
    "MVR" to "Maldivian Rufiyaa",
    "MWK" to "Malawian Kwacha",
    "MXN" to "Mexican Peso",
    "MYR" to "Malaysian Ringgit",
    "MZN" to "Mozambican Metical",
    "NAD" to "Namibian Dollar",
    "NGN" to "Nigerian Naira",
    "NIO" to "Nicaraguan Córdoba",
    "NOK" to "Norwegian Krone",
    "NPR" to "Nepalese Rupee",
    "NZD" to "New Zealand Dollar",
    "OMR" to "Omani Rial",
    "PAB" to "Panamanian Balboa",
    "PEN" to "Peruvian Sol",
    "PGK" to "Papua New Guinean Kina",
    "PHP" to "Philippine Peso",
    "PKR" to "Pakistani Rupee",
    "PLN" to "Polish Zloty",
    "PYG" to "Paraguayan Guarani",
    "QAR" to "Qatari Riyal",
    "RON" to "Romanian Leu",
    "RSD" to "Serbian Dinar",
    "RUB" to "Russian Ruble",
    "RWF" to "Rwandan Franc",
    "SAR" to "Saudi Riyal",
    "SBD" to "Solomon Islands Dollar",
    "SCR" to "Seychellois Rupee",
    "SDG" to "Sudanese Pound",
    "SEK" to "Swedish Krona",
    "SGD" to "Singapore Dollar",
    "SHP" to "Saint Helena Pound",
    "SLL" to "Sierra Leonean Leone",
    "SOS" to "Somali Shilling",
    "SRD" to "Surinamese Dollar",
    "STN" to "São Tomé and Príncipe Dobra",
    "SVC" to "Salvadoran Colón",
    "SYP" to "Syrian Pound",
    "SZL" to "Swazi Lilangeni",
    "THB" to "Thai Baht",
    "TJS" to "Tajikistani Somoni",
    "TMT" to "Turkmenistani Manat",
    "TND" to "Tunisian Dinar",
    "TOP" to "Tongan Paʻanga",
    "TRY" to "Turkish Lira",
    "TTD" to "Trinidad and Tobago Dollar",
    "TWD" to "New Taiwan Dollar",
    "TZS" to "Tanzanian Shilling",
    "UAH" to "Ukrainian Hryvnia",
    "UGX" to "Ugandan Shilling",
    "USD" to "United States Dollar",
    "UYU" to "Uruguayan Peso",
    "UZS" to "Uzbekistani Som",
    "VES" to "Venezuelan Bolívar",
    "VND" to "Vietnamese Dong",
    "VUV" to "Vanuatu Vatu",
    "WST" to "Samoan Tala",
    "XAF" to "Central African CFA Franc",
    "XCD" to "East Caribbean Dollar",
    "XOF" to "West African CFA Franc",
    "XPF" to "CFP Franc",
    "YER" to "Yemeni Rial",
    "ZAR" to "South African Rand",
    "ZMW" to "Zambian Kwacha",
    "ZWL" to "Zimbabwean Dollar",
)