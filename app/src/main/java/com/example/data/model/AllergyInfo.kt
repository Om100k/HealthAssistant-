package com.example.data.model

data class AllergyInfo(
    val hasNoKnownAllergies: Boolean = false,
    val selectedAllergyTypes: List<String> = emptyList(),
    val medicineName: String = "",
    val medicineReaction: String = "",
    val isUnanswered: Boolean = true
) {
    fun summaryText(): String {
        return if (isUnanswered) {
            "Not specified"
        } else if (hasNoKnownAllergies) {
            "No known allergies reported"
        } else {
            val list = selectedAllergyTypes.toMutableList()
            if (selectedAllergyTypes.contains("Medicine allergies") && medicineName.isNotBlank()) {
                val idx = list.indexOf("Medicine allergies")
                list[idx] = "Medicine allergies: $medicineName (Reaction: ${medicineReaction.ifBlank { "Not specified" }})"
            }
            list.joinToString(", ")
        }
    }
}
