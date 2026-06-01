package com.example.util

import android.content.Context
import com.example.data.Expense
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CsvExporter {
    fun exportExpensesToCsv(context: Context, expenses: List<Expense>): File? {
        return try {
            val file = File(context.cacheDir, "expenses_export.csv")
            file.bufferedWriter().use { writer ->
                writer.write("ID,Amount,Original Currency,Category,Date,Notes\n")
                val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                expenses.forEach { expense ->
                    val dateStr = dateFormat.format(Date(expense.date))
                    val sanitizedNotes = (expense.notes ?: "")
                        .replace("\"", "\"\"")
                        .replace("\n", " ")
                    writer.write("${expense.id},${expense.amount},${expense.currency},${expense.category},\"$dateStr\",\"$sanitizedNotes\"\n")
                }
            }
            file
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
