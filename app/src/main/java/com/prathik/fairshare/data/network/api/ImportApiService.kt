package com.prathik.fairshare.data.network.api

import com.prathik.fairshare.data.model.request.AssignPlaceholderRequest
import com.prathik.fairshare.data.model.request.ClaimIdentityRequest
import com.prathik.fairshare.data.model.request.ImportRequest
import com.prathik.fairshare.data.model.request.UnclaimIdentityRequest
import com.prathik.fairshare.data.model.response.ApiResponse
import com.prathik.fairshare.data.model.response.GroupMemberResponse
import com.prathik.fairshare.data.model.response.ImportActionResponse
import com.prathik.fairshare.data.model.response.ImportResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface ImportApiService {

    @POST("api/import/group")
    suspend fun importGroup(@Body request: ImportRequest): ApiResponse<ImportResponse>

    @POST("api/import/friend")
    suspend fun importFriend(@Body request: ImportRequest): ApiResponse<ImportResponse>

    @GET("api/import/groups/{groupId}/unclaimed")
    suspend fun getUnclaimedMembers(@Path("groupId") groupId: String): ApiResponse<List<GroupMemberResponse>>

    @GET("api/import/groups/{groupId}/preview/{placeholderUserId}")
    suspend fun previewPlaceholder(
        @Path("groupId") groupId: String,
        @Path("placeholderUserId") placeholderUserId: String,
    ): ApiResponse<GroupMemberResponse>

    @POST("api/import/groups/{groupId}/claim")
    suspend fun claimIdentity(
        @Path("groupId") groupId: String,
        @Body request: ClaimIdentityRequest,
    ): ApiResponse<ImportActionResponse>

    @POST("api/import/groups/{groupId}/assign")
    suspend fun assignPlaceholder(
        @Path("groupId") groupId: String,
        @Body request: AssignPlaceholderRequest,
    ): ApiResponse<ImportActionResponse>

    @POST("api/import/groups/{groupId}/unclaim")
    suspend fun unclaimIdentity(
        @Path("groupId") groupId: String,
        @Body request: UnclaimIdentityRequest,
    ): ApiResponse<ImportActionResponse>

    @POST("api/import/friend/assign")
    suspend fun assignFriendPlaceholder(
        @Body request: AssignPlaceholderRequest,
    ): ApiResponse<ImportActionResponse>
}