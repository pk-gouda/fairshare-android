package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.model.request.ScanReceiptRequest
import com.prathik.fairshare.data.network.api.ReceiptApiService
import com.prathik.fairshare.data.network.mapSuccess
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Receipt
import com.prathik.fairshare.domain.repository.ReceiptRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptRepositoryImpl @Inject constructor(
    private val receiptService: ReceiptApiService,
) : ReceiptRepository {

    override suspend fun scanReceipt(
        imageBase64: String,
        mimeType: String,
        preferredCurrency: String?,
        scanTraceId: String,
    ): ApiResult<Receipt> {
        return safeApiCall {
            receiptService.scanReceipt(
                request    = ScanReceiptRequest(
                    imageBase64       = imageBase64,
                    mimeType          = mimeType,
                    preferredCurrency = preferredCurrency,
                ),
                traceId    = scanTraceId.ifBlank { null },
            )
        }.mapSuccess { it.toDomain() }
    }

    override suspend fun getReceipt(receiptId: String): ApiResult<Receipt> =
        safeApiCall { receiptService.getReceipt(receiptId) }
            .mapSuccess { it.toDomain() }
}