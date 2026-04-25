package com.prathik.fairshare.data.model.response

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Typed response for GET /api/balances/summary.
 *
 * Previously declared as Map<String, Double> which caused JsonDecodingException
 * because the backend includes currency:"USD" (a String) in the same map.
 */
@Serializable
data class BalanceSummaryResponse(
    @SerialName("owedToMe")   val owedToMe:   Double,
    @SerialName("iOwe")       val iOwe:       Double,
    @SerialName("netBalance") val netBalance: Double,
    @SerialName("currency")   val currency:   String,
)