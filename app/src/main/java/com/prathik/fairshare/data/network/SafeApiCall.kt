package com.prathik.fairshare.data.network

import com.prathik.fairshare.data.model.response.ApiResponse
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.FieldError
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/**
 * Wraps every repository network call in a unified error handler.
 *
 * Every repository method looks like this:
 *
 *     suspend fun login(email: String, password: String): ApiResult<User> =
 *         safeApiCall { authService.login(LoginRequest(email, password)) }
 *             .mapSuccess { it.toDomain() }
 *
 * Error mapping:
 * - IOException          → NetworkError (no internet / timeout)
 * - 400                  → ValidationError (field-level errors from backend)
 * - 401                  → Unauthorized (token expired)
 * - 403                  → Forbidden (no permission)
 * - 404                  → NotFound
 * - 409                  → Conflict (business rule violated)
 * - other HTTP errors    → HttpError
 * - unexpected exception → NetworkError
 */
suspend fun <T> safeApiCall(
    call: suspend () -> ApiResponse<T>,
): ApiResult<T> {
    return try {
        val response = call()
        if (response.success) {
            @Suppress("UNCHECKED_CAST")
            // response.data is null for Unit responses (e.g. markAllRead, logout)
            // In those cases, cast null to T safely using Unit as fallback
            val data = response.data ?: Unit
            ApiResult.Success(data as T)
        } else {
            ApiResult.HttpError(
                code = 0,
                message = response.message ?: "Unknown error",
            )
        }
    } catch (e: IOException) {
        ApiResult.NetworkError(
            exception = e,
            message = "No internet connection. Please check your network.",
        )
    } catch (e: HttpException) {
        parseHttpException(e)
    } catch (e: Exception) {
        ApiResult.NetworkError(
            exception = e,
            message = e.message ?: "An unexpected error occurred.",
        )
    }
}

/**
 * Parses HttpException into the correct ApiResult failure type.
 *
 * IMPORTANT: errorBody().string() consumes the stream on first call.
 * We read it ONCE, then extract both message and errors from the same string.
 */
private fun parseHttpException(e: HttpException): ApiResult<Nothing> {
    val code = e.code()

    // Read the error body exactly once
    val errorBodyString = try {
        e.response()?.errorBody()?.string()
    } catch (ex: Exception) {
        null
    }

    // Parse the error body into ApiResponse — extract message + errors together
    val (message, errors) = if (!errorBodyString.isNullOrBlank()) {
        try {
            val apiResponse = Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            }.decodeFromString<ApiResponse<Unit>>(errorBodyString)

            val parsedMessage = apiResponse.message ?: defaultMessage(code)
            val parsedErrors = apiResponse.errors?.map { fieldError ->
                FieldError(
                    field = fieldError.field,
                    message = fieldError.message,
                    rejectedValue = fieldError.rejectedValue,
                )
            } ?: emptyList()

            Pair(parsedMessage, parsedErrors)
        } catch (parseEx: Exception) {
            Pair(defaultMessage(code), emptyList())
        }
    } else {
        Pair(defaultMessage(code), emptyList())
    }

    return when (code) {
        400 -> ApiResult.ValidationError(message = message, errors = errors)
        401 -> ApiResult.Unauthorized(message = message)
        403 -> ApiResult.Forbidden(message = message)
        404 -> ApiResult.NotFound(message = message)
        409 -> ApiResult.Conflict(message = message)
        else -> ApiResult.HttpError(code = code, message = message)
    }
}

/**
 * Default human-readable messages for common HTTP status codes.
 */
private fun defaultMessage(code: Int): String = when (code) {
    400 -> "Invalid request. Please check your input."
    401 -> "Session expired. Please sign in again."
    403 -> "You don't have permission to do this."
    404 -> "The requested resource was not found."
    409 -> "This action conflicts with existing data."
    500 -> "Server error. Please try again later."
    502 -> "Server is unavailable. Please try again later."
    503 -> "Service is temporarily unavailable."
    else -> "Something went wrong. Please try again."
}

/**
 * Maps a successful ApiResult<T> to ApiResult<R> using the given transform.
 * Passes through all failure types unchanged.
 */
fun <T, R> ApiResult<T>.mapSuccess(transform: (T) -> R): ApiResult<R> {
    return when (this) {
        is ApiResult.Success -> ApiResult.Success(transform(data))
        is ApiResult.ValidationError -> this
        is ApiResult.Unauthorized -> this
        is ApiResult.Forbidden -> this
        is ApiResult.NotFound -> this
        is ApiResult.Conflict -> this
        is ApiResult.HttpError -> this
        is ApiResult.NetworkError -> this
    }
}