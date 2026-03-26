package com.prathik.fairshare.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Represents a field-level validation error from the backend.
 * Used in ApiResult.ValidationError to show inline errors on form fields.
 *
 * Defined in domain layer so the UI layer can reference it without
 * importing anything from the data layer.
 *
 * @param field         the field that failed (e.g. "email", "password")
 * @param message       why it failed (e.g. "Email already registered")
 * @param rejectedValue the value that was rejected (optional)
 */
@Parcelize
data class FieldError(
    val field: String,
    val message: String,
    val rejectedValue: String? = null,
) : Parcelable