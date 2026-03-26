package com.prathik.fairshare.data.network.api

import com.prathik.fairshare.data.model.request.AddCommentRequest
import com.prathik.fairshare.data.model.request.CreateExpenseRequest
import com.prathik.fairshare.data.model.request.ItemAssignmentRequest
import com.prathik.fairshare.data.model.request.UpdateExpenseRequest
import com.prathik.fairshare.data.model.response.ApiResponse
import com.prathik.fairshare.data.model.response.ExpenseCommentResponse
import com.prathik.fairshare.data.model.response.ExpenseItemResponse
import com.prathik.fairshare.data.model.response.ExpenseResponse
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ExpenseApiService {

    @POST("api/expenses")
    suspend fun createExpense(@Body request: CreateExpenseRequest): ApiResponse<ExpenseResponse>

    @GET("api/expenses/{expenseId}")
    suspend fun getExpense(@Path("expenseId") expenseId: String): ApiResponse<ExpenseResponse>

    @PUT("api/expenses/{expenseId}")
    suspend fun updateExpense(
        @Path("expenseId") expenseId: String,
        @Body request: UpdateExpenseRequest,
    ): ApiResponse<ExpenseResponse>

    @DELETE("api/expenses/{expenseId}")
    suspend fun deleteExpense(@Path("expenseId") expenseId: String): ApiResponse<Unit>

    @POST("api/expenses/{expenseId}/restore")
    suspend fun restoreExpense(@Path("expenseId") expenseId: String): ApiResponse<ExpenseResponse>

    @GET("api/groups/{groupId}/expenses")
    suspend fun getGroupExpenses(@Path("groupId") groupId: String): ApiResponse<List<ExpenseResponse>>

    @GET("api/expenses/{expenseId}/items")
    suspend fun getExpenseItems(@Path("expenseId") expenseId: String): ApiResponse<List<ExpenseItemResponse>>

    @PUT("api/expenses/{expenseId}/items/assign")
    suspend fun assignItems(
        @Path("expenseId") expenseId: String,
        @Body request: ItemAssignmentRequest,
    ): ApiResponse<List<ExpenseItemResponse>>

    @GET("api/expenses/search")
    suspend fun searchExpenses(@Query("q") query: String): ApiResponse<List<ExpenseResponse>>

    @GET("api/groups/{groupId}/recurring-expenses")
    suspend fun getRecurringExpenses(@Path("groupId") groupId: String): ApiResponse<List<ExpenseResponse>>

    @POST("api/expenses/{expenseId}/stop-recurring")
    suspend fun stopRecurring(@Path("expenseId") expenseId: String): ApiResponse<Unit>

    @POST("api/expenses/{expenseId}/comments")
    suspend fun addComment(
        @Path("expenseId") expenseId: String,
        @Body request: AddCommentRequest,
    ): ApiResponse<ExpenseCommentResponse>

    @GET("api/expenses/{expenseId}/comments")
    suspend fun getComments(@Path("expenseId") expenseId: String): ApiResponse<List<ExpenseCommentResponse>>

    @DELETE("api/comments/{commentId}")
    suspend fun deleteComment(@Path("commentId") commentId: String): ApiResponse<Unit>
}
