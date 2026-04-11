package com.prathik.fairshare.data.network.api

import com.prathik.fairshare.data.model.response.ApiResponse
import kotlinx.serialization.json.JsonElement
import retrofit2.http.GET
import retrofit2.http.Path

interface AnalyticsApiService {

    @GET("api/groups/{groupId}/analytics/category")
    suspend fun getGroupCategoryBreakdown(@Path("groupId") groupId: String): ApiResponse<JsonElement>

    @GET("api/groups/{groupId}/analytics/member")
    suspend fun getGroupMemberBreakdown(@Path("groupId") groupId: String): ApiResponse<JsonElement>

    @GET("api/groups/{groupId}/analytics/currency")
    suspend fun getGroupCurrencyBreakdown(@Path("groupId") groupId: String): ApiResponse<JsonElement>

    @GET("api/groups/{groupId}/analytics/summary")
    suspend fun getGroupSummary(@Path("groupId") groupId: String): ApiResponse<JsonElement>

    @GET("api/groups/{groupId}/analytics/chart")
    suspend fun getGroupChartData(@Path("groupId") groupId: String): ApiResponse<JsonElement>

    @GET("api/groups/{groupId}/analytics/my-share")
    suspend fun getMyShareInGroup(@Path("groupId") groupId: String): ApiResponse<JsonElement>

    @GET("api/groups/{groupId}/analytics/my-breakdown")
    suspend fun getMyBreakdownInGroup(@Path("groupId") groupId: String): ApiResponse<JsonElement>

    @GET("api/analytics/me/summary")
    suspend fun getPersonalSummary(): ApiResponse<JsonElement>

    @GET("api/analytics/me/monthly")
    suspend fun getPersonalMonthly(): ApiResponse<JsonElement>

    @GET("api/analytics/me/stats")
    suspend fun getPersonalStats(): ApiResponse<JsonElement>

    @GET("api/analytics/friend/{otherUserId}/trends")
    suspend fun getFriendTrends(@Path("otherUserId") otherUserId: String): ApiResponse<JsonElement>
}