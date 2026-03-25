package com.prathik.fairshare.data.model.response

import kotlinx.serialization.Serializable

/**
 * Universal wrapper for every API response from the backend.
 * Every endpoint returns this structure — success or error.
 *
 * @param success   true if the request succeeded
 * @param message   optional human-readable message (shown on error)
 * @param data      the actual response payload — null on error
 * @param errors    field-level validation errors — null on success
 * @param timestamp server timestamp of the response
 */
@Serializable
data class ApiResponse<T>(
    val success: Boolean,
    val message: String? = null,
    val data: T? = null,
    val errors: List<FieldError>? = null,
    val timestamp: String? = null,
) {
    /**
     * Represents a field-level validation error returned by the backend.
     * Maps directly to GlobalExceptionHandler's FieldError record.
     *
     * @param field         the field that failed validation (e.g. "email")
     * @param message       why it failed (e.g. "already exists")
     * @param rejectedValue the value that was rejected
     */
    @Serializable
    data class FieldError(
        val field: String,
        val message: String,
        val rejectedValue: String? = null,
    )
}