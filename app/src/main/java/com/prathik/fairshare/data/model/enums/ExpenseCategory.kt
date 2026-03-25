package com.prathik.fairshare.data.model.enums

/**
 * Categories for classifying expenses.
 * Used for analytics breakdown and auto-detection from description keywords.
 */
enum class ExpenseCategory {
    // Entertainment
    GAMES, MOVIES, MUSIC, SPORTS,

    // Food and drink
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

    // Uncategorized
    GENERAL, OTHER
}