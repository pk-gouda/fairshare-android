package com.prathik.fairshare.data.network.api

import com.prathik.fairshare.data.model.response.ApiResponse
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit interface for analytics endpoints.
 * 11 endpoints across group, personal, and friend analytics.
 *
 * Note: Analytics responses return dynamic shapes (Map<String, Any>)
 * depending on the endpoint. We use Map<String, Any> and parse
 * in the repository implementation.
 */
interface AnalyticsApiService {

    // Group analytics (7 endpoints)
    @GET("api/groups/{groupId}/analytics/category")
    suspend fun getGroupCategoryBreakdown(
        @Path("groupId") groupId: String,
    ): ApiResponse<Map<String, Any>>

    @GET("api/groups/{groupId}/analytics/member")
    suspend fun getGroupMemberBreakdown(
        @Path("groupId") groupId: String,
    ): ApiResponse<Map<String, Any>>

    @GET("api/groups/{groupId}/analytics/currency")
    suspend fun getGroupCurrencyBreakdown(
        @Path("groupId") groupId: String,
    ): ApiResponse<Map<String, Any>>

    @GET("api/groups/{groupId}/analytics/summary")
    suspend fun getGroupSummary(
        @Path("groupId") groupId: String,
    ): ApiResponse<Map<String, Any>>

    @GET("api/groups/{groupId}/analytics/chart")
    suspend fun getGroupChartData(
        @Path("groupId") groupId: String,
    ): ApiResponse<Map<String, Any>>

    @GET("api/groups/{groupId}/analytics/my-share")
    suspend fun getMyShareInGroup(
        @Path("groupId") groupId: String,
    ): ApiResponse<Map<String, Any>>

    @GET("api/groups/{groupId}/analytics/my-breakdown")
    suspend fun getMyBreakdownInGroup(
        @Path("groupId") groupId: String,
    ): ApiResponse<Map<String, Any>>

    // Personal analytics (3 endpoints)
    @GET("api/analytics/me/summary")
    suspend fun getPersonalSummary(): ApiResponse<Map<String, Any>>

    @GET("api/analytics/me/monthly")
    suspend fun getPersonalMonthly(): ApiResponse<Map<String, Any>>

    @GET("api/analytics/me/stats")
    suspend fun getPersonalStats(): ApiResponse<Map<String, Any>>

    // Friend analytics (1 endpoint)
    @GET("api/analytics/friend/{otherUserId}/trends")
    suspend fun getFriendTrends(
        @Path("otherUserId") otherUserId: String,
    ): ApiResponse<Map<String, Any>>
}
