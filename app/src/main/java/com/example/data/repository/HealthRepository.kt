package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.AllergyInfo
import com.example.data.model.AssessmentResult
import com.example.data.model.ConsultationEntity
import com.example.data.model.FollowUpAnswers
import com.example.data.model.Medicine
import com.example.data.model.Symptom
import com.example.data.remote.AiHealthService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class HealthRepository(
    private val database: AppDatabase,
    private val aiService: AiHealthService = AiHealthService()
) {
    private val symptomDao = database.symptomDao()
    private val medicineDao = database.medicineDao()
    private val consultationDao = database.consultationDao()

    suspend fun initDatabase() = withContext(Dispatchers.IO) {
        database.populateInitialData()
    }

    fun getAllSymptoms(): Flow<List<Symptom>> = symptomDao.getAllSymptoms()

    fun getSymptomsByCategory(category: String): Flow<List<Symptom>> =
        symptomDao.getSymptomsByCategory(category)

    fun searchSymptoms(query: String): Flow<List<Symptom>> =
        symptomDao.searchSymptoms(query.trim())

    fun getAllMedicines(): Flow<List<Medicine>> = medicineDao.getAllMedicines()

    fun searchMedicines(query: String): Flow<List<Medicine>> =
        medicineDao.searchMedicines(query.trim())

    suspend fun getMedicineById(id: String): Medicine? = withContext(Dispatchers.IO) {
        medicineDao.getMedicineById(id)
    }

    fun getAllConsultations(): Flow<List<ConsultationEntity>> =
        consultationDao.getAllConsultations()

    suspend fun saveConsultation(result: AssessmentResult) = withContext(Dispatchers.IO) {
        val suggestedSummary = result.suggestedMedicines.joinToString("; ") { "${it.name} (${it.tabletType})" }
        val entity = ConsultationEntity(
            id = result.id,
            timestamp = result.timestamp,
            symptomNames = result.selectedSymptoms.joinToString(", ") { it.name },
            severity = result.followUpAnswers.severity.ifBlank { "Moderate" },
            summaryText = result.symptomsSummary,
            isEmergency = result.isEmergencyFlagged,
            suggestedMedicineSummary = suggestedSummary
        )
        consultationDao.insertConsultation(entity)
    }

    suspend fun deleteConsultation(id: String) = withContext(Dispatchers.IO) {
        consultationDao.deleteConsultation(id)
    }

    suspend fun clearConsultations() = withContext(Dispatchers.IO) {
        consultationDao.clearAll()
    }

    suspend fun analyzeHealth(
        symptoms: List<Symptom>,
        allergyInfo: AllergyInfo,
        answers: FollowUpAnswers
    ): AssessmentResult = withContext(Dispatchers.IO) {
        val result = aiService.analyzeSymptoms(symptoms, allergyInfo, answers)
        // Automatically save to local history so user has consultation record
        saveConsultation(result)
        result
    }
}
