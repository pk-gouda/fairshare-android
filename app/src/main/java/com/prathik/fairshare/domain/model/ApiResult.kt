package com.prathik.fairshare.domain.model

/**
 * Replaces kotlin.Result throughout the app.
 *
 * Each failure case is distinct so the UI can show the right message
 * and take the right action for each case — no parsing exception messages.
 *
 * Success        — request succeeded, data is available
 * ValidationError — 400 — field-level errors to show inline on forms
 * Unauthorized   — 401 — session expired, navigate to Login
 * Forbidden      — 403 — no permission
 * NotFound       — 404 — resource doesn't exist
 * Conflict       — 409 — business rule violated
 * HttpError      — any other HTTP error
 * NetworkError   — no internet / timeout / connection refused
 */
sealed class ApiResult<out T> {

    data class Success<T>(val data: T) : ApiResult<T>()

    data class ValidationError(
        val message: String,
        val errors: List<FieldError> = emptyList(),
    ) : ApiResult<Nothing>()

    data class Unauthorized(
        val message: String = "Session expired. Please sign in again.",
    ) : ApiResult<Nothing>()

    data class Forbidden(
        val message: String = "You don't have permission to do this.",
    ) : ApiResult<Nothing>()

    data class NotFound(
        val message: String,
    ) : ApiResult<Nothing>()

    data class Conflict(
        val message: String,
    ) : ApiResult<Nothing>()

    data class HttpError(
        val code: Int,
        val message: String,
    ) : ApiResult<Nothing>()

    data class NetworkError(
        val exception: Throwable,
        val message: String = "No internet connection. Please check your network.",
    ) : ApiResult<Nothing>()
}

// ── Convenience extensions ────────────────────────────────────────────────────

val ApiResult<*>.isSuccess: Boolean get() = this is ApiResult.Success

fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.data

fun <T> ApiResult<T>.errorMessage(): String? = when (this) {
    is ApiResult.Success -> null
    is ApiResult.ValidationError -> message
    is ApiResult.Unauthorized -> message
    is ApiResult.Forbidden -> message
    is ApiResult.NotFound -> message
    is ApiResult.Conflict -> message
    is ApiResult.HttpError -> message
    is ApiResult.NetworkError -> message
}