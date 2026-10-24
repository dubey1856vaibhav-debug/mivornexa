package com.example.bizflow.model

import java.util.UUID

data class Customer(
    val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val name: String,
    val phone: String,
    val email: String,
    val address: String = "",
    val companyName: String = "",
    val tags: List<String> = emptyList(),
    val notes: String = "",
    val customFieldValues: Map<String, String> = emptyMap(),
    val totalOrdersCount: Int = 0,
    val totalSpent: Double = 0.0,
    val outstandingBalance: Double = 0.0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val syncState: String = "SYNCED" // SYNCED, PENDING_SYNC, OFFLINE_DRAFT
)
