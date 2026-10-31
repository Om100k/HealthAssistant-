package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.ui.screens.AllergyQuestionnaireScreen
import com.example.ui.screens.AnalysisLoadingScreen
import com.example.ui.screens.ConsultationHistoryScreen
import com.example.ui.screens.FollowUpQuestionsScreen
import com.example.ui.screens.MedicineCatalogueScreen
import com.example.ui.screens.MedicineDetailScreen
import com.example.ui.screens.ResultsScreen
import com.example.ui.screens.SymptomSelectionScreen
import com.example.ui.screens.WelcomeScreen
import com.example.ui.viewmodel.HealthViewModel

@Composable
fun AppNavGraph(
    navController: NavHostController,
    viewModel: HealthViewModel = viewModel()
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Welcome.route
    ) {
        composable(Screen.Welcome.route) {
            WelcomeScreen(
                onBrowseSymptoms = {
                    navController.navigate(Screen.SymptomSelection.route)
                },
                onSearchSymptoms = {
                    navController.navigate(Screen.SymptomSelection.route)
                },
                onOpenMedicineCatalogue = {
                    navController.navigate(Screen.MedicineCatalogue.route)
                },
                onOpenHistory = {
                    navController.navigate(Screen.History.route)
                }
            )
        }

        composable(Screen.SymptomSelection.route) {
            SymptomSelectionScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Screen.AllergyQuestionnaire.route) }
            )
        }

        composable(Screen.AllergyQuestionnaire.route) {
            AllergyQuestionnaireScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onContinue = { navController.navigate(Screen.FollowUpQuestions.route) }
            )
        }

        composable(Screen.FollowUpQuestions.route) {
            FollowUpQuestionsScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onContinueToAnalysis = { navController.navigate(Screen.AnalysisLoading.route) }
            )
        }

        composable(Screen.AnalysisLoading.route) {
            AnalysisLoadingScreen(
                viewModel = viewModel,
                onSuccess = {
                    navController.navigate(Screen.Results.route) {
                        popUpTo(Screen.Welcome.route)
                    }
                },
                onCancel = { navController.popBackStack() }
            )
        }

        composable(Screen.Results.route) {
            ResultsScreen(
                viewModel = viewModel,
                onBrowseMedicines = { navController.navigate(Screen.MedicineCatalogue.route) },
                onSelectMedicine = { medicineId ->
                    navController.navigate(Screen.MedicineDetail.createRoute(medicineId))
                },
                onStartNewCheck = {
                    navController.navigate(Screen.Welcome.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.MedicineCatalogue.route) {
            MedicineCatalogueScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() },
                onSelectMedicine = { medicineId ->
                    navController.navigate(Screen.MedicineDetail.createRoute(medicineId))
                }
            )
        }

        composable(
            route = Screen.MedicineDetail.route,
            arguments = listOf(navArgument("medicineId") { type = NavType.StringType })
        ) { backStackEntry ->
            val medicineId = backStackEntry.arguments?.getString("medicineId").orEmpty()
            MedicineDetailScreen(
                medicineId = medicineId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            ConsultationHistoryScreen(
                viewModel = viewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
