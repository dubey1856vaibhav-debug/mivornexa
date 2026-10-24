package com.example.bizflow.model

import java.util.UUID

enum class ItemType {
    PRODUCT,
    SERVICE
}

data class StockMovement(
    val id: String = UUID.randomUUID().toString(),
    val productId: String,
    val quantityChange: Double,
    val reason: String, // "Restock", "Sale", "Damage", "Audit Adjustment"
    val previousStock: Double,
    val newStock: Double,
    val timestamp: Long = System.currentTimeMillis()
)

data class ProductItem(
    val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val name: String,
    val sku: String = "",
    val type: ItemType = ItemType.PRODUCT,
    val category: String = "General",
    val unitPrice: Double = 0.0,
    val costPrice: Double = 0.0,
    val currentStock: Double = 0.0,
    val minStockThreshold: Double = 5.0,
    val unitOfMeasure: String = "pcs",
    val supplierName: String = "",
    val description: String = "",
    val isActive: Boolean = true,
    val syncState: String = "SYNCED"
) {
    val isLowStock: Boolean
        get() = type == ItemType.PRODUCT && currentStock <= minStockThreshold
}
