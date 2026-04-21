package com.prathik.fairshare.domain.model

data class ExpenseChangeLog(
    val changedById: String,
    val changedByName: String,
    val changedAt: String,
    val changes: List<FieldChange>,
) {
    data class FieldChange(
        val fieldName: String,
        val oldValue: String?,
        val newValue: String?,
    )
}