package com.example.bizflow.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.bizflow.data.EmailDispatcher
import com.example.bizflow.model.EmailDraft
import com.example.bizflow.model.EmailSendStatus
import kotlinx.coroutines.launch

@Composable
fun EmailPreviewDialog(
    initialDraft: EmailDraft,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    var recipientEmail by remember { mutableStateOf(initialDraft.recipientEmail) }
    var subject by remember { mutableStateOf(initialDraft.subject) }
    var body by remember { mutableStateOf(initialDraft.bodyHtml) }
    var sendStatus by remember { mutableStateOf(initialDraft.status) }
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var isSending by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = { if (!isSending) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Email,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Customer Email Notification",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                item {
                    Text(
                        text = "Template: ${initialDraft.templateType.displayName}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (statusMessage != null) {
                    item {
                        Card(
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (sendStatus == EmailSendStatus.SENT_SUCCESS) Color(0xFFD1FAE5) else Color(0xFFFEE2E2)
                            ),
                            modifier = Modifier.fillMaxWidth().testTag("email_status_banner")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (sendStatus == EmailSendStatus.SENT_SUCCESS) Icons.Default.CheckCircle else Icons.Default.Error,
                                    contentDescription = null,
                                    tint = if (sendStatus == EmailSendStatus.SENT_SUCCESS) Color(0xFF065F46) else Color(0xFF991B1B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = statusMessage ?: "",
                                    fontSize = 12.sp,
                                    color = if (sendStatus == EmailSendStatus.SENT_SUCCESS) Color(0xFF065F46) else Color(0xFF991B1B)
                                )
                            }
                        }
                    }
                }

                item {
                    OutlinedTextField(
                        value = recipientEmail,
                        onValueChange = { recipientEmail = it },
                        label = { Text("Recipient Email *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("email_recipient_input")
                    )
                }

                item {
                    OutlinedTextField(
                        value = subject,
                        onValueChange = { subject = it },
                        label = { Text("Subject *") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    OutlinedTextField(
                        value = body,
                        onValueChange = { body = it },
                        label = { Text("Message Body (Preview & Edit)") },
                        minLines = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                item {
                    Text(
                        text = "Security guarantee: Sent via server-side endpoint. Zero SMTP credentials or sensitive API tokens are stored in this Android client.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val updatedDraft = initialDraft.copy(
                        recipientEmail = recipientEmail.trim(),
                        subject = subject.trim(),
                        bodyHtml = body.trim()
                    )
                    isSending = true
                    sendStatus = EmailSendStatus.SENDING
                    statusMessage = "Connecting to BizFlow Server Mail Queue..."

                    coroutineScope.launch {
                        val result = EmailDispatcher.dispatchEmail(updatedDraft)
                        isSending = false
                        result.fold(
                            onSuccess = { msg ->
                                sendStatus = EmailSendStatus.SENT_SUCCESS
                                statusMessage = msg
                            },
                            onFailure = { err ->
                                sendStatus = EmailSendStatus.FAILED
                                statusMessage = "Failed: ${err.message}. Your draft was preserved."
                            }
                        )
                    }
                },
                enabled = !isSending && recipientEmail.isNotBlank() && sendStatus != EmailSendStatus.SENT_SUCCESS,
                modifier = Modifier.testTag("send_email_button")
            ) {
                if (isSending) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dispatching...")
                } else if (sendStatus == EmailSendStatus.SENT_SUCCESS) {
                    Text("Sent Successfully")
                } else {
                    Text("Send via Server")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSending
            ) {
                Text(if (sendStatus == EmailSendStatus.SENT_SUCCESS) "Close" else "Cancel")
            }
        }
    )
}
