package com.prathik.fairshare.data.repository.impl

import com.prathik.fairshare.data.local.ExpenseDao
import com.prathik.fairshare.data.local.ExpenseEntity
import com.prathik.fairshare.data.model.mapper.toDomain
import com.prathik.fairshare.data.model.request.AddCommentRequest
import com.prathik.fairshare.data.model.request.CreateExpenseRequest
import com.prathik.fairshare.data.model.request.ItemAssignmentRequest
import com.prathik.fairshare.data.model.request.UpdateExpenseRequest
import com.prathik.fairshare.data.network.api.ExpenseApiService
import com.prathik.fairshare.data.network.mapSuccess
import com.prathik.fairshare.data.network.safeApiCall
import com.prathik.fairshare.domain.model.ApiResult
import com.prathik.fairshare.domain.model.Expense
import com.prathik.fairshare.domain.model.ExpenseCategory
import com.prathik.fairshare.domain.model.ExpenseComment
import com.prathik.fairshare.domain.model.ExpenseItem
import com.prathik.fairshare.domain.model.SplitType
import com.prathik.fairshare.domain.repository.ExpenseRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpenseRepositoryImpl @Inject constructor(
    private val expenseService: ExpenseApiService,
    private val expenseDao: ExpenseDao,
) : ExpenseRepository {

    override suspend fun getGroupExpenses(groupId: String): ApiResult<List<Expense>> {
        val result = safeApiCall { expenseService.getGroupExpenses(groupId) }
        if (result is ApiResult.Success) {
            val entities = result.data.map { it.toEntity() }
            expenseDao.deleteByGroupId(groupId)
            expenseDao.insertAll(entities)
        }
        return result.mapSuccess { list -> list.map { it.toDomain() } }
    }

    override suspend fun getExpense(expenseId: String): ApiResult<Expense> =
        safeApiCall { expenseService.getExpense(expenseId) }
            .mapSuccess { it.toDomain() }

    override suspend fun createExpense(
        groupId: String?,
        description: String,
        totalAmount: Double,
        currency: String,
        splitType: SplitType?,
        category: ExpenseCategory?,
        notes: String?,
        expenseDate: String?,
        payerData: Map<String, Double>?,
        splitData: Map<String, Double>?,
        receiptId: String?,
        remainderPointer: Int?,
    ): ApiResult<Expense> =
        safeApiCall {
            expenseService.createExpense(
                CreateExpenseRequest(
                    groupId     = groupId,
                    description = description,
                    totalAmount = totalAmount,
                    currency    = currency,
                    splitType   = splitType,
                    category    = category,
                    notes       = notes,
                    expenseDate = expenseDate,
                    payerData   = payerData,
                    splitData        = splitData,
                    receiptId        = receiptId,
                    remainderPointer = remainderPointer,
                )
            )
        }.mapSuccess { it.toDomain() }

    override suspend fun updateExpense(
        expenseId: String,
        description: String?,
        totalAmount: Double?,
        currency: String?,
        splitType: SplitType?,
        category: ExpenseCategory?,
        notes: String?,
        expenseDate: String?,
        payerData: Map<String, Double>?,
        splitData: Map<String, Double>?,
    ): ApiResult<Expense> =
        safeApiCall {
            expenseService.updateExpense(
                expenseId,
                UpdateExpenseRequest(
                    description = description,
                    totalAmount = totalAmount,
                    currency    = currency,
                    splitType   = splitType,
                    category    = category,
                    notes       = notes,
                    expenseDate = expenseDate,
                    payerData   = payerData,
                    splitData   = splitData,
                )
            )
        }.mapSuccess { it.toDomain() }

    override suspend fun deleteExpense(expenseId: String): ApiResult<Unit> {
        val result = safeApiCall { expenseService.deleteExpense(expenseId) }
        return when (result) {
            is ApiResult.Success -> {
                expenseDao.deleteById(expenseId)
                ApiResult.Success(Unit)
            }
            is ApiResult.NetworkError    -> result
            is ApiResult.HttpError       -> result
            is ApiResult.ValidationError -> result
            is ApiResult.Unauthorized    -> result
            is ApiResult.NotFound        -> result
            is ApiResult.Forbidden       -> result
            is ApiResult.Conflict        -> result
        }
    }

    override suspend fun restoreExpense(expenseId: String): ApiResult<Expense> =
        safeApiCall { expenseService.restoreExpense(expenseId) }.mapSuccess { it.toDomain() }

    override suspend fun searchExpenses(query: String): ApiResult<List<Expense>> =
        safeApiCall { expenseService.searchExpenses(query) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun getRecurringExpenses(groupId: String): ApiResult<List<Expense>> =
        safeApiCall { expenseService.getRecurringExpenses(groupId) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    override suspend fun stopRecurring(expenseId: String): ApiResult<Unit> =
        safeApiCall { expenseService.stopRecurring(expenseId) }.mapSuccess { }

    override suspend fun getDirectExpensesWithFriend(friendId: String): ApiResult<List<Expense>> =
        safeApiCall { expenseService.getDirectExpensesWithFriend(friendId) }
            .mapSuccess { list -> list.map { it.toDomain() } }

    private fun com.prathik.fairshare.data.model.response.ExpenseResponse.toEntity() =
        ExpenseEntity(
            id           = id,
            description  = description,
            totalAmount  = totalAmount,
            currency     = currency,
            groupId      = groupId,
            groupName    = groupName,
            addedById    = addedById,
            addedByName  = addedByName,
            splitType    = splitType,
            category     = category,
            notes        = notes,
            expenseDate  = expenseDate,
            isDeleted    = isDeleted,
            commentCount = commentCount,
            itemCount    = itemCount,
            yourPaid     = yourPaid,
            yourShare    = yourShare,
            yourBalance  = yourBalance,
            createdAt    = createdAt,
            updatedAt    = updatedAt,
        )
}