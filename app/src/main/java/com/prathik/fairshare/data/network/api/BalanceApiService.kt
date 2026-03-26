package com.prathik.fairshare.data.network.api

import com.prathik.fairshare.data.model.response.ApiResponse
import com.prathik.fairshare.data.model.response.BalanceResponse
import retrofit2.http.GET
import retrofit2.http.Path

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
