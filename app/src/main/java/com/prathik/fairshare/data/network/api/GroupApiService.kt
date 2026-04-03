package com.prathik.fairshare.data.network.api

import com.prathik.fairshare.data.model.request.AddMemberRequest
import com.prathik.fairshare.data.model.request.CreateGroupRequest
import com.prathik.fairshare.data.model.request.JoinGroupRequest
import com.prathik.fairshare.data.model.request.UpdateGroupRequest
import com.prathik.fairshare.data.model.response.ApiResponse
import com.prathik.fairshare.data.model.response.BalanceResponse
import com.prathik.fairshare.data.model.response.GroupMemberResponse
import com.prathik.fairshare.data.model.response.GroupResponse
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface GroupApiService {

    @POST("api/groups")
    suspend fun createGroup(@Body request: CreateGroupRequest): ApiResponse<GroupResponse>

    @GET("api/groups")
    suspend fun getMyGroups(): ApiResponse<List<GroupResponse>>

    @GET("api/groups/{groupId}")
    suspend fun getGroup(@Path("groupId") groupId: String): ApiResponse<GroupResponse>

    @PUT("api/groups/{groupId}")
    suspend fun updateGroup(
        @Path("groupId") groupId: String,
        @Body request: UpdateGroupRequest,
    ): ApiResponse<GroupResponse>

    @DELETE("api/groups/{groupId}")
    suspend fun deleteGroup(@Path("groupId") groupId: String): ApiResponse<Unit>

    @POST("api/groups/{groupId}/members")
    suspend fun addMember(
        @Path("groupId") groupId: String,
        @Body request: AddMemberRequest,
    ): ApiResponse<GroupMemberResponse>

    @DELETE("api/groups/{groupId}/members/{memberId}")
    suspend fun removeMember(
        @Path("groupId") groupId: String,
        @Path("memberId") memberId: String,
    ): ApiResponse<Unit>

    @GET("api/groups/{groupId}/members")
    suspend fun getMembers(@Path("groupId") groupId: String): ApiResponse<List<GroupMemberResponse>>

    @POST("api/groups/join")
    suspend fun joinGroup(@Body request: JoinGroupRequest): ApiResponse<GroupMemberResponse>

    @POST("api/groups/{groupId}/archive")
    suspend fun archiveGroup(@Path("groupId") groupId: String): ApiResponse<Unit>

    @POST("api/groups/{groupId}/leave")
    suspend fun leaveGroup(@Path("groupId") groupId: String): ApiResponse<Unit>

    @POST("api/groups/{groupId}/unarchive")
    suspend fun unarchiveGroup(@Path("groupId") groupId: String): ApiResponse<Unit>

    @GET("api/groups/{groupId}/balances")
    suspend fun getGroupBalances(@Path("groupId") groupId: String): ApiResponse<List<BalanceResponse>>

    @GET("api/groups/{groupId}/export")
    suspend fun exportGroupCsv(@Path("groupId") groupId: String): ResponseBody
}