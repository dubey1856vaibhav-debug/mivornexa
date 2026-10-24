package com.example.bizflow.model

import java.util.UUID

enum class TaskPriority(val displayName: String) {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    URGENT("Urgent")
}

enum class TaskStatus(val displayName: String) {
    TODO("To Do"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed")
}

data class TeamMember(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val email: String,
    val role: UserRole,
    val department: String = "Operations"
)

data class TeamTask(
    val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val title: String,
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.TODO,
    val assignedToMemberName: String = "Unassigned",
    val relatedRecordId: String? = null,
    val dueDate: Long = System.currentTimeMillis() + 86400000L * 2,
    val createdAt: Long = System.currentTimeMillis(),
    val syncState: String = "SYNCED"
)

enum class EmailSendStatus {
    IDLE,
    SENDING,
    SENT_SUCCESS,
    FAILED,
    QUEUED_OFFLINE
}

enum class EmailTemplateType(val displayName: String) {
    INVOICE_BILL("Invoice & Bill"),
    PAYMENT_RECEIPT("Payment Receipt"),
    WORK_STATUS_UPDATE("Work Order Status Update"),
    PAYMENT_REMINDER("Payment Due Reminder"),
    APPOINTMENT_CONFIRMATION("Appointment Confirmation")
}

data class EmailDraft(
    val id: String = UUID.randomUUID().toString(),
    val workspaceId: String,
    val recipientEmail: String,
    val recipientName: String,
    val subject: String,
    val bodyHtml: String,
    val templateType: EmailTemplateType,
    val relatedEntityId: String,
    val createdAt: Long = System.currentTimeMillis(),
    val status: EmailSendStatus = EmailSendStatus.IDLE,
    val errorMessage: String? = null
)
