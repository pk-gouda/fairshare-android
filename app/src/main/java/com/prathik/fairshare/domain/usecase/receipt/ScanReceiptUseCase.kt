package com.prathik.fairshare.domain.usecase.receipt

import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Receipt
import com.prathik.fairshare.domain.repository.ReceiptRepository
import javax.inject.Inject

/**
 * Scans a receipt image and returns extracted data.
 * Used in ReceiptScanScreen — result pre-fills AddExpenseScreen.
 */
class ScanReceiptUseCase @Inject constructor(
    private val receiptRepository: ReceiptRepository,
) {
    suspend operator fun invoke(
        imageBase64      : String,
        mimeType         : String,
        preferredCurrency: String,
    ): ApiResult<Receipt> {
        return receiptRepository.scanReceipt(
            imageBase64       = imageBase64,
            mimeType          = mimeType,
            preferredCurrency = preferredCurrency,
        )
    }
}