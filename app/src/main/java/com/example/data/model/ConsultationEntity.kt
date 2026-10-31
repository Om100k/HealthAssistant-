package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "consultations")
data class ConsultationEntity(
    @PrimaryKey val id: String,
    val timestamp: Long,
    val symptomNames: String,
    val severity: String,
    val summaryText: String,
    val isEmergency: Boolean,
    val suggestedMedicineSummary: String = ""
)

