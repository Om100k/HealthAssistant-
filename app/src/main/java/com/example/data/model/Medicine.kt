package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey val id: String,
    val medicineName: String,
    val genericName: String,
    val drugCategory: String,
    val commonUses: String,
    val importantWarnings: String,
    val sideEffects: String,
    val allergyInformation: String,
    val contraindications: String,
    val interactions: String,
    val prescriptionOnly: Boolean = false
)
