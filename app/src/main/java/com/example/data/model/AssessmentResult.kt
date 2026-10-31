package com.example.data.model

data class SuggestedMedicine(
    val id: String = "",
    val name: String = "",
    val tabletType: String = "Oral Tablet",
    val activeIngredient: String = "",
    val purpose: String = "",
    val dosageGuidance: String = "",
    val precautions: String = "",
    val suitabilityBadge: String = "First-Line OTC Option",
    val catalogueMedicineId: String? = null
)

data class AssessmentResult(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val symptomsSummary: String = "",
    val possibleGeneralCauses: List<String> = emptyList(),
    val physiologicalExplanation: String = "",
    val warningSigns: List<String> = emptyList(),
    val allergyConcerns: List<String> = emptyList(),
    val recommendedNextSteps: List<String> = emptyList(),
    val whenToConsultProfessional: String = "",
    val suggestedMedicines: List<SuggestedMedicine> = emptyList(),
    val educationalMedicines: List<Medicine> = emptyList(),
    val isEmergencyFlagged: Boolean = false,
    val selectedSymptoms: List<Symptom> = emptyList(),
    val allergyInfo: AllergyInfo = AllergyInfo(),
    val followUpAnswers: FollowUpAnswers = FollowUpAnswers(),
    val rawAiResponse: String = ""
)

