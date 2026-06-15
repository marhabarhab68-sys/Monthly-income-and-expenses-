package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // "INCOME" or "EXPENSE"
    val name: String,
    val iconName: String, // e.g. "Restaurant", "Home", "DirectionsCar"
    val colorHex: String, // hex representation e.g. "#FF5722"
    val limitAmount: Double? = null // custom budget limit for expenses
)
