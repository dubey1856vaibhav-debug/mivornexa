package com.example.bizflow.data

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.bizflow.model.Invoice
import com.example.bizflow.model.Workspace
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfGenerator {

    fun generateInvoicePdf(context: Context, invoice: Invoice, workspace: Workspace): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 points
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42) // #0F172A
            textSize = 20f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = Paint().apply {
            color = Color.rgb(2, 132, 199) // #0284C7
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val headerPaint = Paint().apply {
            color = Color.rgb(51, 65, 85) // #334155
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val textPaint = Paint().apply {
            color = Color.rgb(30, 41, 59)
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val secondaryPaint = Paint().apply {
            color = Color.rgb(100, 116, 139)
            textSize = 9f
        }

        val linePaint = Paint().apply {
            color = Color.rgb(226, 232, 240)
            strokeWidth = 1f
        }

        val boxPaint = Paint().apply {
            color = Color.rgb(241, 245, 249)
            style = Paint.Style.FILL
        }

        val accentBarPaint = Paint().apply {
            color = Color.rgb(2, 132, 199)
            style = Paint.Style.FILL
        }

        // Top brand bar
        canvas.drawRect(0f, 0f, 595f, 12f, accentBarPaint)

        // Workspace header
        var y = 50f
        canvas.drawText(workspace.name, 40f, y, titlePaint)
        y += 18f
        canvas.drawText(workspace.tagline, 40f, y, subtitlePaint)
        y += 14f
        canvas.drawText("${workspace.address}  |  ${workspace.phone}  |  ${workspace.email}", 40f, y, secondaryPaint)

        // Invoice badge on right
        val invTypeLabel = invoice.type.displayName.uppercase()
        canvas.drawText(invTypeLabel, 430f, 50f, titlePaint)
        canvas.drawText("No: ${invoice.invoiceNumber}", 430f, 68f, headerPaint)
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        canvas.drawText("Date: ${sdf.format(Date(invoice.issueDate))}", 430f, 82f, secondaryPaint)
        canvas.drawText("Status: ${invoice.status.displayName}", 430f, 96f, secondaryPaint)

        // Divider
        y += 25f
        canvas.drawLine(40f, y, 555f, y, linePaint)

        // Bill to section
        y += 20f
        canvas.drawText("BILL TO:", 40f, y, headerPaint)
        y += 14f
        canvas.drawText(invoice.customerName, 40f, y, titlePaint.apply { textSize = 13f })
        titlePaint.textSize = 20f // restore
        y += 14f
        if (invoice.customerEmail.isNotEmpty()) {
            canvas.drawText(invoice.customerEmail, 40f, y, textPaint)
            y += 14f
        }
        if (invoice.customerAddress.isNotEmpty()) {
            canvas.drawText(invoice.customerAddress, 40f, y, textPaint)
            y += 14f
        }

        // Table Header
        y += 15f
        canvas.drawRect(40f, y, 555f, y + 24f, boxPaint)
        canvas.drawText("ITEM / DESCRIPTION", 50f, y + 16f, headerPaint)
        canvas.drawText("QTY", 320f, y + 16f, headerPaint)
        canvas.drawText("PRICE", 380f, y + 16f, headerPaint)
        canvas.drawText("AMOUNT (${workspace.currencySymbol})", 450f, y + 16f, headerPaint)

        y += 24f

        // Table Rows
        for (item in invoice.items) {
            y += 20f
            canvas.drawText(item.description.take(40), 50f, y, textPaint)
            canvas.drawText(String.format(Locale.US, "%.1f", item.quantity), 320f, y, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.unitPrice), 380f, y, textPaint)
            canvas.drawText(String.format(Locale.US, "%.2f", item.totalAmount), 460f, y, textPaint)
            canvas.drawLine(40f, y + 6f, 555f, y + 6f, linePaint)
        }

        // Financial totals box
        y += 35f
        val totalsStartX = 340f
        canvas.drawText("Subtotal:", totalsStartX, y, textPaint)
        canvas.drawText("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", invoice.subtotal)}", 480f, y, textPaint)

        if (invoice.taxAmount > 0) {
            y += 16f
            canvas.drawText("Tax (${invoice.taxRatePercent}%):", totalsStartX, y, textPaint)
            canvas.drawText("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", invoice.taxAmount)}", 480f, y, textPaint)
        }

        if (invoice.discountAmount > 0) {
            y += 16f
            canvas.drawText("Discount:", totalsStartX, y, textPaint)
            canvas.drawText("-${workspace.currencySymbol}${String.format(Locale.US, "%.2f", invoice.discountAmount)}", 480f, y, textPaint)
        }

        y += 20f
        canvas.drawLine(totalsStartX, y - 6f, 555f, y - 6f, linePaint)
        canvas.drawText("Total Amount:", totalsStartX, y, headerPaint.apply { textSize = 11f })
        canvas.drawText("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", invoice.totalAmount)}", 480f, y, headerPaint)

        y += 18f
        canvas.drawText("Payments Applied:", totalsStartX, y, textPaint)
        canvas.drawText("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", invoice.totalPaid)}", 480f, y, textPaint)

        y += 20f
        canvas.drawRect(totalsStartX - 10f, y - 14f, 555f, y + 8f, boxPaint)
        canvas.drawText("Balance Due:", totalsStartX, y, titlePaint.apply { textSize = 12f; color = Color.rgb(2, 132, 199) })
        canvas.drawText("${workspace.currencySymbol}${String.format(Locale.US, "%.2f", invoice.balanceDue)}", 480f, y, titlePaint)

        // Notes and Footer
        y += 50f
        canvas.drawText("Notes & Terms:", 40f, y, headerPaint.apply { textSize = 10f; color = Color.rgb(51, 65, 85) })
        y += 14f
        canvas.drawText(invoice.notes, 40f, y, textPaint)

        // Bottom Footer
        canvas.drawText("Generated by BizFlow Mobile Companion • Confidential & Proprietary", 140f, 800f, secondaryPaint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "${invoice.invoiceNumber}.pdf")
        FileOutputStream(file).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()
        return file
    }

    fun sharePdf(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(shareIntent, "Share Document via..."))
    }
}
