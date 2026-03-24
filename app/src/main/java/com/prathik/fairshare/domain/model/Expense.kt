package com.prathik.fairshare.domain.model

enum class SplitType {
    EQUAL, UNEQUAL, PERCENTAGE, SHARES
}

enum class ExpenseCategory {
    // Entertainment
    GAMES, MOVIES, MUSIC, SPORTS,
    // Food
    DINING_OUT, GROCERIES, LIQUOR,
    // Home
    ELECTRONICS, FURNITURE, HOUSEHOLD_SUPPLIES, MAINTENANCE,
    MORTGAGE, PETS, RENT, SERVICES,
    // Life
    CHILDCARE, CLOTHING, EDUCATION, GIFTS, INSURANCE, MEDICAL, TAXES,
    // Transportation
    BICYCLE, BUS_TRAIN, CAR, GAS_FUEL, HOTEL, PARKING, PLANE, TAXI,
    // Utilities
    CLEANING, ELECTRICITY, HEAT_GAS, TRASH, TV_PHONE_INTERNET, WATER,
    // Misc
    GENERAL, OTHER
}

data class Expense(
    val id: String,
    val description: String,
    val totalAmount: Double,
    val currency: String,
    val groupId: String?,
    val groupName: String?,
    val addedById: String,
    val addedByName: String,
    val splitType: SplitType,
    val category: ExpenseCategory?,
    val notes: String?,
    val expenseDate: String,
    val isDeleted: Boolean,
    val payers: List<PayerDetail>,
    val splits: List<SplitDetail>,
    val commentCount: Int,
    val itemCount: Int,
    val yourPaid: Double,
    val yourShare: Double,
    val yourBalance: Double,
    val createdAt: String,
    val updatedAt: String,
) {
    data class PayerDetail(
        val userId: String,
        val fullName: String,
        val amountPaid: Double,
    )

    data class SplitDetail(
        val userId: String,
        val fullName: String,
        val amountOwed: Double,
        val percentage: Double?,
        val shares: Int?,
        val isSettled: Boolean,
    )
}