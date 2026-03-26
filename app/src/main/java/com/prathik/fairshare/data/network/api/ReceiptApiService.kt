package com.prathik.fairshare.data.network.api

import com.prathik.fairshare.data.model.request.ScanReceiptRequest
import com.prathik.fairshare.data.model.response.ApiResponse
import com.prathik.fairshare.data.model.response.ReceiptResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/**
 * Retrofit interface for /api/receipts/** endpoints.
 * 2 endpoints for receipt scanning and retrieval.
 */
interface ReceiptApiService {

    @POST("api/receipts/scan")
    suspend fun scanReceipt(@Body request: ScanReceiptRequest): ApiResponse<ReceiptResponse>

    @GET("api/receipts/{receiptId}")
    suspend fun getReceipt(@Path("receiptId") receiptId: String): ApiResponse<ReceiptResponse>
}
