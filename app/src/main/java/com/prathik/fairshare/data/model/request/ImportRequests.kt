package com.prathik.fairshare.data.model.request

import kotlinx.serialization.Serializable

/**
 * Request body for POST /api/import/group or POST /api/import/friend
 *
 * type      — "GROUP" creates a new group + placeholders + all expense data
 *           — "FRIEND" creates non-group expenses + placeholders (no group)
 * groupName — required when type = "GROUP"
 * csvContent — raw CSV file content exported from Splitwise
 */
@Serializable
data class ImportRequest(
    val type: String,
    val groupName: String? = null,
    val csvContent: String,
)

/**
 * Request body for POST /api/import/groups/{groupId}/assign
 * Assigns a PLACEHOLDER user to a real existing user (friend).
 * Used when the group creator knows which friend the placeholder represents.
 *
 * placeholderUserId — the PLACEHOLDER user's ID from the import
 * friendUserId      — the real user's ID to assign the placeholder to
 */
@Serializable
data class AssignPlaceholderRequest(
    val placeholderUserId: String,
    val friendUserId: String,
)

/**
 * Request body for POST /api/import/groups/{groupId}/claim
 * Used when the current user recognises themselves as a placeholder.
 *
 * placeholderUserId — the PLACEHOLDER user's ID that the current user claims
 */
@Serializable
data class ClaimIdentityRequest(
    val placeholderUserId: String,
)

/**
 * Request body for POST /api/import/groups/{groupId}/unclaim
 * Reverses a wrong claim — used when the wrong person claimed a placeholder.
 *
 * wrongClaimerUserId — the user who wrongly claimed the placeholder
 * originalCsvName    — the original name from the CSV to identify the placeholder
 */
@Serializable
data class UnclaimIdentityRequest(
    val wrongClaimerUserId: String,
    val originalCsvName: String,
)
