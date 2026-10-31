package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AllergyInfo
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.GptCard
import com.example.ui.components.StepProgressIndicator
import com.example.ui.viewmodel.HealthViewModel

@Composable
fun AllergyQuestionnaireScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit,
    onContinue: () -> Unit
) {
    val allergyInfo by viewModel.allergyInfo.collectAsStateWithLifecycle()

    var hasNoKnown by remember { mutableStateOf(allergyInfo.hasNoKnownAllergies) }
    var selectedTypes by remember { mutableStateOf(allergyInfo.selectedAllergyTypes.toSet()) }
    var medicineName by remember { mutableStateOf(allergyInfo.medicineName) }
    var medicineReaction by remember { mutableStateOf(allergyInfo.medicineReaction) }

    val allergyOptions = listOf(
        "No known allergies",
        "Medicine allergies",
        "Food allergies",
        "Dust allergies",
        "Pollen allergies",
        "Skin allergies",
        "Other",
        "Not sure"
    )

    fun persistState() {
        val updated = AllergyInfo(
            hasNoKnownAllergies = hasNoKnown,
            selectedAllergyTypes = selectedTypes.toList(),
            medicineName = medicineName,
            medicineReaction = medicineReaction,
            isUnanswered = !hasNoKnown && selectedTypes.isEmpty()
        )
        viewModel.updateAllergyInfo(updated)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.testTag("back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "Allergy Profile",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                StepProgressIndicator(
                    currentStep = 2,
                    totalSteps = 5,
                    stepTitle = "Allergy Questionnaire"
                )
            }
        },
        bottomBar = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.background)
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.outlineVariant
                    )
                    .padding(horizontal = 20.dp, vertical = 14.dp)
            ) {
                Button(
                    onClick = {
                        persistState()
                        onContinue()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("continue_to_follow_up_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Next: Follow-up Questions",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Do you have any known allergies?",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onBackground
            )

            Text(
                text = "Allergy and cross-reactivity checks protect your safety during medication educational review. Multiple selections are permitted.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Allergy checkboxes list
            allergyOptions.forEach { option ->
                val isChecked = if (option == "No known allergies") hasNoKnown else selectedTypes.contains(option)

                GptCard(
                    modifier = Modifier.testTag("allergy_option_${option.replace(" ", "_")}"),
                    backgroundColor = if (isChecked) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
                    else MaterialTheme.colorScheme.surface,
                    borderColor = if (isChecked) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.outlineVariant,
                    padding = 12.dp,
                    onClick = {
                        if (option == "No known allergies") {
                            hasNoKnown = !hasNoKnown
                            if (hasNoKnown) {
                                selectedTypes = emptySet()
                                medicineName = ""
                                medicineReaction = ""
                            }
                        } else {
                            hasNoKnown = false
                            selectedTypes = if (selectedTypes.contains(option)) {
                                selectedTypes - option
                            } else {
                                selectedTypes + option
                            }
                        }
                    }
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { checked ->
                                if (option == "No known allergies") {
                                    hasNoKnown = checked
                                    if (checked) {
                                        selectedTypes = emptySet()
                                        medicineName = ""
                                        medicineReaction = ""
                                    }
                                } else {
                                    hasNoKnown = false
                                    selectedTypes = if (checked) {
                                        selectedTypes + option
                                    } else {
                                        selectedTypes - option
                                    }
                                }
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = MaterialTheme.colorScheme.primary,
                                uncheckedColor = MaterialTheme.colorScheme.outlineVariant
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = option,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // If Medicine allergies is selected, prompt for medicine name and reaction
            if (selectedTypes.contains("Medicine allergies")) {
                Spacer(modifier = Modifier.height(6.dp))
                GptCard(
                    backgroundColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    padding = 16.dp
                ) {
                    Text(
                        text = "Medicine Allergy Details",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Specify the medication name and the reaction experienced (e.g., penicillin -> hives/swelling).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = medicineName,
                        onValueChange = { medicineName = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("medicine_allergy_name_input"),
                        label = { Text("Medicine Name (e.g., Penicillin, Sulfa, Aspirin)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = medicineReaction,
                        onValueChange = { medicineReaction = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("medicine_allergy_reaction_input"),
                        label = { Text("Reaction (e.g., Hives, Anaphylaxis, Rash, Swelling)") },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.surface,
                            unfocusedContainerColor = MaterialTheme.colorScheme.surface
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            DisclaimerBanner(compact = true)
        }
    }
}
