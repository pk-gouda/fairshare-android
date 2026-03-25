package com.prathik.fairshare.domain.model

import com.prathik.fairshare.data.model.response.ApiResponse

/**
 * Replaces kotlin.Result throughout the app.
 *
 * kotlin.Result can't distinguish between different failure types —
 * a network timeout, a 401, a validation error, and a 500 all look
 * the same. ApiResult gives the UI layer enough information to show
 * the right message and take the right action for each case.
 *
 * Success        — request succeeded, data is available
 * ValidationError — 400 Bad Request — one or more fields failed validation
 *                   errors contains field-level messages to show inline
 * Unauthorized   — 401 — token expired or invalid, navigate to Login
 * Forbidden      — 403 — user doesn't have permission
 * NotFound       — 404 — resource doesn't exist
 * Conflict       — 409 — business rule violated (e.g. email already exists)
 * HttpError      — any other HTTP error with status code + message
 * NetworkError   — no internet, timeout, or connection refused
 */
sealed class ApiResult<out T> {

    /**
     * Request succeeded. [data] contains the response payload.
     */
    data class Success<T>(val data: T) : ApiResult<T>()

    /**
     * 400 Bad Request — input validation failed.
     * [errors] contains field-level messages from the backend.
     * Use these to show inline errors on form fields.
     */
    data class ValidationError(
        val message: String,
        val errors: List<ApiResponse.FieldError> = emptyList(),
    ) : ApiResult<Nothing>()

    /**
     * 401 Unauthorized — session expired or token invalid.
     * The app should clear tokens and navigate to Login.
     */
    data class Unauthorized(
        val message: String = "Session expired. Please sign in again.",
    ) : ApiResult<Nothing>()

    /**
     * 403 Forbidden — authenticated but not permitted to perform this action.
     */
    data class Forbidden(
        val message: String = "You don't have permission to do this.",
    ) : ApiResult<Nothing>()

    /**
     * 404 Not Found — the requested resource doesn't exist.
     */
    data class NotFound(
        val message: String,
    ) : ApiResult<Nothing>()

    /**
     * 409 Conflict — a business rule was violated.
     * Examples: email already registered, group has unsettled balances.
     */
    data class Conflict(
        val message: String,
    ) : ApiResult<Nothing>()

    /**
     * Any other HTTP error not covered by the cases above.
     * [code] is the HTTP status code, [message] is from the backend.
     */
    data class HttpError(
        val code: Int,
        val message: String,
    ) : ApiResult<Nothing>()

    /**
     * No internet connection, request timed out, or server unreachable.
     * Show "Check your connection" and offer a retry.
     */
    data class NetworkError(
        val exception: Throwable,
        val message: String = "No internet connection. Please check your network.",
    ) : ApiResult<Nothing>()
}

// ── Convenience extensions ────────────────────────────────────────────────────

/**
 * Returns true if this result is a Success.
 */
val ApiResult<*>.isSuccess: Boolean get() = this is ApiResult.Success

/**
 * Returns the data if Success, null otherwise.
 */
fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.data

/**
 * Returns a user-facing error message for any failure state.
 * Returns null for Success.
 */
fun <T> ApiResult<T>.errorMessage(): String? = when (this) {
    is ApiResult.Success         -> null
    is ApiResult.ValidationError -> message
    is ApiResult.Unauthorized    -> message
    is ApiResult.Forbidden       -> message
    is ApiResult.NotFound        -> message
    is ApiResult.Conflict        -> message
    is ApiResult.HttpError       -> message
    is ApiResult.NetworkError    -> message
}