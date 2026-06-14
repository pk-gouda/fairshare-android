package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.model.request.AssignPlaceholderRequest
import com.prathik.fairshare.data.model.request.ClaimIdentityRequest
import com.prathik.fairshare.data.model.request.ImportRequest
import com.prathik.fairshare.data.model.request.UnclaimIdentityRequest
import com.prathik.fairshare.data.model.response.ImportResponse
import com.prathik.fairshare.data.network.api.ImportApiService
import com.prathik.fairshare.data.network.mapSuccess
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.GroupMember
import com.prathik.fairshare.domain.repository.ImportRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportRepositoryImpl @Inject constructor(
    private val importService: ImportApiService,
) : ImportRepository {

    override suspend fun importGroup(
        csvContent: String,
        groupName: String,
        groupType: String,
        importerCsvName: String?,
    ): ApiResult<Map<String, Any>> =
        safeApiCall {
            importService.importGroup(
                ImportRequest(
                    type            = "GROUP",
                    groupName       = groupName,
                    groupType       = groupType,
                    csvContent      = csvContent,
                    importerCsvName = importerCsvName,
                )
            )
        }.mapSuccess { it?.toMap() ?: emptyMap() }

    override suspend fun importFriend(csvContent: String, importerCsvName: String?): ApiResult<Map<String, Any>> =
        safeApiCall {
            importService.importFriend(
                ImportRequest(type = "FRIEND", csvContent = csvContent, importerCsvName = importerCsvName)
            )
        }.mapSuccess { it?.toMap() ?: emptyMap() }

    override suspend fun getUnclaimedMembers(groupId: String): ApiResult<List<GroupMember>> =
        safeApiCall { importService.getUnclaimedMembers(groupId) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun assignPlaceholder(
        groupId: String,
        placeholderUserId: String,
        friendUserId: String,
    ): ApiResult<Map<String, Any>> =
        safeApiCall {
            importService.assignPlaceholder(
                groupId,
                AssignPlaceholderRequest(
                    placeholderUserId = placeholderUserId,
                    friendUserId      = friendUserId,
                )
            )
        }.mapSuccess { emptyMap() }

    override suspend fun claimIdentity(
        groupId: String,
        placeholderUserId: String,
    ): ApiResult<Map<String, Any>> =
        safeApiCall {
            importService.claimIdentity(
                groupId,
                ClaimIdentityRequest(placeholderUserId = placeholderUserId)
            )
        }.mapSuccess { emptyMap() }

    override suspend fun unclaimIdentity(
        groupId: String,
        wrongClaimerUserId: String,
        originalCsvName: String,
    ): ApiResult<Map<String, Any>> {
        // Dormant method — see ImportRepository.unclaimIdentity KDoc.
        // These guards make a wrong wiring fail loudly at the call site rather
        // than silently sending a payload the backend rejects.
        require(wrongClaimerUserId.isNotBlank()) {
            "wrongClaimerUserId must be a real active claimer ID, not blank"
        }
        require(originalCsvName.isNotBlank()) {
            "originalCsvName must be the original CSV name to restore, not blank"
        }
        return safeApiCall {
            importService.unclaimIdentity(
                groupId,
                UnclaimIdentityRequest(
                    wrongClaimerUserId = wrongClaimerUserId,
                    originalCsvName    = originalCsvName,
                )
            )
        }.mapSuccess { emptyMap() }
    }

    override suspend fun assignFriendPlaceholder(
        placeholderUserId: String,
        friendUserId: String,
    ): ApiResult<Map<String, Any>> =
        safeApiCall {
            importService.assignFriendPlaceholder(
                AssignPlaceholderRequest(
                    placeholderUserId = placeholderUserId,
                    friendUserId      = friendUserId,
                )
            )
        }.mapSuccess { emptyMap() }
}

// Extension to convert ImportResponse → Map<String, Any> (keeps domain layer unchanged)
private fun ImportResponse.toMap(): Map<String, Any> = buildMap {
    put("type",               type ?: "")
    put("groupId",            groupId ?: "")
    put("groupName",          groupName ?: "")
    put("inviteCode",         inviteCode ?: "")
    put("expensesCreated",    expensesCreated)
    put("settlementsCreated", settlementsCreated)
    put("rowsSkipped",        rowsSkipped)
    put("totalRows",          totalRows)
    put("members", members.map { entry ->
        mapOf(
            "csvName"           to entry.csvName,
            "placeholderUserId" to entry.placeholderUserId,
            "status"            to entry.status,
        )
    })
    put("removedNames",       removedNames)
    put("warnings",           warnings)
}