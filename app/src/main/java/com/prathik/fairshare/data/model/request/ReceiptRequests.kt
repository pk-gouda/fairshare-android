package com.prathik.fairshare.data.model.request

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ScanReceiptRequest(
    @SerialName("imageBase64")       val imageBase64: String,
    @SerialName("mimeType")          val mimeType: String,
    @SerialName("preferredCurrency") val preferredCurrency: String? = null,
)
