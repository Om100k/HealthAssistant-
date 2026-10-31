package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "symptoms")
data class Symptom(
    @PrimaryKey val id: String,
    val name: String,
    val category: String,
    val description: String,
    val typicalSeverity: String = "Moderate",
    val isRedFlag: Boolean = false,
    val followUpHint: String = ""
)
