package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.model.*
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object DocumentExportUtil {

    fun generateQuotationPdf(
        context: Context,
        quotation: QuotationEntity,
        workspace: WorkspaceEntity?
    ): File {
        val studioName = workspace?.name ?: "Shakir Photography & Cinema"
        val studioGst = workspace?.gstNumber ?: "27AAACL1234F1Z8"
        val studioPhone = workspace?.phone ?: "+91 98234 56789"
        val studioEmail = workspace?.email ?: "shakirtamboli.pune11@gmail.com"
        val studioAddress = workspace?.address ?: "Penthouse 4B, Sky Vista Tower, Viman Nagar, Pune"

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 (points)
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }

        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Header Background Banner
        paint.color = Color.rgb(15, 23, 42) // Dark Slate
        canvas.drawRect(0f, 0f, 595f, 110f, paint)

        // Studio Name & Branding
        paint.color = Color.WHITE
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(studioName, 40f, 45f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(203, 213, 225) // Slate-300
        canvas.drawText("$studioAddress • Phone: $studioPhone", 40f, 65f, paint)
        canvas.drawText("Email: $studioEmail • GSTIN: $studioGst", 40f, 82f, paint)

        // Quotation Badge
        paint.color = Color.rgb(37, 99, 235) // Blue-600
        canvas.drawRoundRect(415f, 25f, 555f, 85f, 8f, 8f, paint)
        paint.color = Color.WHITE
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PROPOSAL", 445f, 48f, paint)
        paint.textSize = 10f
        canvas.drawText(quotation.quotationNumber, 430f, 68f, paint)

        // Divider Line
        paint.color = Color.rgb(37, 99, 235)
        paint.strokeWidth = 3f
        canvas.drawLine(40f, 110f, 555f, 110f, paint)
        paint.strokeWidth = 1f

        var currentY = 140f

        // Two Column Section: Quotation Details & Client Info
        paint.color = Color.rgb(241, 245, 249) // Slate-100 Box
        canvas.drawRoundRect(40f, currentY, 280f, currentY + 95f, 8f, 8f, paint)
        canvas.drawRoundRect(300f, currentY, 555f, currentY + 95f, 8f, 8f, paint)

        // Client Info Box
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PROPOSAL PREPARED FOR:", 52f, currentY + 20f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 13f
        canvas.drawText(quotation.clientName, 52f, currentY + 40f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Contact: ${quotation.contact}", 52f, currentY + 58f, paint)
        canvas.drawText("Venue / City: ${quotation.eventLocation.ifBlank { "Pune" }}", 52f, currentY + 75f, paint)

        // Event / Schedule Box
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("EVENT & SCHEDULE DETAILS:", 312f, currentY + 20f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 11f
        canvas.drawText("Event Date: ${quotation.eventDate}", 312f, currentY + 40f, paint)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        canvas.drawText("Status: ${quotation.status}", 312f, currentY + 58f, paint)
        canvas.drawText("Payment Terms: 30% Adv • 50% Shoot • 20% Delivery", 312f, currentY + 75f, paint)

        currentY += 120f

        // Scope of Services Header
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SCOPE OF WORK & PRODUCTION DELIVERABLES", 40f, currentY, paint)
        currentY += 12f

        // Table Header
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRect(40f, currentY, 555f, currentY + 26f, paint)

        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("#", 50f, currentY + 17f, paint)
        canvas.drawText("Description of Services / Deliverables", 80f, currentY + 17f, paint)
        canvas.drawText("Amount (INR)", 470f, currentY + 17f, paint)

        currentY += 26f

        // Row 1: Primary Services
        paint.color = Color.rgb(255, 255, 255)
        canvas.drawRect(40f, currentY, 555f, currentY + 36f, paint)
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawLine(40f, currentY + 36f, 555f, currentY + 36f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        canvas.drawText("1", 50f, currentY + 22f, paint)
        canvas.drawText(quotation.services.take(65), 80f, currentY + 22f, paint)
        val subtotalStr = "₹${"%,d".format(quotation.subtotal.toLong())}"
        canvas.drawText(subtotalStr, 470f, currentY + 22f, paint)

        currentY += 36f

        // Row 2: Package details if any
        if (quotation.packageDetails.isNotBlank()) {
            paint.color = Color.rgb(255, 255, 255)
            canvas.drawRect(40f, currentY, 555f, currentY + 30f, paint)
            paint.color = Color.rgb(226, 232, 240)
            canvas.drawLine(40f, currentY + 30f, 555f, currentY + 30f, paint)

            paint.color = Color.rgb(100, 116, 139)
            paint.textSize = 9f
            canvas.drawText("•", 50f, currentY + 18f, paint)
            canvas.drawText(quotation.packageDetails.take(75), 80f, currentY + 18f, paint)
            canvas.drawText("Included", 470f, currentY + 18f, paint)
            currentY += 30f
        }

        currentY += 15f

        // Summary Calculations (Right-Aligned Card)
        val calcCardLeft = 320f
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRoundRect(calcCardLeft, currentY, 555f, currentY + 140f, 8f, 8f, paint)
        paint.color = Color.rgb(203, 213, 225)
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(calcCardLeft, currentY, 555f, currentY + 140f, 8f, 8f, paint)
        paint.style = Paint.Style.FILL

        var calcY = currentY + 22f
        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(71, 85, 105)

        // Subtotal
        canvas.drawText("Subtotal:", calcCardLeft + 16f, calcY, paint)
        canvas.drawText("₹${"%,d".format(quotation.subtotal.toLong())}", 470f, calcY, paint)
        calcY += 20f

        // Discount
        if (quotation.discount > 0) {
            canvas.drawText("Discount:", calcCardLeft + 16f, calcY, paint)
            paint.color = Color.rgb(16, 185, 129) // Emerald
            canvas.drawText("-₹${"%,d".format(quotation.discount.toLong())}", 470f, calcY, paint)
            paint.color = Color.rgb(71, 85, 105)
            calcY += 20f
        }

        // GST
        canvas.drawText("GST @ 18% (CGST 9% + SGST 9%):", calcCardLeft + 16f, calcY, paint)
        canvas.drawText("+₹${"%,d".format(quotation.taxAmount.toLong())}", 470f, calcY, paint)
        calcY += 22f

        // Grand Total
        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("Total Payable:", calcCardLeft + 16f, calcY, paint)
        paint.color = Color.rgb(37, 99, 235)
        canvas.drawText("₹${"%,d".format(quotation.totalAmount.toLong())}", 470f, calcY, paint)
        calcY += 22f

        // Advance Required
        paint.textSize = 10f
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("Advance Required (30%):", calcCardLeft + 16f, calcY, paint)
        canvas.drawText("₹${"%,d".format(quotation.advanceRequired.toLong())}", 470f, calcY, paint)

        currentY += 155f

        // Terms & Conditions Block
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TERMS & CONDITIONS", 40f, currentY, paint)
        currentY += 16f

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        val termsList = listOf(
            "1. 30% advance required upon proposal confirmation to block shoot dates.",
            "2. 50% payment payable on the primary shoot date.",
            "3. Final balance 20% payable before final delivery of raw assets and edited albums/films.",
            "4. All creative deliverables remain property of the studio until full commercial settlement."
        )
        for (term in termsList) {
            canvas.drawText(term, 40f, currentY, paint)
            currentY += 14f
        }

        currentY += 25f

        // Signature & Approval Block
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawLine(380f, currentY + 30f, 555f, currentY + 30f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("For $studioName", 380f, currentY + 45f, paint)
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("Authorized Signatory", 380f, currentY + 58f, paint)

        // Footer note
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        canvas.drawText("Generated by APP BOSS • Business Operation Studio System • Shakir Photography & Cinema", 40f, 815f, paint)

        pdfDocument.finishPage(page)

        val cleanNumber = quotation.quotationNumber.replace("/", "_").replace("-", "_")
        val file = File(context.cacheDir, "Quotation_${cleanNumber}.pdf")
        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            try { pdfDocument.close() } catch (_: Exception) {}
        }
        return file
    }

    fun generateQuotationXlsx(
        context: Context,
        quotation: QuotationEntity,
        workspace: WorkspaceEntity?
    ): File {
        val studioName = workspace?.name ?: "Shakir Photography & Cinema"
        val studioGst = workspace?.gstNumber ?: "27AAACL1234F1Z8"
        val cleanNumber = quotation.quotationNumber.replace("/", "_").replace("-", "_")
        val file = File(context.cacheDir, "Quotation_${cleanNumber}.xlsx")

        // Build valid OpenXML Spreadsheet (.xlsx) package
        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

        val packageRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Quotation" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

        val workbookRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""

        val sheet1Xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
    <row r="1">
      <c r="A1" t="inlineStr"><is><t>$studioName - COMMERCIAL QUOTATION</t></is></c>
    </row>
    <row r="2">
      <c r="A2" t="inlineStr"><is><t>GSTIN: $studioGst</t></is></c>
      <c r="B2" t="inlineStr"><is><t>Quotation Number: ${quotation.quotationNumber}</t></is></c>
    </row>
    <row r="3">
      <c r="A3" t="inlineStr"><is><t>Client Name: ${quotation.clientName}</t></is></c>
      <c r="B3" t="inlineStr"><is><t>Contact: ${quotation.contact}</t></is></c>
    </row>
    <row r="4">
      <c r="A4" t="inlineStr"><is><t>Event Date: ${quotation.eventDate}</t></is></c>
      <c r="B4" t="inlineStr"><is><t>Location: ${quotation.eventLocation}</t></is></c>
    </row>
    <row r="5"/>
    <row r="6">
      <c r="A6" t="inlineStr"><is><t>Item / Service Description</t></is></c>
      <c r="B6" t="inlineStr"><is><t>Amount (INR)</t></is></c>
    </row>
    <row r="7">
      <c r="A7" t="inlineStr"><is><t>${quotation.services}</t></is></c>
      <c r="B7"><v>${quotation.subtotal.toLong()}</v></c>
    </row>
    <row r="8">
      <c r="A8" t="inlineStr"><is><t>Package Details / Scope: ${quotation.packageDetails}</t></is></c>
      <c r="B8" t="inlineStr"><is><t>Included</t></is></c>
    </row>
    <row r="9">
      <c r="A9" t="inlineStr"><is><t>Subtotal</t></is></c>
      <c r="B9"><v>${quotation.subtotal.toLong()}</v></c>
    </row>
    <row r="10">
      <c r="A10" t="inlineStr"><is><t>Special Discount</t></is></c>
      <c r="B10"><v>${quotation.discount.toLong()}</v></c>
    </row>
    <row r="11">
      <c r="A11" t="inlineStr"><is><t>GST @ 18%</t></is></c>
      <c r="B11"><v>${quotation.taxAmount.toLong()}</v></c>
    </row>
    <row r="12">
      <c r="A12" t="inlineStr"><is><t>Total Proposal Amount</t></is></c>
      <c r="B12"><v>${quotation.totalAmount.toLong()}</v></c>
    </row>
    <row r="13">
      <c r="A13" t="inlineStr"><is><t>Advance Required (30%)</t></is></c>
      <c r="B13"><v>${quotation.advanceRequired.toLong()}</v></c>
    </row>
    <row r="14">
      <c r="A14" t="inlineStr"><is><t>Balance Payable on Delivery</t></is></c>
      <c r="B14"><v>${quotation.balanceAmount.toLong()}</v></c>
    </row>
    <row r="15"/>
    <row r="16">
      <c r="A16" t="inlineStr"><is><t>Terms: ${quotation.terms}</t></is></c>
    </row>
  </sheetData>
</worksheet>"""

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            writeZipEntry(zos, "[Content_Types].xml", contentTypes)
            writeZipEntry(zos, "_rels/.rels", packageRels)
            writeZipEntry(zos, "xl/workbook.xml", workbookXml)
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", workbookRels)
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", sheet1Xml)
        }
        return file
    }

    fun generateQuotationDocx(
        context: Context,
        quotation: QuotationEntity,
        workspace: WorkspaceEntity?
    ): File {
        val studioName = workspace?.name ?: "Shakir Photography & Cinema"
        val studioGst = workspace?.gstNumber ?: "27AAACL1234F1Z8"
        val cleanNumber = quotation.quotationNumber.replace("/", "_").replace("-", "_")
        val file = File(context.cacheDir, "Quotation_${cleanNumber}.docx")

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

        val packageRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        val documentXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p>
      <w:pPr><w:jc w:val="center"/></w:pPr>
      <w:r><w:rPr><w:b/><w:sz w:val="36"/></w:rPr><w:t>$studioName</w:t></w:r>
    </w:p>
    <w:p>
      <w:pPr><w:jc w:val="center"/></w:pPr>
      <w:r><w:rPr><w:sz w:val="20"/></w:rPr><w:t>GSTIN: $studioGst | Official Commercial Proposal</w:t></w:r>
    </w:p>
    <w:p><w:r><w:t>__________________________________________________________________________</w:t></w:r></w:p>
    <w:p>
      <w:r><w:rPr><w:b/></w:rPr><w:t>Quotation Number: </w:t></w:r>
      <w:r><w:t>${quotation.quotationNumber}</w:t></w:r>
    </w:p>
    <w:p>
      <w:r><w:rPr><w:b/></w:rPr><w:t>Client Name: </w:t></w:r>
      <w:r><w:t>${quotation.clientName} (Contact: ${quotation.contact})</w:t></w:r>
    </w:p>
    <w:p>
      <w:r><w:rPr><w:b/></w:rPr><w:t>Event Date &amp; Location: </w:t></w:r>
      <w:r><w:t>${quotation.eventDate} @ ${quotation.eventLocation}</w:t></w:r>
    </w:p>
    <w:p><w:r><w:rPr><w:b/><w:sz w:val="24"/></w:rPr><w:t>Scope of Work &amp; Services:</w:t></w:r></w:p>
    <w:p><w:r><w:t>${quotation.services}</w:t></w:r></w:p>
    <w:p><w:r><w:t>Package Details: ${quotation.packageDetails}</w:t></w:r></w:p>
    <w:p><w:r><w:rPr><w:b/><w:sz w:val="24"/></w:rPr><w:t>Commercial Terms &amp; Settlement:</w:t></w:r></w:p>
    <w:p><w:r><w:t>• Subtotal: INR ${"%,d".format(quotation.subtotal.toLong())}</w:t></w:r></w:p>
    <w:p><w:r><w:t>• Discount: INR ${"%,d".format(quotation.discount.toLong())}</w:t></w:r></w:p>
    <w:p><w:r><w:t>• GST @ 18%: INR ${"%,d".format(quotation.taxAmount.toLong())}</w:t></w:r></w:p>
    <w:p><w:r><w:rPr><w:b/></w:rPr><w:t>• Grand Total Payable: INR ${"%,d".format(quotation.totalAmount.toLong())}</w:t></w:r></w:p>
    <w:p><w:r><w:t>• Advance Booking Deposit (30%): INR ${"%,d".format(quotation.advanceRequired.toLong())}</w:t></w:r></w:p>
    <w:p><w:r><w:t>• Balance on Completion: INR ${"%,d".format(quotation.balanceAmount.toLong())}</w:t></w:r></w:p>
    <w:p><w:r><w:rPr><w:b/><w:sz w:val="24"/></w:rPr><w:t>Payment Terms &amp; Conditions:</w:t></w:r></w:p>
    <w:p><w:r><w:t>${quotation.terms}</w:t></w:r></w:p>
    <w:p><w:r><w:t>__________________________________________________________________________</w:t></w:r></w:p>
    <w:p>
      <w:pPr><w:jc w:val="right"/></w:pPr>
      <w:r><w:rPr><w:b/></w:rPr><w:t>For $studioName</w:t></w:r>
    </w:p>
    <w:p>
      <w:pPr><w:jc w:val="right"/></w:pPr>
      <w:r><w:t>Authorized Signatory</w:t></w:r>
    </w:p>
  </w:body>
</w:document>"""

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            writeZipEntry(zos, "[Content_Types].xml", contentTypes)
            writeZipEntry(zos, "_rels/.rels", packageRels)
            writeZipEntry(zos, "word/document.xml", documentXml)
        }
        return file
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        val entry = ZipEntry(entryName)
        zos.putNextEntry(entry)
        val bytes = content.toByteArray(StandardCharsets.UTF_8)
        zos.write(bytes, 0, bytes.size)
        zos.closeEntry()
    }

    fun openFile(context: Context, file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            shareFile(context, file, mimeType, "Open ${file.name}")
        }
    }

    fun shareFile(context: Context, file: File, mimeType: String, title: String) {
        try {
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(intent, "Share via").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (_: Exception) {}
    }

    fun printPdf(context: Context, file: File, jobName: String) {
        openFile(context, file, "application/pdf")
    }

    fun generateFinancialYearPdf(
        context: Context,
        workspace: WorkspaceEntity?,
        selectedFy: String,
        grossRevenue: Double,
        annualWages: Double,
        directExpenses: Double,
        overheadExpenses: Double,
        netProfit: Double,
        profitMargin: Int,
        expenses: List<ExpenseEntity>,
        employees: List<EmployeeEntity>
    ): File {
        val studioName = workspace?.name ?: "Shakir Photography & Cinema"
        val studioGst = workspace?.gstNumber ?: "27AAACL1234F1Z8"

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint().apply { isAntiAlias = true }

        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Header Banner
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRect(0f, 0f, 595f, 105f, paint)

        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(studioName, 40f, 40f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawText("GSTIN: $studioGst • Official P&L Statement", 40f, 60f, paint)
        canvas.drawText("Report: Financial Year Analytics ($selectedFy)", 40f, 78f, paint)

        // Accent Line
        paint.color = Color.rgb(37, 99, 235)
        paint.strokeWidth = 3f
        canvas.drawLine(40f, 105f, 555f, 105f, paint)
        paint.strokeWidth = 1f

        var currentY = 135f

        // Performance Summary Banner
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(40f, currentY, 555f, currentY + 80f, 10f, 10f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("EXECUTIVE FINANCIAL SUMMARY — $selectedFy", 55f, currentY + 22f, paint)

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 10f
        paint.color = Color.rgb(71, 85, 105)
        canvas.drawText("Gross Revenue (Collected): ₹${"%,d".format(grossRevenue.toLong())}", 55f, currentY + 42f, paint)
        canvas.drawText("Total Costs (Wages + Expenses): ₹${"%,d".format((annualWages + directExpenses + overheadExpenses).toLong())}", 55f, currentY + 60f, paint)

        paint.color = if (netProfit >= 0) Color.rgb(16, 185, 129) else Color.rgb(239, 68, 68)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 12f
        canvas.drawText("Net Profit: ₹${"%,d".format(netProfit.toLong())} ($profitMargin% margin)", 330f, currentY + 42f, paint)
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Reconciled with SQLite Ledger", 330f, currentY + 60f, paint)

        currentY += 105f

        // Section: Breakdown of Expenditure
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("P&L COST LEDGER BREAKDOWN", 40f, currentY, paint)
        currentY += 12f

        // Table Header
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRect(40f, currentY, 555f, currentY + 24f, paint)

        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Category", 50f, currentY + 16f, paint)
        canvas.drawText("Description", 180f, currentY + 16f, paint)
        canvas.drawText("Amount (INR)", 450f, currentY + 16f, paint)
        currentY += 24f

        val lineItems = listOf(
            Triple("Gross Revenue", "Total Realized Client Payments", grossRevenue),
            Triple("Staff Wages", "Annualized Studio Team Salaries", annualWages),
            Triple("Direct Expenses", "Shoot Travel, Equipment, Petrol, Printing", directExpenses),
            Triple("Studio Overheads", "Office Rent, Software Subscriptions, Utilities", overheadExpenses),
            Triple("Net Profit", "EBITDA Equivalent Bottom-Line", netProfit)
        )

        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9f

        for (item in lineItems) {
            paint.color = Color.WHITE
            canvas.drawRect(40f, currentY, 555f, currentY + 24f, paint)
            paint.color = Color.rgb(226, 232, 240)
            canvas.drawLine(40f, currentY + 24f, 555f, currentY + 24f, paint)

            paint.color = Color.rgb(30, 41, 59)
            canvas.drawText(item.first, 50f, currentY + 16f, paint)
            canvas.drawText(item.second, 180f, currentY + 16f, paint)
            canvas.drawText("₹${"%,d".format(item.third.toLong())}", 450f, currentY + 16f, paint)
            currentY += 24f
        }

        currentY += 25f

        // Team Staffing Structure
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("STAFF COMPENSATION & HEADCOUNT", 40f, currentY, paint)
        currentY += 12f

        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRect(40f, currentY, 555f, currentY + 22f, paint)
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 9f
        canvas.drawText("Employee Name", 50f, currentY + 15f, paint)
        canvas.drawText("Designation & Dept", 200f, currentY + 15f, paint)
        canvas.drawText("Monthly CTC", 450f, currentY + 15f, paint)
        currentY += 22f

        for (emp in employees.take(6)) {
            paint.color = Color.WHITE
            canvas.drawRect(40f, currentY, 555f, currentY + 22f, paint)
            paint.color = Color.rgb(226, 232, 240)
            canvas.drawLine(40f, currentY + 22f, 555f, currentY + 22f, paint)

            paint.color = Color.rgb(51, 65, 85)
            paint.textSize = 9f
            canvas.drawText(emp.name, 50f, currentY + 15f, paint)
            canvas.drawText("${emp.designation} (${emp.department})", 200f, currentY + 15f, paint)
            canvas.drawText("₹${"%,d".format(emp.basicSalary.toLong())}", 450f, currentY + 15f, paint)
            currentY += 22f
        }

        // Footer & Signature
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawLine(380f, 750f, 555f, 750f, paint)
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("For $studioName", 380f, 765f, paint)
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(100, 116, 139)
        canvas.drawText("Managing Director / Super Admin", 380f, 778f, paint)

        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        canvas.drawText("APP BOSS • Financial Intelligence Engine • Generated on ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())}", 40f, 815f, paint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "FY_Report_${selectedFy.replace(" ", "_")}.pdf")
        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            try { pdfDocument.close() } catch (_: Exception) {}
        }
        return file
    }

    fun generateOperationsReportPdf(
        context: Context,
        workspace: WorkspaceEntity?,
        leads: List<LeadEntity>,
        quotations: List<QuotationEntity>,
        bookings: List<BookingEntity>,
        periodText: String
    ): File {
        val studioName = workspace?.name ?: "Shakir Photography & Cinema"
        val studioGst = workspace?.gstNumber ?: "27AAACL1234F1Z8"

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint().apply { isAntiAlias = true }

        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Header Banner
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRect(0f, 0f, 595f, 105f, paint)

        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(studioName, 40f, 40f, paint)

        paint.textSize = 10f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawText("GSTIN: $studioGst • Commercial Intelligence", 40f, 60f, paint)
        canvas.drawText("Operations & Sales Workload Report ($periodText)", 40f, 78f, paint)

        // Accent Line
        paint.color = Color.rgb(37, 99, 235)
        paint.strokeWidth = 3f
        canvas.drawLine(40f, 105f, 555f, 105f, paint)
        paint.strokeWidth = 1f

        var currentY = 135f

        val wonLeads = leads.count { it.status == "Won" || it.isConverted }
        val conversionRate = if (leads.isNotEmpty()) ((wonLeads.toDouble() / leads.size.toDouble()) * 100).toInt() else 0
        val totalBookingValue = bookings.sumOf { it.totalAmount }

        // Key Metrics Grid
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(40f, currentY, 280f, currentY + 75f, 8f, 8f, paint)
        canvas.drawRoundRect(300f, currentY, 555f, currentY + 75f, 8f, 8f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SALES PIPELINE CONVERSION", 52f, currentY + 22f, paint)
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 13f
        canvas.drawText("$conversionRate% ($wonLeads won / ${leads.size} inquiries)", 52f, currentY + 44f, paint)
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Total Active Inquiries: ${leads.size}", 52f, currentY + 62f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("CONFIRMED BOOKINGS VALUE", 312f, currentY + 22f, paint)
        paint.color = Color.rgb(37, 99, 235)
        paint.textSize = 13f
        canvas.drawText("₹${"%,d".format(totalBookingValue.toLong())}", 312f, currentY + 44f, paint)
        paint.color = Color.rgb(100, 116, 139)
        paint.textSize = 9f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("${bookings.size} Confirmed Production Projects", 312f, currentY + 62f, paint)

        currentY += 95f

        // Active Quotations Section
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("RECENT PROPOSALS & QUOTATION STATUS", 40f, currentY, paint)
        currentY += 12f

        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRect(40f, currentY, 555f, currentY + 22f, paint)
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 9f
        canvas.drawText("Quote #", 50f, currentY + 15f, paint)
        canvas.drawText("Client Name", 140f, currentY + 15f, paint)
        canvas.drawText("Event Date", 290f, currentY + 15f, paint)
        canvas.drawText("Status", 400f, currentY + 15f, paint)
        canvas.drawText("Total (INR)", 480f, currentY + 15f, paint)
        currentY += 22f

        for (q in quotations.take(8)) {
            paint.color = Color.WHITE
            canvas.drawRect(40f, currentY, 555f, currentY + 22f, paint)
            paint.color = Color.rgb(226, 232, 240)
            canvas.drawLine(40f, currentY + 22f, 555f, currentY + 22f, paint)

            paint.color = Color.rgb(51, 65, 85)
            paint.textSize = 9f
            canvas.drawText(q.quotationNumber, 50f, currentY + 15f, paint)
            canvas.drawText(q.clientName.take(20), 140f, currentY + 15f, paint)
            canvas.drawText(q.eventDate, 290f, currentY + 15f, paint)
            canvas.drawText(q.status, 400f, currentY + 15f, paint)
            canvas.drawText("₹${"%,d".format(q.totalAmount.toLong())}", 480f, currentY + 15f, paint)
            currentY += 22f
        }

        // Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        canvas.drawText("APP BOSS • Operations & Sales Audit • Generated on ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())}", 40f, 815f, paint)

        pdfDocument.finishPage(page)

        val file = File(context.cacheDir, "Operations_Report_${periodText.replace(" ", "_")}.pdf")
        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            try { pdfDocument.close() } catch (_: Exception) {}
        }
        return file
    }

    fun saveFileToDownloads(
        context: Context,
        file: File,
        mimeType: String,
        displayName: String
    ): File {
        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
        val destDir = if (downloadsDir != null && (downloadsDir.exists() || downloadsDir.mkdirs())) {
            downloadsDir
        } else {
            context.getExternalFilesDir(android.os.Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir
        }
        val targetFile = File(destDir, displayName)
        file.inputStream().use { input ->
            targetFile.outputStream().use { output ->
                input.copyTo(output)
            }
        }
        return targetFile
    }

    fun generateSalarySlipPdf(
        context: Context,
        record: PayrollRecordEntity,
        employee: EmployeeEntity?,
        workspace: WorkspaceEntity?
    ): File {
        val studioName = workspace?.name ?: "Shakir Photography & Cinema"
        val studioGst = workspace?.gstNumber ?: "27AAACL1234F1Z8"
        val studioPhone = workspace?.phone ?: "+91 98234 56789"
        val studioAddress = workspace?.address ?: "Penthouse 4B, Sky Vista Tower, Viman Nagar, Pune"
        val empName = employee?.name ?: record.employeeName
        val empCode = employee?.employeeCode ?: "EMP-${record.employeeId.takeLast(4).uppercase()}"
        val designation = employee?.designation ?: "Team Member"
        val department = employee?.department ?: "Production"

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint().apply { isAntiAlias = true }

        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Header Background Banner
        paint.color = Color.rgb(15, 23, 42) // Dark Slate
        canvas.drawRect(0f, 0f, 595f, 105f, paint)

        // Studio Branding
        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(studioName, 40f, 42f, paint)

        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawText("$studioAddress • Phone: $studioPhone", 40f, 62f, paint)
        canvas.drawText("GSTIN: $studioGst • Business Operational Studio System (BOSS)", 40f, 78f, paint)

        // Salary Slip Badge
        paint.color = Color.rgb(16, 185, 129) // Emerald-500
        canvas.drawRoundRect(405f, 22f, 555f, 82f, 8f, 8f, paint)
        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("SALARY SLIP", 435f, 44f, paint)
        paint.textSize = 9.5f
        canvas.drawText(record.monthYear.uppercase(), 438f, 64f, paint)

        var currentY = 125f

        // Employee Info Box (Two column)
        paint.color = Color.rgb(241, 245, 249) // Slate-100
        canvas.drawRoundRect(40f, currentY, 285f, currentY + 80f, 8f, 8f, paint)
        canvas.drawRoundRect(300f, currentY, 555f, currentY + 80f, 8f, 8f, paint)

        // Left: Employee Profile
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("EMPLOYEE DETAILS", 52f, currentY + 18f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 12f
        canvas.drawText(empName, 52f, currentY + 36f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Code: $empCode • Dept: $department", 52f, currentY + 52f, paint)
        canvas.drawText("Designation: $designation", 52f, currentY + 68f, paint)

        // Right: Policy & Basis
        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("PAYROLL POLICY & RECORD ID", 312f, currentY + 18f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 10f
        val basisStr = if (record.calculationBasis == "SCHEDULED_WORKING_DAYS") "Scheduled Days (${record.scheduledWorkingDays} days)" else "Calendar Days (${record.totalCalendarDays} days)"
        canvas.drawText("Basis: $basisStr", 312f, currentY + 36f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Standard: 9.0 hrs/day • Status: ${record.status}", 312f, currentY + 52f, paint)
        canvas.drawText("Slip Ref: ${record.id}", 312f, currentY + 68f, paint)

        currentY += 95f

        // Attendance & Working Hours Summary Bar
        paint.color = Color.rgb(238, 242, 255) // Indigo-50
        canvas.drawRoundRect(40f, currentY, 555f, currentY + 52f, 8f, 8f, paint)
        paint.color = Color.rgb(67, 56, 202) // Indigo-700
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("ATTENDANCE & WORKING HOURS SUMMARY", 52f, currentY + 16f, paint)

        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT
        val attSummary = "Present: ${record.presentDays}d • Half-Day: ${record.halfDays}d • Leave: ${record.paidLeaveDays}d • Weekly Off: ${record.weeklyOffDays}d • Holiday: ${record.holidayDays}d • Absent: ${record.unpaidLeaveDays}d"
        canvas.drawText(attSummary, 52f, currentY + 32f, paint)

        val hoursSummary = "Actual Hours: ${record.totalWorkingHours} hrs • Payable Days: ${record.payableDays}d • Daily Rate: ₹${"%,.2f".format(record.dailyRate)}"
        canvas.drawText(hoursSummary, 52f, currentY + 45f, paint)

        currentY += 68f

        // Table Header (Earnings on left, Deductions on right)
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRect(40f, currentY, 285f, currentY + 22f, paint)
        canvas.drawRect(300f, currentY, 555f, currentY + 22f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("EARNINGS & ALLOWANCES", 50f, currentY + 15f, paint)
        canvas.drawText("AMOUNT (INR)", 205f, currentY + 15f, paint)

        canvas.drawText("DEDUCTIONS & ADJUSTMENTS", 310f, currentY + 15f, paint)
        canvas.drawText("AMOUNT (INR)", 475f, currentY + 15f, paint)

        currentY += 22f

        // Row 1: Basic Monthly CTC
        paint.color = Color.rgb(255, 255, 255)
        canvas.drawRect(40f, currentY, 555f, currentY + 22f, paint)
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawLine(40f, currentY + 22f, 555f, currentY + 22f, paint)

        paint.color = Color.rgb(51, 65, 85)
        paint.typeface = Typeface.DEFAULT
        paint.textSize = 9.5f
        canvas.drawText("Base Monthly Salary", 50f, currentY + 15f, paint)
        canvas.drawText("₹${"%,d".format(record.baseMonthlySalary.toLong())}", 205f, currentY + 15f, paint)

        val dedLabel = if (record.deductionReason.isNotBlank()) "Deduction (${record.deductionReason.take(18)})" else "Authorized Deductions"
        canvas.drawText(dedLabel, 310f, currentY + 15f, paint)
        canvas.drawText("₹${"%,d".format(record.deductions.toLong())}", 475f, currentY + 15f, paint)

        currentY += 22f

        // Row 2: Regular Earned Salary
        paint.color = Color.rgb(255, 255, 255)
        canvas.drawRect(40f, currentY, 555f, currentY + 22f, paint)
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawLine(40f, currentY + 22f, 555f, currentY + 22f, paint)

        val earnedSalary = maxOf(0.0, record.grossPayable - record.overtimeAmount)
        canvas.drawText("Regular Earned Salary", 50f, currentY + 15f, paint)
        canvas.drawText("₹${"%,d".format(earnedSalary.toLong())}", 205f, currentY + 15f, paint)

        canvas.drawText("TDS / Tax Deductions", 310f, currentY + 15f, paint)
        canvas.drawText("₹0", 475f, currentY + 15f, paint)

        currentY += 22f

        // Row 3: Overtime
        paint.color = Color.rgb(255, 255, 255)
        canvas.drawRect(40f, currentY, 555f, currentY + 22f, paint)
        paint.color = Color.rgb(241, 245, 249)
        canvas.drawLine(40f, currentY + 22f, 555f, currentY + 22f, paint)

        val otLabel = if (record.overtimeHours > 0) "Overtime (${record.overtimeHours}h @ 1.5x)" else "Overtime Pay"
        canvas.drawText(otLabel, 50f, currentY + 15f, paint)
        canvas.drawText("₹${"%,d".format(record.overtimeAmount.toLong())}", 205f, currentY + 15f, paint)

        canvas.drawText("Other Deductions", 310f, currentY + 15f, paint)
        canvas.drawText("₹0", 475f, currentY + 15f, paint)

        currentY += 22f

        // Row 4: Totals Row
        paint.color = Color.rgb(248, 250, 252)
        canvas.drawRect(40f, currentY, 285f, currentY + 24f, paint)
        canvas.drawRect(300f, currentY, 555f, currentY + 24f, paint)

        paint.color = Color.rgb(15, 23, 42)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textSize = 9.5f
        canvas.drawText("Gross Earnings", 50f, currentY + 16f, paint)
        canvas.drawText("₹${"%,d".format(record.grossPayable.toLong())}", 205f, currentY + 16f, paint)

        canvas.drawText("Total Deductions", 310f, currentY + 16f, paint)
        canvas.drawText("₹${"%,d".format(record.deductions.toLong())}", 475f, currentY + 16f, paint)

        currentY += 36f

        // Net Salary Box (Prominent)
        paint.color = Color.rgb(16, 185, 129) // Emerald-500
        canvas.drawRoundRect(40f, currentY, 555f, currentY + 54f, 8f, 8f, paint)

        paint.color = Color.WHITE
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("NET PAYABLE SALARY", 55f, currentY + 24f, paint)
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Mode: Direct Bank Transfer • Currency: INR (₹)", 55f, currentY + 42f, paint)

        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val netStr = "₹${"%,.2f".format(record.netSalary)}"
        canvas.drawText(netStr, 380f, currentY + 34f, paint)

        currentY += 72f

        // Calculation Notes / Audit breakdown
        if (record.calculationNotes.isNotBlank()) {
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawRoundRect(40f, currentY, 555f, currentY + 68f, 6f, 6f, paint)
            paint.color = Color.rgb(71, 85, 105)
            paint.textSize = 8.5f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("AUDIT & CALCULATION NOTES:", 52f, currentY + 16f, paint)
            paint.typeface = Typeface.DEFAULT
            paint.textSize = 8f
            val cleanNotes = record.calculationNotes.lines().take(4)
            var noteY = currentY + 30f
            for (line in cleanNotes) {
                canvas.drawText(line.take(85), 52f, noteY, paint)
                noteY += 12f
            }
            currentY += 80f
        }

        // Signatures
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Employee Signature: __________________", 50f, 780f, paint)
        canvas.drawText("Authorized Signatory: __________________", 330f, 780f, paint)

        // Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("Generated via APP BOSS • Confirmed Studio Payroll Record • ${record.approvalDate.ifBlank { "System Verified" }}", 40f, 815f, paint)

        pdfDocument.finishPage(page)

        val cleanEmpName = empName.replace(" ", "_").replace("/", "_")
        val cleanMonth = record.monthYear.replace(" ", "_").replace("/", "_")
        val file = File(context.cacheDir, "SalarySlip_${cleanEmpName}_${cleanMonth}.pdf")
        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            try { pdfDocument.close() } catch (_: Exception) {}
        }
        return file
    }

    fun generateSalarySlipXlsx(
        context: Context,
        record: PayrollRecordEntity,
        employee: EmployeeEntity?,
        workspace: WorkspaceEntity?
    ): File {
        val studioName = workspace?.name ?: "Shakir Photography & Cinema"
        val empName = employee?.name ?: record.employeeName
        val cleanEmpName = empName.replace(" ", "_").replace("/", "_")
        val cleanMonth = record.monthYear.replace(" ", "_").replace("/", "_")
        val file = File(context.cacheDir, "SalarySlip_${cleanEmpName}_${cleanMonth}.xlsx")

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

        val packageRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Salary Slip" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

        val workbookRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""

        val earnedSalary = maxOf(0.0, record.grossPayable - record.overtimeAmount)

        val sheet1Xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
    <row r="1">
      <c r="A1" t="inlineStr"><is><t>$studioName - EMPLOYEE SALARY SLIP</t></is></c>
    </row>
    <row r="2">
      <c r="A2" t="inlineStr"><is><t>Payroll Month: ${record.monthYear}</t></is></c>
      <c r="B2" t="inlineStr"><is><t>Status: ${record.status}</t></is></c>
    </row>
    <row r="3">
      <c r="A3" t="inlineStr"><is><t>Employee Name: $empName</t></is></c>
      <c r="B3" t="inlineStr"><is><t>Employee Code: ${employee?.employeeCode ?: record.employeeId}</t></is></c>
    </row>
    <row r="4">
      <c r="A4" t="inlineStr"><is><t>Designation: ${employee?.designation ?: "Staff"}</t></is></c>
      <c r="B4" t="inlineStr"><is><t>Department: ${employee?.department ?: "Production"}</t></is></c>
    </row>
    <row r="5">
      <c r="A5" t="inlineStr"><is><t>Calculation Basis: ${record.calculationBasis}</t></is></c>
      <c r="B5" t="inlineStr"><is><t>Scheduled Working Days: ${record.scheduledWorkingDays}</t></is></c>
    </row>
    <row r="6"/>
    <row r="7">
      <c r="A7" t="inlineStr"><is><t>Attendance Parameter</t></is></c>
      <c r="B7" t="inlineStr"><is><t>Count / Value</t></is></c>
    </row>
    <row r="8">
      <c r="A8" t="inlineStr"><is><t>Present Days</t></is></c>
      <c r="B8"><v>${record.presentDays}</v></c>
    </row>
    <row r="9">
      <c r="A9" t="inlineStr"><is><t>Half Days</t></is></c>
      <c r="B9"><v>${record.halfDays}</v></c>
    </row>
    <row r="10">
      <c r="A10" t="inlineStr"><is><t>Approved Leaves</t></is></c>
      <c r="B10"><v>${record.paidLeaveDays}</v></c>
    </row>
    <row r="11">
      <c r="A11" t="inlineStr"><is><t>Weekly Offs</t></is></c>
      <c r="B11"><v>${record.weeklyOffDays}</v></c>
    </row>
    <row r="12">
      <c r="A12" t="inlineStr"><is><t>Holidays</t></is></c>
      <c r="B12"><v>${record.holidayDays}</v></c>
    </row>
    <row r="13">
      <c r="A13" t="inlineStr"><is><t>Absent Days</t></is></c>
      <c r="B13"><v>${record.unpaidLeaveDays}</v></c>
    </row>
    <row r="14">
      <c r="A14" t="inlineStr"><is><t>Total Working Hours</t></is></c>
      <c r="B14"><v>${record.totalWorkingHours}</v></c>
    </row>
    <row r="15">
      <c r="A15" t="inlineStr"><is><t>Daily Rate (INR)</t></is></c>
      <c r="B15"><v>${record.dailyRate.toLong()}</v></c>
    </row>
    <row r="16"/>
    <row r="18">
      <c r="A18" t="inlineStr"><is><t>Salary Component</t></is></c>
      <c r="B18" t="inlineStr"><is><t>Amount (INR)</t></is></c>
    </row>
    <row r="19">
      <c r="A19" t="inlineStr"><is><t>Monthly Base CTC</t></is></c>
      <c r="B19"><v>${record.baseMonthlySalary.toLong()}</v></c>
    </row>
    <row r="20">
      <c r="A20" t="inlineStr"><is><t>Regular Earned Salary</t></is></c>
      <c r="B20"><v>${earnedSalary.toLong()}</v></c>
    </row>
    <row r="21">
      <c r="A21" t="inlineStr"><is><t>Overtime Amount (${record.overtimeHours} hrs)</t></is></c>
      <c r="B21"><v>${record.overtimeAmount.toLong()}</v></c>
    </row>
    <row r="22">
      <c r="A22" t="inlineStr"><is><t>Gross Payable</t></is></c>
      <c r="B22"><v>${record.grossPayable.toLong()}</v></c>
    </row>
    <row r="23">
      <c r="A23" t="inlineStr"><is><t>Deductions (${record.deductionReason.ifBlank { "Standard" }})</t></is></c>
      <c r="B23"><v>${record.deductions.toLong()}</v></c>
    </row>
    <row r="24">
      <c r="A24" t="inlineStr"><is><t>Net Payable Salary</t></is></c>
      <c r="B24"><v>${record.netSalary.toLong()}</v></c>
    </row>
  </sheetData>
</worksheet>"""

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            writeZipEntry(zos, "[Content_Types].xml", contentTypes)
            writeZipEntry(zos, "_rels/.rels", packageRels)
            writeZipEntry(zos, "xl/workbook.xml", workbookXml)
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", workbookRels)
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", sheet1Xml)
        }
        return file
    }

    fun generateSalarySlipDocx(
        context: Context,
        record: PayrollRecordEntity,
        employee: EmployeeEntity?,
        workspace: WorkspaceEntity?
    ): File {
        val studioName = workspace?.name ?: "Shakir Photography & Cinema"
        val empName = employee?.name ?: record.employeeName
        val cleanEmpName = empName.replace(" ", "_").replace("/", "_")
        val cleanMonth = record.monthYear.replace(" ", "_").replace("/", "_")
        val file = File(context.cacheDir, "SalarySlip_${cleanEmpName}_${cleanMonth}.docx")

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

        val packageRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        val earnedSalary = maxOf(0.0, record.grossPayable - record.overtimeAmount)

        val documentXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p>
      <w:pPr><w:jc w:val="center"/></w:pPr>
      <w:r><w:rPr><w:b/><w:sz w:val="36"/></w:rPr><w:t>$studioName</w:t></w:r>
    </w:p>
    <w:p>
      <w:pPr><w:jc w:val="center"/></w:pPr>
      <w:r><w:rPr><w:sz w:val="22"/><w:b/></w:rPr><w:t>SALARY SLIP - ${record.monthYear.uppercase()}</w:t></w:r>
    </w:p>
    <w:p><w:r><w:t>__________________________________________________________________________</w:t></w:r></w:p>
    <w:p>
      <w:r><w:rPr><w:b/></w:rPr><w:t>Employee Name: </w:t></w:r>
      <w:r><w:t>$empName (Code: ${employee?.employeeCode ?: record.employeeId})</w:t></w:r>
    </w:p>
    <w:p>
      <w:r><w:rPr><w:b/></w:rPr><w:t>Designation &amp; Department: </w:t></w:r>
      <w:r><w:t>${employee?.designation ?: "Staff"} • ${employee?.department ?: "Production"}</w:t></w:r>
    </w:p>
    <w:p>
      <w:r><w:rPr><w:b/></w:rPr><w:t>Calculation Basis: </w:t></w:r>
      <w:r><w:t>${record.calculationBasis} (${record.scheduledWorkingDays} Scheduled Days, 9.0h/day)</w:t></w:r>
    </w:p>
    <w:p><w:r><w:rPr><w:b/><w:sz w:val="24"/></w:rPr><w:t>Attendance &amp; Hours Summary:</w:t></w:r></w:p>
    <w:p><w:r><w:t>• Present Days: ${record.presentDays} | Half Days: ${record.halfDays} | Absent: ${record.unpaidLeaveDays}</w:t></w:r></w:p>
    <w:p><w:r><w:t>• Approved Leaves: ${record.paidLeaveDays} | Weekly Offs: ${record.weeklyOffDays} | Holidays: ${record.holidayDays}</w:t></w:r></w:p>
    <w:p><w:r><w:t>• Actual Working Hours: ${record.totalWorkingHours} hrs | Payable Days: ${record.payableDays} days</w:t></w:r></w:p>
    <w:p><w:r><w:t>• Daily Rate: INR ${"%,.2f".format(record.dailyRate)}</w:t></w:r></w:p>
    <w:p><w:r><w:rPr><w:b/><w:sz w:val="24"/></w:rPr><w:t>Earnings &amp; Compensation Breakdown:</w:t></w:r></w:p>
    <w:p><w:r><w:t>• Monthly Base CTC: INR ${"%,d".format(record.baseMonthlySalary.toLong())}</w:t></w:r></w:p>
    <w:p><w:r><w:t>• Regular Earned Salary: INR ${"%,d".format(earnedSalary.toLong())}</w:t></w:r></w:p>
    <w:p><w:r><w:t>• Overtime Pay (${record.overtimeHours} hrs @ 1.5x): INR ${"%,d".format(record.overtimeAmount.toLong())}</w:t></w:r></w:p>
    <w:p><w:r><w:rPr><w:b/></w:rPr><w:t>• Gross Earnings: INR ${"%,d".format(record.grossPayable.toLong())}</w:t></w:r></w:p>
    <w:p><w:r><w:t>• Deductions (${record.deductionReason.ifBlank { "None" }}): -INR ${"%,d".format(record.deductions.toLong())}</w:t></w:r></w:p>
    <w:p><w:r><w:rPr><w:b/><w:sz w:val="26"/></w:rPr><w:t>• NET PAYABLE SALARY: INR ${"%,.2f".format(record.netSalary)}</w:t></w:r></w:p>
    <w:p><w:r><w:t>Status: ${record.status} • Approved by: ${record.approvedBy.ifBlank { "Management" }}</w:t></w:r></w:p>
    <w:p><w:r><w:t>__________________________________________________________________________</w:t></w:r></w:p>
    <w:p>
      <w:pPr><w:jc w:val="right"/></w:pPr>
      <w:r><w:rPr><w:b/></w:rPr><w:t>For $studioName</w:t></w:r>
    </w:p>
    <w:p>
      <w:pPr><w:jc w:val="right"/></w:pPr>
      <w:r><w:t>Authorized Signatory</w:t></w:r>
    </w:p>
  </w:body>
</w:document>"""

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            writeZipEntry(zos, "[Content_Types].xml", contentTypes)
            writeZipEntry(zos, "_rels/.rels", packageRels)
            writeZipEntry(zos, "word/document.xml", documentXml)
        }
        return file
    }

    fun generateMonthlyPayrollSummaryPdf(
        context: Context,
        records: List<PayrollRecordEntity>,
        monthYear: String,
        workspace: WorkspaceEntity?
    ): File {
        val studioName = workspace?.name ?: "Shakir Photography & Cinema"
        val studioGst = workspace?.gstNumber ?: "27AAACL1234F1Z8"
        val studioPhone = workspace?.phone ?: "+91 98234 56789"

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        val paint = Paint().apply { isAntiAlias = true }

        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Header Background
        paint.color = Color.rgb(15, 23, 42)
        canvas.drawRect(0f, 0f, 595f, 100f, paint)

        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(studioName, 40f, 40f, paint)

        paint.textSize = 9.5f
        paint.typeface = Typeface.DEFAULT
        paint.color = Color.rgb(203, 213, 225)
        canvas.drawText("Studio Compensation & Wage Master Sheet • Month: $monthYear", 40f, 60f, paint)
        canvas.drawText("GSTIN: $studioGst • Phone: $studioPhone", 40f, 76f, paint)

        // Badge
        paint.color = Color.rgb(37, 99, 235)
        canvas.drawRoundRect(415f, 24f, 555f, 76f, 8f, 8f, paint)
        paint.color = Color.WHITE
        paint.textSize = 10f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("MONTHLY MASTER", 432f, 44f, paint)
        paint.textSize = 9f
        canvas.drawText("${records.size} Staff Members", 442f, 62f, paint)

        var currentY = 120f

        // Aggregate Totals Card
        val totalGross = records.sumOf { it.grossPayable }
        val totalNet = records.sumOf { it.netSalary }
        val totalDeductions = records.sumOf { it.deductions }
        val totalHours = records.sumOf { it.totalWorkingHours }

        paint.color = Color.rgb(241, 245, 249)
        canvas.drawRoundRect(40f, currentY, 555f, currentY + 54f, 8f, 8f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL GROSS PAYABLE", 55f, currentY + 18f, paint)
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 12f
        canvas.drawText("₹${"%,d".format(totalGross.toLong())}", 55f, currentY + 38f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8.5f
        canvas.drawText("TOTAL DEDUCTIONS", 195f, currentY + 18f, paint)
        paint.color = Color.rgb(239, 68, 68)
        paint.textSize = 12f
        canvas.drawText("-₹${"%,d".format(totalDeductions.toLong())}", 195f, currentY + 38f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8.5f
        canvas.drawText("NET WAGE BILL", 325f, currentY + 18f, paint)
        paint.color = Color.rgb(16, 185, 129)
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("₹${"%,d".format(totalNet.toLong())}", 325f, currentY + 38f, paint)

        paint.color = Color.rgb(71, 85, 105)
        paint.textSize = 8.5f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("TOTAL HOURS", 460f, currentY + 18f, paint)
        paint.color = Color.rgb(15, 23, 42)
        paint.textSize = 11f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("${totalHours.toInt()} hrs", 460f, currentY + 38f, paint)

        currentY += 70f

        // Table Header
        paint.color = Color.rgb(226, 232, 240)
        canvas.drawRect(40f, currentY, 555f, currentY + 22f, paint)
        paint.color = Color.rgb(30, 41, 59)
        paint.textSize = 8.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("Employee Name", 50f, currentY + 15f, paint)
        canvas.drawText("Basis", 170f, currentY + 15f, paint)
        canvas.drawText("Days / Hrs", 245f, currentY + 15f, paint)
        canvas.drawText("Gross (INR)", 340f, currentY + 15f, paint)
        canvas.drawText("Ded (INR)", 415f, currentY + 15f, paint)
        canvas.drawText("Net (INR)", 475f, currentY + 15f, paint)
        canvas.drawText("Status", 525f, currentY + 15f, paint)

        currentY += 22f

        for (r in records) {
            paint.color = Color.WHITE
            canvas.drawRect(40f, currentY, 555f, currentY + 22f, paint)
            paint.color = Color.rgb(241, 245, 249)
            canvas.drawLine(40f, currentY + 22f, 555f, currentY + 22f, paint)

            paint.color = Color.rgb(51, 65, 85)
            paint.textSize = 8.5f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText(r.employeeName.take(18), 50f, currentY + 15f, paint)
            canvas.drawText(if (r.calculationBasis == "SCHEDULED_WORKING_DAYS") "Sched" else "Cal", 170f, currentY + 15f, paint)
            canvas.drawText("${r.payableDays}d / ${r.totalWorkingHours.toInt()}h", 245f, currentY + 15f, paint)
            canvas.drawText("₹${"%,d".format(r.grossPayable.toLong())}", 340f, currentY + 15f, paint)
            canvas.drawText("₹${"%,d".format(r.deductions.toLong())}", 415f, currentY + 15f, paint)

            paint.color = Color.rgb(16, 185, 129)
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("₹${"%,d".format(r.netSalary.toLong())}", 475f, currentY + 15f, paint)

            paint.color = when (r.status) {
                "Paid" -> Color.rgb(16, 185, 129)
                "Approved" -> Color.rgb(37, 99, 235)
                else -> Color.rgb(100, 116, 139)
            }
            canvas.drawText(r.status.take(6), 525f, currentY + 15f, paint)

            currentY += 22f
        }

        // Footer
        paint.color = Color.rgb(148, 163, 184)
        paint.textSize = 8f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("APP BOSS Studio Payroll Master • ${records.size} Records • Generated on ${java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date())}", 40f, 815f, paint)

        pdfDocument.finishPage(page)

        val cleanMonth = monthYear.replace(" ", "_").replace("/", "_")
        val file = File(context.cacheDir, "Monthly_Payroll_Summary_${cleanMonth}.pdf")
        try {
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }
        } finally {
            try { pdfDocument.close() } catch (_: Exception) {}
        }
        return file
    }

    fun generateMonthlyPayrollSummaryXlsx(
        context: Context,
        records: List<PayrollRecordEntity>,
        monthYear: String,
        workspace: WorkspaceEntity?
    ): File {
        val studioName = workspace?.name ?: "Shakir Photography & Cinema"
        val cleanMonth = monthYear.replace(" ", "_").replace("/", "_")
        val file = File(context.cacheDir, "Monthly_Payroll_Summary_${cleanMonth}.xlsx")

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""

        val packageRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

        val workbookXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Payroll Summary" sheetId="1" r:id="rId1"/>
  </sheets>
</workbook>"""

        val workbookRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
</Relationships>"""

        val rowsXml = StringBuilder()
        rowsXml.append("""
    <row r="1">
      <c r="A1" t="inlineStr"><is><t>$studioName - MONTHLY PAYROLL MASTER ($monthYear)</t></is></c>
    </row>
    <row r="2">
      <c r="A2" t="inlineStr"><is><t>Employee Name</t></is></c>
      <c r="B2" t="inlineStr"><is><t>Base Salary</t></is></c>
      <c r="C2" t="inlineStr"><is><t>Basis</t></is></c>
      <c r="D2" t="inlineStr"><is><t>Payable Days</t></is></c>
      <c r="E2" t="inlineStr"><is><t>Working Hours</t></is></c>
      <c r="F2" t="inlineStr"><is><t>Gross Salary</t></is></c>
      <c r="G2" t="inlineStr"><is><t>Deductions</t></is></c>
      <c r="H2" t="inlineStr"><is><t>Net Salary</t></is></c>
      <c r="I2" t="inlineStr"><is><t>Status</t></is></c>
    </row>""")

        var rowIdx = 3
        for (r in records) {
            rowsXml.append("""
    <row r="$rowIdx">
      <c r="A$rowIdx" t="inlineStr"><is><t>${r.employeeName}</t></is></c>
      <c r="B$rowIdx"><v>${r.baseMonthlySalary.toLong()}</v></c>
      <c r="C$rowIdx" t="inlineStr"><is><t>${r.calculationBasis}</t></is></c>
      <c r="D$rowIdx"><v>${r.payableDays}</v></c>
      <c r="E$rowIdx"><v>${r.totalWorkingHours}</v></c>
      <c r="F$rowIdx"><v>${r.grossPayable.toLong()}</v></c>
      <c r="G$rowIdx"><v>${r.deductions.toLong()}</v></c>
      <c r="H$rowIdx"><v>${r.netSalary.toLong()}</v></c>
      <c r="I$rowIdx" t="inlineStr"><is><t>${r.status}</t></is></c>
    </row>""")
            rowIdx++
        }

        val totalNet = records.sumOf { it.netSalary }
        val totalGross = records.sumOf { it.grossPayable }
        val totalDed = records.sumOf { it.deductions }

        rowsXml.append("""
    <row r="$rowIdx">
      <c r="A$rowIdx" t="inlineStr"><is><t>TOTALS</t></is></c>
      <c r="F$rowIdx"><v>${totalGross.toLong()}</v></c>
      <c r="G$rowIdx"><v>${totalDed.toLong()}</v></c>
      <c r="H$rowIdx"><v>${totalNet.toLong()}</v></c>
    </row>""")

        val sheet1Xml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <sheetData>
$rowsXml
  </sheetData>
</worksheet>"""

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            writeZipEntry(zos, "[Content_Types].xml", contentTypes)
            writeZipEntry(zos, "_rels/.rels", packageRels)
            writeZipEntry(zos, "xl/workbook.xml", workbookXml)
            writeZipEntry(zos, "xl/_rels/workbook.xml.rels", workbookRels)
            writeZipEntry(zos, "xl/worksheets/sheet1.xml", sheet1Xml)
        }
        return file
    }

    fun generateMonthlyPayrollSummaryDocx(
        context: Context,
        records: List<PayrollRecordEntity>,
        monthYear: String,
        workspace: WorkspaceEntity?
    ): File {
        val studioName = workspace?.name ?: "Shakir Photography & Cinema"
        val cleanMonth = monthYear.replace(" ", "_").replace("/", "_")
        val file = File(context.cacheDir, "Monthly_Payroll_Summary_${cleanMonth}.docx")

        val contentTypes = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>"""

        val packageRels = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>"""

        val totalGross = records.sumOf { it.grossPayable }
        val totalNet = records.sumOf { it.netSalary }
        val totalDed = records.sumOf { it.deductions }

        val staffParagraphs = StringBuilder()
        for (r in records) {
            staffParagraphs.append("""
    <w:p>
      <w:r><w:rPr><w:b/></w:rPr><w:t>${r.employeeName}: </w:t></w:r>
      <w:r><w:t>Base: INR ${"%,d".format(r.baseMonthlySalary.toLong())} | ${r.payableDays}d (${r.totalWorkingHours.toInt()}h) | Gross: INR ${"%,d".format(r.grossPayable.toLong())} | Ded: INR ${"%,d".format(r.deductions.toLong())} | Net: INR ${"%,d".format(r.netSalary.toLong())} [${r.status}]</w:t></w:r>
    </w:p>""")
        }

        val documentXml = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
  <w:body>
    <w:p>
      <w:pPr><w:jc w:val="center"/></w:pPr>
      <w:r><w:rPr><w:b/><w:sz w:val="36"/></w:rPr><w:t>$studioName</w:t></w:r>
    </w:p>
    <w:p>
      <w:pPr><w:jc w:val="center"/></w:pPr>
      <w:r><w:rPr><w:sz w:val="22"/><w:b/></w:rPr><w:t>MONTHLY PAYROLL STATEMENT - $monthYear</w:t></w:r>
    </w:p>
    <w:p><w:r><w:t>__________________________________________________________________________</w:t></w:r></w:p>
    <w:p>
      <w:r><w:rPr><w:b/></w:rPr><w:t>Studio Wage Summary: </w:t></w:r>
      <w:r><w:t>Total Staff: ${records.size} | Gross: INR ${"%,d".format(totalGross.toLong())} | Deductions: INR ${"%,d".format(totalDed.toLong())} | Total Net Wage Bill: INR ${"%,d".format(totalNet.toLong())}</w:t></w:r>
    </w:p>
    <w:p><w:r><w:rPr><w:b/><w:sz w:val="24"/></w:rPr><w:t>Staff Compensation Details:</w:t></w:r></w:p>
$staffParagraphs
    <w:p><w:r><w:t>__________________________________________________________________________</w:t></w:r></w:p>
    <w:p>
      <w:pPr><w:jc w:val="right"/></w:pPr>
      <w:r><w:rPr><w:b/></w:rPr><w:t>For $studioName</w:t></w:r>
    </w:p>
    <w:p>
      <w:pPr><w:jc w:val="right"/></w:pPr>
      <w:r><w:t>Authorized Signatory</w:t></w:r>
    </w:p>
  </w:body>
</w:document>"""

        ZipOutputStream(FileOutputStream(file)).use { zos ->
            writeZipEntry(zos, "[Content_Types].xml", contentTypes)
            writeZipEntry(zos, "_rels/.rels", packageRels)
            writeZipEntry(zos, "word/document.xml", documentXml)
        }
        return file
    }
}

