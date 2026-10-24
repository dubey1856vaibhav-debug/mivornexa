package com.example.bizflow.model

import java.util.UUID

data class LineItem(
    val id: String = UUID.randomUUID().toString(),
    val description: String,
    val quantity: Double = 1.0,
    val unitPrice: Double = 0.0,
    val taxPercent: Double = 0.0,
    val discountPercent: Double = 0.0
) {
    val totalAmount: Double
        get() {
            val base = quantity * unitPrice
            val discounted = base * (1.0 - discountPercent / 100.0)
            return discounted * (1.0 + taxPercent / 100.0)
        }
}

data class WorkRecord(
    val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val recordNumber: String, // e.g. "ORD-1001", "PRJ-204", "REP-502"
    val customerId: String,
    val customerName: String,
    val customerPhone: String = "",
    val title: String,
    val currentStageId: String,
    val assignedStaffName: String = "Unassigned",
    val items: List<LineItem> = emptyList(),
    val customFieldValues: Map<String, String> = emptyMap(),
    val notes: String = "",
    val dueDate: Long = System.currentTimeMillis() + 86400000L * 3, // 3 days default
    val totalAmount: Double = 0.0,
    val advancePaid: Double = 0.0,
    val balanceDue: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncState: String = "SYNCED"
)
