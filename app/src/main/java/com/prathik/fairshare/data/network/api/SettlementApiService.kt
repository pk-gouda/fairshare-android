package com.prathik.fairshare.data.network.api

import com.prathik.fairshare.data.model.request.SettleRequest
import com.prathik.fairshare.data.model.response.ApiResponse
import com.prathik.fairshare.data.model.response.BalanceResponse
import com.prathik.fairshare.data.model.response.SettlementResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface SettlementApiService {

    @GET("api/settlements/breakdown/{otherUserId}")
    suspend fun getBreakdown(@Path("otherUserId") otherUserId: String): ApiResponse<List<BalanceResponse>>

    @POST("api/settlements")
    suspend fun settle(@Body request: SettleRequest): ApiResponse<List<SettlementResponse>>

    @POST("api/settlements/{settlementId}/confirm")
    suspend fun confirmSettlement(@Path("settlementId") settlementId: String): ApiResponse<SettlementResponse>

    @POST("api/settlements/{settlementId}/cancel")
    suspend fun cancelSettlement(@Path("settlementId") settlementId: String): ApiResponse<SettlementResponse>

    @GET("api/settlements/history/{otherUserId}")
    suspend fun getHistory(@Path("otherUserId") otherUserId: String): ApiResponse<List<SettlementResponse>>

    @GET("api/settlements/pending")
    suspend fun getPending(): ApiResponse<List<SettlementResponse>>

    @GET("api/settlements/initiated")
    suspend fun getInitiated(): ApiResponse<List<SettlementResponse>>
}
