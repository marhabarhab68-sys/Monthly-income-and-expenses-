package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Double,
    val categoryName: String,
    val date: Long, // timestamp
    val note: String,
    val source: String // e.g., "Salary Source", "Cash", "Bank Account", "Credit Card"
)
