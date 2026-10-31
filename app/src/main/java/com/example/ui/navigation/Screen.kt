package com.example.ui.navigation

sealed class Screen(val route: String) {
    data object Welcome : Screen("welcome")
    data object SymptomSelection : Screen("symptom_selection")
    data object AllergyQuestionnaire : Screen("allergy_questionnaire")
    data object FollowUpQuestions : Screen("follow_up_questions")
    data object AnalysisLoading : Screen("analysis_loading")
    data object Results : Screen("results")
    data object MedicineCatalogue : Screen("medicine_catalogue")
    data object MedicineDetail : Screen("medicine_detail/{medicineId}") {
        fun createRoute(medicineId: String) = "medicine_detail/$medicineId"
    }
    data object History : Screen("history")
}
