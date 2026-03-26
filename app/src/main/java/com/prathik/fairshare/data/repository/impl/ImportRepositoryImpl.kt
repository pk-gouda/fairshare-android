package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.model.request.AssignPlaceholderRequest
import com.prathik.fairshare.data.model.request.ClaimIdentityRequest
import com.prathik.fairshare.data.model.request.ImportRequest
import com.prathik.fairshare.data.model.request.UnclaimIdentityRequest
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
    ): ApiResult<Map<String, Any>> =
        safeApiCall {
            importService.importGroup(
                ImportRequest(type = "GROUP", groupName = groupName, csvContent = csvContent)
            )
        }.mapSuccess { it ?: emptyMap() }

    override suspend fun importFriend(csvContent: String): ApiResult<Map<String, Any>> =
        safeApiCall {
            importService.importFriend(
                ImportRequest(type = "FRIEND", csvContent = csvContent)
            )
        }.mapSuccess { it ?: emptyMap() }

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
        }.mapSuccess { it ?: emptyMap() }

    override suspend fun claimIdentity(
        groupId: String,
        placeholderUserId: String,
    ): ApiResult<Map<String, Any>> =
        safeApiCall {
            importService.claimIdentity(
                groupId,
                ClaimIdentityRequest(placeholderUserId = placeholderUserId)
            )
        }.mapSuccess { it ?: emptyMap() }

    override suspend fun unclaimIdentity(
        groupId: String,
        placeholderUserId: String,
    ): ApiResult<Map<String, Any>> =
        safeApiCall {
            importService.unclaimIdentity(
                groupId,
                UnclaimIdentityRequest(
                    wrongClaimerUserId = placeholderUserId,
                    originalCsvName   = "",
                )
            )
        }.mapSuccess { it ?: emptyMap() }
}
