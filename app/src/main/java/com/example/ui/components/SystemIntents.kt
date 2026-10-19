package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.CalendarContract
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.model.QuotationEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object SystemIntents {

    fun dialPhoneNumber(context: Context, phoneNumber: String) {
        try {
            val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
            if (cleanNumber.isBlank()) {
                Toast.makeText(context, "No phone number available", Toast.LENGTH_SHORT).show()
                return
            }
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$cleanNumber")).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open phone dialer: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openWhatsAppOrSms(context: Context, phoneNumber: String, clientName: String, eventType: String, messageText: String? = null) {
        try {
            var cleanNumber = phoneNumber.replace(Regex("[^0-9]"), "")
            if (cleanNumber.length == 10) {
                cleanNumber = "91$cleanNumber" // Default India country code
            }
            val text = messageText ?: "Hello $clientName,\nGreeting from BMS Studios! Regarding your $eventType booking."
            val encodedText = Uri.encode(text)

            // Try opening WhatsApp first
            val whatsappUri = Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=$encodedText")
            val whatsappIntent = Intent(Intent.ACTION_VIEW, whatsappUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(whatsappIntent)
        } catch (e: Exception) {
            // Fallback to standard SMS
            try {
                val clean = phoneNumber.replace(Regex("[^0-9+]"), "")
                val smsIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$clean")).apply {
                    putExtra("sms_body", messageText ?: "Hello $clientName, greeting from BMS Studios!")
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }
                context.startActivity(smsIntent)
            } catch (ex: Exception) {
                Toast.makeText(context, "Cannot open messaging: ${ex.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun addToCalendar(
        context: Context,
        title: String,
        description: String,
        location: String,
        dateStr: String = ""
    ) {
        try {
            val intent = Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title)
                .putExtra(CalendarContract.Events.DESCRIPTION, description)
                .putExtra(CalendarContract.Events.EVENT_LOCATION, location)
                .putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true)
                .apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK
                }

            // Attempt to parse date or default to tomorrow
            try {
                val format = SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH)
                val parsed = format.parse(dateStr)
                if (parsed != null) {
                    intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, parsed.time)
                    intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, parsed.time + (8 * 3600 * 1000))
                }
            } catch (_: Exception) {
                val now = System.currentTimeMillis() + (24 * 3600 * 1000)
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, now)
            }

            context.startActivity(intent)
            Toast.makeText(context, "Opening Google Calendar to schedule...", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Calendar app not found: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun openVenueInMaps(context: Context, venueName: String, city: String = "") {
        try {
            val query = if (city.isNotBlank()) "$venueName, $city" else venueName
            val gmmIntentUri = Uri.parse("geo:0,0?q=" + Uri.encode(query))
            val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(mapIntent)
        } catch (e: Exception) {
            // Web fallback
            val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=" + Uri.encode("$venueName $city"))
            val webIntent = Intent(Intent.ACTION_VIEW, webUri).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
            context.startActivity(webIntent)
        }
    }

    fun openExternalUrl(context: Context, url: String) {
        try {
            var target = url.trim()
            if (!target.startsWith("http://") && !target.startsWith("https://")) {
                target = "https://$target"
            }
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot open link: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun generateAndShareQuotationPdf(
        context: Context,
        quotation: QuotationEntity,
        studioName: String,
        studioGst: String,
        studioAddress: String
    ) {
        try {
            val pdfDoc = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 (595x842 pt)
            val page = pdfDoc.startPage(pageInfo)
            val canvas = page.canvas

            val paint = Paint().apply { isAntiAlias = true }

            // Header Background Banner
            paint.color = Color.parseColor("#0F172A")
            canvas.drawRect(0f, 0f, 595f, 110f, paint)

            // Studio Name
            paint.color = Color.WHITE
            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(studioName, 40f, 48f, paint)

            // Subtitle
            paint.color = Color.parseColor("#38BDF8")
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Commercial Estimate & Package Proposal (BOSS)", 40f, 68f, paint)

            paint.color = Color.parseColor("#94A3B8")
            paint.textSize = 9f
            canvas.drawText("GSTIN: $studioGst | $studioAddress", 40f, 86f, paint)

            // Quotation Number & Date right-aligned
            paint.color = Color.parseColor("#F59E0B")
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText(quotation.quotationNumber, 400f, 48f, paint)

            paint.color = Color.WHITE
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Date: ${quotation.eventDate}", 400f, 68f, paint)
            canvas.drawText("Status: ${quotation.status.uppercase()}", 400f, 86f, paint)

            // Client Info Box
            var y = 145f
            paint.color = Color.parseColor("#F1F5F9")
            canvas.drawRoundRect(40f, y, 555f, y + 65f, 8f, 8f, paint)

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 12f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("ISSUED TO (CLIENT):", 55f, y + 22f, paint)

            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText(quotation.clientName, 55f, y + 38f, paint)
            paint.color = Color.parseColor("#475569")
            canvas.drawText("Phone: ${quotation.contact} | Location: ${quotation.eventLocation}", 55f, y + 54f, paint)

            // Deliverables / Package
            y += 85f
            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 13f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("PACKAGE SPECIFICATIONS & SCOPE OF WORK", 40f, y, paint)

            y += 20f
            paint.color = Color.parseColor("#334155")
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("Package: ${quotation.packageDetails}", 40f, y, paint)

            y += 18f
            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val serviceLines = quotation.services.chunked(70)
            for (line in serviceLines) {
                canvas.drawText("• $line", 45f, y, paint)
                y += 16f
            }

            // Financial Breakdown Table
            y += 15f
            paint.color = Color.parseColor("#E2E8F0")
            canvas.drawRect(40f, y, 555f, y + 1f, paint)

            y += 22f
            paint.color = Color.parseColor("#475569")
            paint.textSize = 11f
            canvas.drawText("Base Subtotal", 45f, y, paint)
            canvas.drawText("₹${quotation.subtotal.toInt()}", 480f, y, paint)

            if (quotation.discount > 0) {
                y += 18f
                paint.color = Color.parseColor("#10B981")
                canvas.drawText("Special Studio Discount", 45f, y, paint)
                canvas.drawText("-₹${quotation.discount.toInt()}", 480f, y, paint)
            }

            y += 18f
            paint.color = Color.parseColor("#475569")
            canvas.drawText("Applicable GST (${quotation.taxRate.toInt()}%)", 45f, y, paint)
            canvas.drawText("+₹${quotation.taxAmount.toInt()}", 480f, y, paint)

            y += 12f
            paint.color = Color.parseColor("#0F172A")
            canvas.drawRect(40f, y, 555f, y + 2f, paint)

            y += 24f
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 14f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("GRAND TOTAL CONTRACT VALUE", 45f, y, paint)
            paint.color = Color.parseColor("#D97706")
            canvas.drawText("₹${quotation.totalAmount.toInt()}", 470f, y, paint)

            // Payment Terms Box
            y += 35f
            paint.color = Color.parseColor("#EFF6FF")
            canvas.drawRoundRect(40f, y, 555f, y + 70f, 8f, 8f, paint)

            paint.color = Color.parseColor("#1E3A8A")
            paint.textSize = 11f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("SCHEDULE OF PAYMENTS:", 55f, y + 22f, paint)

            paint.textSize = 10f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("1. Advance on Booking Confirmation: ₹${quotation.advanceRequired.toInt()} (30%)", 55f, y + 38f, paint)
            canvas.drawText("2. Remaining Balance: ₹${quotation.balanceAmount.toInt()} (Pre-delivery handover)", 55f, y + 54f, paint)

            // Terms
            y += 90f
            paint.color = Color.parseColor("#64748B")
            paint.textSize = 9f
            canvas.drawText("Terms & Conditions: ${quotation.terms}", 40f, y, paint)
            y += 14f
            canvas.drawText("Generated electronically via Business Operational Studio System (BOSS).", 40f, y, paint)

            // Signatures
            y += 45f
            paint.color = Color.parseColor("#94A3B8")
            canvas.drawRect(40f, y, 180f, y + 1f, paint)
            canvas.drawRect(400f, y, 540f, y + 1f, paint)

            paint.color = Color.parseColor("#475569")
            paint.textSize = 9f
            canvas.drawText("Client Acceptance", 65f, y + 14f, paint)
            canvas.drawText("Authorized Studio Signatory", 405f, y + 14f, paint)

            pdfDoc.finishPage(page)

            // Save PDF to cache
            val outputDir = File(context.cacheDir, "quotations").apply { mkdirs() }
            val cleanId = quotation.quotationNumber.replace(Regex("[^a-zA-Z0-9_-]"), "_")
            val pdfFile = File(outputDir, "Quotation_$cleanId.pdf")
            val fos = FileOutputStream(pdfFile)
            pdfDoc.writeTo(fos)
            fos.close()
            pdfDoc.close()

            // Share PDF
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", pdfFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "Quotation ${quotation.quotationNumber} - $studioName")
                putExtra(Intent.EXTRA_TEXT, "Hello ${quotation.clientName},\nPlease find attached the official commercial quotation ${quotation.quotationNumber} from $studioName.\nTotal: ₹${quotation.totalAmount.toInt()} (Includes 18% GST).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Quotation PDF via...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            Toast.makeText(context, "Quotation PDF generated & ready to share!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Error generating PDF: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    fun shareCsvData(context: Context, fileName: String, csvContent: String, title: String) {
        try {
            val outputDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val csvFile = File(outputDir, "$fileName.csv")
            csvFile.writeText(csvContent)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", csvFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, "$title (BOSS Export)")
                putExtra(Intent.EXTRA_TEXT, "Exported $title from Business Operational Studio System (BOSS).")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export $title to...").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            Toast.makeText(context, "Exported $fileName.csv successfully!", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to export CSV: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }
}
