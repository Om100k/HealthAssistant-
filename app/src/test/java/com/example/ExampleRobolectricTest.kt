package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.MedicineCatalogueData
import com.example.data.local.SymptomCatalogueData
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("SymptoMed", appName)
    }

    @Test
    fun `verify symptom catalogue has at least 300 symptoms across 30 categories`() {
        val categories = SymptomCatalogueData.CATEGORIES
        assertEquals(30, categories.size)

        val allSymptoms = SymptomCatalogueData.getAllSymptoms()
        assertTrue("Expected at least 300 symptoms, found ${allSymptoms.size}", allSymptoms.size >= 300)

        // Verify distinct IDs
        val uniqueIds = allSymptoms.map { it.id }.toSet()
        assertEquals(allSymptoms.size, uniqueIds.size)

        // Verify every category has symptoms
        categories.forEach { category ->
            val count = allSymptoms.count { it.category == category }
            assertTrue("Category '$category' should have at least 8 symptoms, found $count", count >= 8)
        }
    }

    @Test
    fun `verify pharmacist verified medicines database`() {
        val medicines = MedicineCatalogueData.getAllMedicines()
        assertTrue("Medicines should not be empty", medicines.isNotEmpty())
        medicines.forEach { med ->
            assertTrue(med.medicineName.isNotBlank())
            assertTrue(med.genericName.isNotBlank())
            assertTrue(med.importantWarnings.isNotBlank())
            assertTrue(med.contraindications.isNotBlank())
        }
    }

    @Test
    fun `verify suggested medicine generation for symptoms`() = kotlinx.coroutines.test.runTest {
        val aiService = com.example.data.remote.AiHealthService()
        val symptoms = listOf(
            com.example.data.model.Symptom(
                id = "sym_01",
                name = "Tension Headache",
                category = "Head and Brain",
                description = "Dull aching head pain",
                typicalSeverity = "Mild-Moderate"
            )
        )
        val result = aiService.analyzeSymptoms(
            selectedSymptoms = symptoms,
            allergyInfo = com.example.data.model.AllergyInfo(),
            followUpAnswers = com.example.data.model.FollowUpAnswers()
        )
        assertTrue("Expected suggested medicines in assessment result", result.suggestedMedicines.isNotEmpty())
        val firstSuggested = result.suggestedMedicines.first()
        assertTrue("Suggested medicine name should not be blank", firstSuggested.name.isNotBlank())
        assertTrue("Tablet type should be specified", firstSuggested.tabletType.isNotBlank())
        assertTrue("Purpose should be specified", firstSuggested.purpose.isNotBlank())
        assertTrue("Dosage guidance should be specified", firstSuggested.dosageGuidance.isNotBlank())
    }
}

