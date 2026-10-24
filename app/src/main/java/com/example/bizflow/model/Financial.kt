package com.example.bizflow.model

import java.util.UUID

enum class InvoiceType(val displayName: String) {
    INVOICE("Tax Invoice"),
    ESTIMATE("Estimate / Quote"),
    RECEIPT("Payment Receipt")
}

enum class InvoiceStatus(val displayName: String) {
    DRAFT("Draft"),
    SENT("Sent"),
    PARTIALLY_PAID("Partially Paid"),
    PAID("Paid"),
    OVERDUE("Overdue"),
    CANCELLED("Cancelled")
}

enum class PaymentMethod(val displayName: String) {
    CASH("Cash"),
    BANK_TRANSFER("Bank Transfer"),
    UPI("UPI / QR Code"),
    CARD("Credit/Debit Card"),
    CHEQUE("Cheque")
}

data class Payment(
    val id: String = UUID.randomUUID().toString(),
    val invoiceId: String,
    val workspaceId: String,
    val amount: Double,
    val method: PaymentMethod,
    val referenceNumber: String = "",
    val paymentType: String = "PARTIAL", // ADVANCE, PARTIAL, FINAL, REFUND
    val notes: String = "",
    val recordedBy: String = "Staff",
    val timestamp: Long = System.currentTimeMillis()
)

data class Invoice(
    val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val invoiceNumber: String, // e.g. "INV-2026-001"
    val customerId: String,
    val customerName: String,
    val customerEmail: String = "",
    val customerAddress: String = "",
    val type: InvoiceType = InvoiceType.INVOICE,
    val status: InvoiceStatus = InvoiceStatus.DRAFT,
    val workRecordId: String? = null,
    val items: List<LineItem> = emptyList(),
    val subtotal: Double = 0.0,
    val taxRatePercent: Double = 0.0,
    val taxAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val totalPaid: Double = 0.0,
    val balanceDue: Double = 0.0,
    val payments: List<Payment> = emptyList(),
    val issueDate: Long = System.currentTimeMillis(),
    val dueDate: Long = System.currentTimeMillis() + 86400000L * 14, // 14 days
    val notes: String = "Thank you for your business!",
    val syncState: String = "SYNCED"
)

enum class ExpenseCategory(val displayName: String) {
    INVENTORY_MATERIALS("Inventory & Raw Materials"),
    RENT_FACILITIES("Rent & Facility Costs"),
    UTILITIES("Utilities & Internet"),
    PAYROLL_SALARIES("Payroll & Contractors"),
    MARKETING("Marketing & Ads"),
    EQUIPMENT_TOOLS("Equipment & Maintenance"),
    TRAVEL_TRANSPORT("Travel & Transport"),
    OFFICE_SUPPLIES("Office & Administrative"),
    TAXES_LICENSES("Taxes & Govt Fees"),
    MISCELLANEOUS("Miscellaneous")
}

data class Expense(
    val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val title: String,
    val category: ExpenseCategory,
    val amount: Double,
    val paidTo: String,
    val paymentMethod: PaymentMethod = PaymentMethod.CASH,
    val referenceReceiptNo: String = "",
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val recordedBy: String = "Admin",
    val syncState: String = "SYNCED"
)
