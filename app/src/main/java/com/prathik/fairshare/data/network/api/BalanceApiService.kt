package com.prathik.fairshare.data.network.api

import com.prathik.fairshare.data.model.response.ApiResponse
import com.prathik.fairshare.data.model.response.BalanceResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for /api/balances/** endpoints.
 * 3 endpoints for balance retrieval.
 *
 * Note: summary and breakdown endpoints return Map<String, Any> from backend.
 * We use JsonObject (kotlinx) to handle dynamic response shapes.
 */
interface BalanceApiService {

    @GET("api/balances")
    suspend fun getAllBalances(): ApiResponse<List<BalanceResponse>>

    @GET("api/balances/{otherUserId}")
    suspend fun getBalanceWithUser(
        @Path("otherUserId") otherUserId: String,
    ): ApiResponse<List<BalanceResponse>>

    @GET("api/balances/summary")
    suspend fun getBalanceSummary(): ApiResponse<Map<String, Double>>
}
