package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calculation_history")
data class CalculationHistory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val mode: String,          // "forward" or "backward"
    val baseHour: Int,
    val baseMin: Int,
    val duration: Int,
    val resultTimeStr: String,
    val totalBreakTime: Int,
    val totalElapsed: Int,
    val timestamp: Long = System.currentTimeMillis()
)
