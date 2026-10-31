package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AllergyInfo
import com.example.data.model.AssessmentResult
import com.example.data.model.ConsultationEntity
import com.example.data.model.FollowUpAnswers
import com.example.data.model.Medicine
import com.example.data.model.Symptom
import com.example.data.repository.HealthRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AssessmentUiState {
    data object Idle : AssessmentUiState
    data object Loading : AssessmentUiState
    data class Success(val result: AssessmentResult) : AssessmentUiState
    data class Error(val message: String) : AssessmentUiState
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class HealthViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HealthRepository

    init {
        val db = AppDatabase.getInstance(application)
        repository = HealthRepository(db)
        viewModelScope.launch {
            repository.initDatabase()
        }
    }

    // Symptom Selection State
    private val _selectedSymptoms = MutableStateFlow<List<Symptom>>(emptyList())
    val selectedSymptoms: StateFlow<List<Symptom>> = _selectedSymptoms.asStateFlow()

    private val _symptomSearchQuery = MutableStateFlow("")
    val symptomSearchQuery: StateFlow<String> = _symptomSearchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow("All")
    val selectedCategory: StateFlow<String> = _selectedCategory.asStateFlow()

    // Reactive Symptoms List with debounce
    val filteredSymptoms: StateFlow<List<Symptom>> = combine(
        _symptomSearchQuery.debounce(250L),
        _selectedCategory
    ) { query, category ->
        Pair(query, category)
    }.flatMapLatest { (query, category) ->
        if (query.isNotBlank()) {
            repository.searchSymptoms(query)
        } else if (category != "All") {
            repository.getSymptomsByCategory(category)
        } else {
            repository.getAllSymptoms()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Allergy State
    private val _allergyInfo = MutableStateFlow(AllergyInfo())
    val allergyInfo: StateFlow<AllergyInfo> = _allergyInfo.asStateFlow()

    // Follow-up Answers State
    private val _followUpAnswers = MutableStateFlow(FollowUpAnswers())
    val followUpAnswers: StateFlow<FollowUpAnswers> = _followUpAnswers.asStateFlow()

    // Assessment State
    private val _assessmentUiState = MutableStateFlow<AssessmentUiState>(AssessmentUiState.Idle)
    val assessmentUiState: StateFlow<AssessmentUiState> = _assessmentUiState.asStateFlow()

    // Medicine Catalogue State
    private val _medicineSearchQuery = MutableStateFlow("")
    val medicineSearchQuery: StateFlow<String> = _medicineSearchQuery.asStateFlow()

    val filteredMedicines: StateFlow<List<Medicine>> = _medicineSearchQuery
        .debounce(250L)
        .flatMapLatest { query ->
            if (query.isNotBlank()) {
                repository.searchMedicines(query)
            } else {
                repository.getAllMedicines()
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Consultation History State
    val consultationHistory: StateFlow<List<ConsultationEntity>> = repository.getAllConsultations()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Actions
    fun toggleSymptom(symptom: Symptom) {
        val current = _selectedSymptoms.value.toMutableList()
        val exists = current.any { it.id == symptom.id }
        if (exists) {
            current.removeAll { it.id == symptom.id }
            _selectedSymptoms.value = current
        } else {
            if (current.size < 5) {
                current.add(symptom)
                _selectedSymptoms.value = current
            }
        }
    }

    fun removeSymptom(symptom: Symptom) {
        val current = _selectedSymptoms.value.toMutableList()
        current.removeAll { it.id == symptom.id }
        _selectedSymptoms.value = current
    }

    fun clearSelectedSymptoms() {
        _selectedSymptoms.value = emptyList()
    }

    fun setSymptomSearch(query: String) {
        _symptomSearchQuery.value = query
    }

    fun setCategory(category: String) {
        _selectedCategory.value = category
    }

    fun updateAllergyInfo(info: AllergyInfo) {
        _allergyInfo.value = info
    }

    fun updateFollowUpAnswers(answers: FollowUpAnswers) {
        _followUpAnswers.value = answers
    }

    fun setMedicineSearch(query: String) {
        _medicineSearchQuery.value = query
    }

    fun startAssessment() {
        val symptoms = _selectedSymptoms.value
        if (symptoms.isEmpty()) return

        _assessmentUiState.value = AssessmentUiState.Loading
        viewModelScope.launch {
            try {
                val result = repository.analyzeHealth(
                    symptoms = symptoms,
                    allergyInfo = _allergyInfo.value,
                    answers = _followUpAnswers.value
                )
                _assessmentUiState.value = AssessmentUiState.Success(result)
            } catch (e: Exception) {
                _assessmentUiState.value = AssessmentUiState.Error(
                    e.localizedMessage ?: "Unable to complete assessment. Please check connection and try again."
                )
            }
        }
    }

    fun resetAssessmentFlow() {
        _selectedSymptoms.value = emptyList()
        _symptomSearchQuery.value = ""
        _selectedCategory.value = "All"
        _allergyInfo.value = AllergyInfo()
        _followUpAnswers.value = FollowUpAnswers()
        _assessmentUiState.value = AssessmentUiState.Idle
    }

    fun deleteConsultation(id: String) {
        viewModelScope.launch {
            repository.deleteConsultation(id)
        }
    }

    fun clearAllConsultations() {
        viewModelScope.launch {
            repository.clearConsultations()
        }
    }
}
