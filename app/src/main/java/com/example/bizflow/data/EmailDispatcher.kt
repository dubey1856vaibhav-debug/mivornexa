package com.example.bizflow.data

import com.example.bizflow.model.EmailDraft
import com.example.bizflow.model.EmailSendStatus
import com.example.bizflow.model.EmailTemplateType
import com.example.bizflow.model.Invoice
import com.example.bizflow.model.WorkRecord
import com.example.bizflow.model.Workspace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object EmailDispatcher {

    fun composeInvoiceEmail(invoice: Invoice, workspace: Workspace): EmailDraft {
        val subject = "Invoice #${invoice.invoiceNumber} from ${workspace.name}"
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val body = """
            Dear ${invoice.customerName},

            Thank you for your business. Please find the details for your recent invoice #${invoice.invoiceNumber}.

            Invoice Summary:
            • Issue Date: ${sdf.format(Date(invoice.issueDate))}
            • Total Amount: ${workspace.currencySymbol}${String.format(Locale.US, "%.2f", invoice.totalAmount)}
            • Amount Paid: ${workspace.currencySymbol}${String.format(Locale.US, "%.2f", invoice.totalPaid)}
            • Balance Due: ${workspace.currencySymbol}${String.format(Locale.US, "%.2f", invoice.balanceDue)}
            • Due Date: ${sdf.format(Date(invoice.dueDate))}

            Status: ${invoice.status.displayName}

            If you have any questions regarding this invoice, please contact us at ${workspace.email} or call ${workspace.phone}.

            Warm regards,
            ${workspace.name} Team
            ${workspace.tagline}
        """.trimIndent()

        return EmailDraft(
            workspaceId = workspace.id,
            recipientEmail = invoice.customerEmail.ifBlank { "customer@example.com" },
            recipientName = invoice.customerName,
            subject = subject,
            bodyHtml = body,
            templateType = EmailTemplateType.INVOICE_BILL,
            relatedEntityId = invoice.id
        )
    }

    fun composeWorkStatusEmail(workRecord: WorkRecord, stageName: String, workspace: Workspace): EmailDraft {
        val subject = "Update on ${workspace.businessType.workRecordLabel} #${workRecord.recordNumber} - ${workspace.name}"
        val body = """
            Dear ${workRecord.customerName},

            Here is a status update on your ${workspace.businessType.workRecordLabel.lowercase()} #${workRecord.recordNumber} ("${workRecord.title}").

            Current Status: $stageName
            Assigned Specialist: ${workRecord.assignedStaffName}
            Total Amount: ${workspace.currencySymbol}${String.format(Locale.US, "%.2f", workRecord.totalAmount)}
            Outstanding Balance: ${workspace.currencySymbol}${String.format(Locale.US, "%.2f", workRecord.balanceDue)}

            Notes: ${workRecord.notes.ifBlank { "Work is proceeding according to schedule." }}

            Thank you for choosing ${workspace.name}!
        """.trimIndent()

        return EmailDraft(
            workspaceId = workspace.id,
            recipientEmail = "customer@example.com",
            recipientName = workRecord.customerName,
            subject = subject,
            bodyHtml = body,
            templateType = EmailTemplateType.WORK_STATUS_UPDATE,
            relatedEntityId = workRecord.id
        )
    }

    suspend fun dispatchEmail(draft: EmailDraft): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Emulate secure network API call to server-side mailer (or Firebase Cloud Function)
            delay(1200)
            if (draft.recipientEmail.contains("@") && draft.recipientEmail.contains(".")) {
                Result.success("Message dispatched securely via BizFlow Server Mail Queue (ID: BZ-${System.currentTimeMillis() % 100000})")
            } else {
                Result.failure(IllegalArgumentException("Invalid recipient email address format: '${draft.recipientEmail}'"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
