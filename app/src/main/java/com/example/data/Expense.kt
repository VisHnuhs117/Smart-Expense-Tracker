package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val amount: Double,
    val currency: String, // "USD" or "INR"
    val category: String, // "Food", "Travel", "Bills", "Shopping", "Entertainment", "Others"
    val date: Long,       // Timestamp in milliseconds
    val notes: String? = null,
    val userEmail: String = "guest@example.com"
)
