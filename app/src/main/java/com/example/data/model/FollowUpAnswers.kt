package com.example.data.model

data class FollowUpAnswers(
    val duration: String = "",
    val severity: String = "",
    val onset: String = "",
    val progression: String = "",
    val associatedSymptoms: List<String> = emptyList(),
    val currentMedications: String = "",
    val existingConditions: List<String> = emptyList(),
    val ageGroup: String = ""
)
