package com.prathik.fairshare.data.network.api

import com.prathik.fairshare.data.model.request.ScanReceiptRequest
import com.prathik.fairshare.data.model.response.ApiResponse
import com.prathik.fairshare.data.model.response.ExpenseItemResponse
import com.prathik.fairshare.data.model.response.ReceiptResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface ReceiptApiService {

    @POST("api/receipts/scan")
    suspend fun scanReceipt(
        @Body   request : ScanReceiptRequest,
        @Header("X-Scan-Trace-Id") traceId: String? = null,
        // TEMPORARY header — remove before GA release
    ): ApiResponse<ReceiptResponse>

    @GET("api/receipts/{receiptId}")
    suspend fun getReceipt(@Path("receiptId") receiptId: String): ApiResponse<ReceiptResponse>

    @GET("api/receipts/{receiptId}/items")
    suspend fun getReceiptItems(@Path("receiptId") receiptId: String): ApiResponse<List<ExpenseItemResponse>>
}