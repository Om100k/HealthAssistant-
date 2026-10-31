package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.example.data.model.FollowUpAnswers
import com.example.ui.components.DisclaimerBanner
import com.example.ui.components.GptCard
import com.example.ui.components.StepProgressIndicator
import com.example.ui.viewmodel.HealthViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FollowUpQuestionsScreen(
    viewModel: HealthViewModel,
    onBack: () -> Unit,
    onContinueToAnalysis: () -> Unit
) {
    val selectedSymptoms by viewModel.selectedSymptoms.collectAsStateWithLifecycle()
    val initialAnswers by viewModel.followUpAnswers.collectAsStateWithLifecycle()

    var duration by remember { mutableStateOf(initialAnswers.duration.ifBlank { "1-3 days" }) }
    var severity by remember { mutableStateOf(initialAnswers.severity.ifBlank { "Moderate" }) }
    var onset by remember { mutableStateOf(initialAnswers.onset.ifBlank { "Gradual onset" }) }
    var progression by remember { mutableStateOf(initialAnswers.progression.ifBlank { "Stable" }) }
    var associatedSymptoms by remember { mutableStateOf(initialAnswers.associatedSymptoms.toSet()) }
    var medications by remember { mutableStateOf(initialAnswers.currentMedications) }
    var existingConditions by remember { mutableStateOf(initialAnswers.existingConditions.toSet()) }
    var ageGroup by remember { mutableStateOf(initialAnswers.ageGroup.ifBlank { "Adult (18-64)" }) }

    val durationOptions = listOf("<24 hours", "1-3 days", "4-7 days", "1-2 weeks", ">2 weeks")
    val severityOptions = listOf("Mild", "Moderate", "Severe", "Critical")
    val onsetOptions = listOf("Sudden onset", "Gradual onset")
    val progressionOptions = listOf("Improving", "Stable", "Getting worse", "Fluctuating")
    val commonAssociated = listOf(
        "Fever", "Chills", "Fatigue", "Nausea", "Dizziness", "Shortness of breath", "Headache", "Body aches", "None"
    )
    val conditionsOptions = listOf(
        "Hypertension", "Diabetes", "Asthma/COPD", "Heart Disease", "Kidney Disease", "Liver Disease", "None"
    )
    val ageOptions = listOf("Child (0-12)", "Teen (13-17)", "Adult (18-64)", "Senior (65+)")

    fun persistAndStart() {
        viewModel.updateFollowUpAnswers(
            FollowUpAnswers(
                duration = duration,
                severity = severity,
                onset = onset,
                progression = progression,
                associatedSymptoms = associatedSymptoms.toList(),
                currentMedications = medications,
                existingConditions = existingConditions.toList(),
                ageGroup = ageGroup
            )
        )
        viewModel.startAssessment()
        onContinueToAnalysis()
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
                        text = "Clinical Context",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                StepProgressIndicator(
                    currentStep = 3,
                    totalSteps = 5,
                    stepTitle = "Follow-up Details"
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
                    onClick = { persistAndStart() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("submit_assessment_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(
                        text = "Generate AI Health Assessment",
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Symptom-specific hint card if hints exist
            val hints = selectedSymptoms.mapNotNull { it.followUpHint.ifBlank { null } }
            if (hints.isNotEmpty()) {
                GptCard(
                    backgroundColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                    borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                    padding = 12.dp
                ) {
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Outlined.HelpOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(18.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Symptom Specific Considerations",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            hints.take(2).forEach { hint ->
                                Text(
                                    text = "• $hint",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }
            }

            // 1. How long have you experienced the symptoms?
            QuestionGroup(
                title = "How long have you experienced the symptoms?",
                options = durationOptions,
                selected = duration,
                onSelect = { duration = it }
            )

            // 2. How severe are they?
            QuestionGroup(
                title = "How severe are the symptoms?",
                options = severityOptions,
                selected = severity,
                onSelect = { severity = it }
            )

            // 3. When did they start?
            QuestionGroup(
                title = "How did the symptoms begin?",
                options = onsetOptions,
                selected = onset,
                onSelect = { onset = it }
            )

            // 4. Progression
            QuestionGroup(
                title = "Are they improving or worsening?",
                options = progressionOptions,
                selected = progression,
                onSelect = { progression = it }
            )

            // 5. Associated symptoms (Multiple selection)
            Column {
                Text(
                    text = "Are there other associated symptoms?",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    commonAssociated.forEach { item ->
                        val isSelected = associatedSymptoms.contains(item)
                        FilterPill(
                            label = item,
                            isSelected = isSelected,
                            onClick = {
                                associatedSymptoms = if (item == "None") {
                                    setOf("None")
                                } else {
                                    val set = (associatedSymptoms - "None").toMutableSet()
                                    if (isSelected) set.remove(item) else set.add(item)
                                    set
                                }
                            }
                        )
                    }
                }
            }

            // 6. Existing conditions
            Column {
                Text(
                    text = "Do you have any existing medical conditions?",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    conditionsOptions.forEach { condition ->
                        val isSelected = existingConditions.contains(condition)
                        FilterPill(
                            label = condition,
                            isSelected = isSelected,
                            onClick = {
                                existingConditions = if (condition == "None") {
                                    setOf("None")
                                } else {
                                    val set = (existingConditions - "None").toMutableSet()
                                    if (isSelected) set.remove(condition) else set.add(condition)
                                    set
                                }
                            }
                        )
                    }
                }
            }

            // 7. Current medications
            Column {
                Text(
                    text = "Are you taking any current medications?",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = medications,
                    onValueChange = { medications = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("current_medications_input"),
                    placeholder = {
                        Text(
                            text = "List prescription or OTC medications (or leave blank if none)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    },
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }

            // 8. Age group
            QuestionGroup(
                title = "What is your age group?",
                options = ageOptions,
                selected = ageGroup,
                onSelect = { ageGroup = it }
            )

            Spacer(modifier = Modifier.height(10.dp))
            DisclaimerBanner(compact = true)
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuestionGroup(
    title: String,
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { option ->
                FilterPill(
                    label = option,
                    isSelected = option == selected,
                    onClick = { onSelect(option) }
                )
            }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    Box(
        modifier = Modifier
            .clip(shape)
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .border(
                width = 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outlineVariant,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp)
            .testTag("pill_${label.replace(" ", "_")}")
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
    }
}
