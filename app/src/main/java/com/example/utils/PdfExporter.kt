package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.Category
import com.example.data.Transaction
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object PdfExporter {

    fun generateMonthlyReport(
        context: Context,
        monthName: String,
        categories: List<Category>,
        transactions: List<Transaction>
    ): File? {
        val pdfDocument = PdfDocument()
        
        // A4 Paper Size: 595 x 842 points
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paintTitle = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 24f
            color = Color.parseColor("#1A237E") // Dark Indigo
        }

        val paintSubtitle = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 12f
            color = Color.GRAY
        }

        val paintHeading = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 14f
            color = Color.parseColor("#212121")
        }

        val paintText = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textSize = 10f
            color = Color.parseColor("#424242")
        }

        val paintTextBold = Paint().apply {
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textSize = 10f
            color = Color.parseColor("#212121")
        }

        val paintLine = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
        }

        var y = 50f

        // Document Header
        canvas.drawText("MONTHLY FINANCIAL REPORT", 40f, y, paintTitle)
        y += 20f
        canvas.drawText("Month: $monthName | Generated: ${SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date())}", 40f, y, paintSubtitle)
        y += 20f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 30f

        // Financial Overview Summary
        canvas.drawText("FINANCIAL OVERVIEW", 40f, y, paintHeading)
        y += 25f

        val totalIncome = transactions.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val netSavings = totalIncome - totalExpense

        canvas.drawText("Total Income:", 40f, y, paintText)
        canvas.drawText(String.format(Locale.getDefault(), "$%.2f", totalIncome), 180f, y, paintTextBold)
        y += 18f

        canvas.drawText("Total Expenses:", 40f, y, paintText)
        canvas.drawText(String.format(Locale.getDefault(), "$%.2f", totalExpense), 180f, y, paintTextBold)
        y += 18f

        canvas.drawText("Net Savings:", 40f, y, paintText)
        val netPaint = Paint(paintTextBold).apply {
            color = if (netSavings >= 0) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
        }
        canvas.drawText(String.format(Locale.getDefault(), "$%.2f", netSavings), 180f, y, netPaint)
        y += 30f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 25f

        // Expense Category Limits & Budgets
        canvas.drawText("EXPENSE CATEGORY VS LIMIT", 40f, y, paintHeading)
        y += 20f

        // Table Header
        canvas.drawText("Category Name", 40f, y, paintTextBold)
        canvas.drawText("Actual Spent", 200f, y, paintTextBold)
        canvas.drawText("Category Limit", 320f, y, paintTextBold)
        canvas.drawText("Status / Remaining", 440f, y, paintTextBold)
        y += 8f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 16f

        val expenseCategories = categories.filter { it.type == "EXPENSE" }
        for (cat in expenseCategories) {
            val spent = transactions
                .filter { it.type == "EXPENSE" && it.categoryName == cat.name }
                .sumOf { it.amount }
            val limit = cat.limitAmount ?: 0.0

            canvas.drawText(cat.name, 40f, y, paintText)
            canvas.drawText(String.format(Locale.getDefault(), "$%.2f", spent), 200f, y, paintText)
            
            if (limit > 0) {
                canvas.drawText(String.format(Locale.getDefault(), "$%.2f", limit), 320f, y, paintText)
                val remaining = limit - spent
                val statusText = if (remaining >= 0) {
                    String.format(Locale.getDefault(), "Safe ($%.2f left)", remaining)
                } else {
                    String.format(Locale.getDefault(), "Overby $%.2f", -remaining)
                }
                val statusPaint = Paint(paintTextBold).apply {
                    color = if (remaining >= 0) Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
                }
                canvas.drawText(statusText, 440f, y, statusPaint)
            } else {
                canvas.drawText("No LimitSet", 320f, y, paintSubtitle)
                canvas.drawText("-", 440f, y, paintText)
            }
            y += 15f

            // Handle page overflow if needed
            if (y > 780f) break
        }

        y += 20f
        canvas.drawLine(40f, y, 555f, y, paintLine)
        y += 25f

        // Recent Activity List
        if (y < 750f) {
            canvas.drawText("RECENT TRANSACTIONS", 40f, y, paintHeading)
            y += 20f
            
            canvas.drawText("Date", 40f, y, paintTextBold)
            canvas.drawText("Type", 110f, y, paintTextBold)
            canvas.drawText("Category", 185f, y, paintTextBold)
            canvas.drawText("Source", 295f, y, paintTextBold)
            canvas.drawText("Note", 395f, y, paintTextBold)
            canvas.drawText("Amount", 495f, y, paintTextBold)
            y += 8f
            canvas.drawLine(40f, y, 555f, y, paintLine)
            y += 16f

            val dateFormat = SimpleDateFormat("MM-dd", Locale.getDefault())
            val recentTransactions = transactions.take(15) // Limit page size to prevent overflow errors
            for (tx in recentTransactions) {
                canvas.drawText(dateFormat.format(Date(tx.date)), 40f, y, paintText)
                canvas.drawText(tx.type, 110f, y, paintText)
                canvas.drawText(tx.categoryName, 185f, y, paintText)
                canvas.drawText(tx.source, 295f, y, paintText)
                
                val truncatedNote = if (tx.note.length > 18) tx.note.substring(0, 15) + "..." else tx.note
                canvas.drawText(truncatedNote, 395f, y, paintText)

                val amtPaint = Paint(paintTextBold).apply {
                    color = if (tx.type == "INCOME") Color.parseColor("#2E7D32") else Color.parseColor("#C62828")
                }
                val prefix = if (tx.type == "INCOME") "+" else "-"
                canvas.drawText(String.format(Locale.getDefault(), "%s$%.2f", prefix, tx.amount), 495f, y, amtPaint)
                y += 14f

                if (y > 800f) break
            }
        }

        pdfDocument.finishPage(page)

        // Write file output
        val reportFile = File(context.cacheDir, "Monthly_Budget_Report_${monthName.replace(" ", "_")}.pdf")
        return try {
            val fos = FileOutputStream(reportFile)
            pdfDocument.writeTo(fos)
            pdfDocument.close()
            fos.close()
            reportFile
        } catch (e: Exception) {
            e.printStackTrace()
            pdfDocument.close()
            null
        }
    }
}
