package com.example.data.remote

import android.util.Log
import com.example.data.local.MedicineCatalogueData
import com.example.data.model.AllergyInfo
import com.example.data.model.AssessmentResult
import com.example.data.model.FollowUpAnswers
import com.example.data.model.Medicine
import com.example.data.model.Symptom
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import kotlinx.coroutines.withTimeout
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val TAG = "AiHealthService"

class AiHealthService {

    // Lazy initialization of Firebase Generative Model as required by skill
    private val generativeModel by lazy {
        try {
            Firebase.ai.generativeModel(
                modelName = "gemini-3.8-flash"
            )
        } catch (e: Exception) {
            Log.w(TAG, "Firebase.ai could not be initialized lazily; will use local clinical reasoning fallback", e)
            null
        }
    }

    suspend fun analyzeSymptoms(
        selectedSymptoms: List<Symptom>,
        allergyInfo: AllergyInfo,
        followUpAnswers: FollowUpAnswers
    ): AssessmentResult {
        val hasRedFlagSymptom = selectedSymptoms.any { it.isRedFlag } ||
                followUpAnswers.severity.equals("Critical", ignoreCase = true) ||
                followUpAnswers.severity.equals("Severe", ignoreCase = true)

        val relevantMedicines = findEducationalMedicinesForSymptoms(selectedSymptoms)

        // Try AI generation with realistic coroutine timeout
        try {
            val model = generativeModel
            if (model != null) {
                val prompt = buildAnalysisPrompt(selectedSymptoms, allergyInfo, followUpAnswers)
                val response = withTimeout(40_000L) {
                    model.generateContent(prompt)
                }
                val text = response.text
                if (!text.isNullOrBlank()) {
                    val parsed = parseAiResponse(text, selectedSymptoms, allergyInfo, followUpAnswers, relevantMedicines, hasRedFlagSymptom)
                    if (parsed != null) {
                        return parsed
                    }
                }
            }
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            Log.w(TAG, "AI generation timed out; falling back to clinical decision engine", e)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "AI inference unavailable; using local clinical knowledge engine", e)
        }

        // Reliable clinical knowledge fallback
        return generateClinicalKnowledgeResult(
            selectedSymptoms = selectedSymptoms,
            allergyInfo = allergyInfo,
            followUpAnswers = followUpAnswers,
            relevantMedicines = relevantMedicines,
            hasRedFlag = hasRedFlagSymptom
        )
    }

    private fun buildAnalysisPrompt(
        symptoms: List<Symptom>,
        allergyInfo: AllergyInfo,
        answers: FollowUpAnswers
    ): String {
        val symptomsStr = symptoms.joinToString("; ") { "${it.name} (${it.category}, typical severity: ${it.typicalSeverity})" }
        return """
            You are a clinical educational assistant providing non-diagnostic healthcare guidance.
            User Profile & Consultation Data:
            - Selected Symptoms: $symptomsStr
            - Allergy Profile: ${allergyInfo.summaryText()}
            - Duration: ${answers.duration.ifBlank { "Not specified" }}
            - Reported Severity: ${answers.severity.ifBlank { "Not specified" }}
            - Onset: ${answers.onset.ifBlank { "Not specified" }}
            - Progression: ${answers.progression.ifBlank { "Not specified" }}
            - Associated Symptoms: ${answers.associatedSymptoms.joinToString(", ").ifBlank { "None reported" }}
            - Current Medications: ${answers.currentMedications.ifBlank { "None reported" }}
            - Existing Conditions: ${answers.existingConditions.joinToString(", ").ifBlank { "None reported" }}
            - Age Group: ${answers.ageGroup.ifBlank { "Adult" }}

            Provide your educational response in STRICT JSON format with the following keys:
            {
              "symptomsSummary": "A scannable 2-3 sentence overview of the symptom pattern and timeline.",
              "possibleGeneralCauses": ["Potential non-diagnostic general cause 1", "Potential general cause 2", "Potential general cause 3"],
              "physiologicalExplanation": "Clear explanation of how the body's immune, neurological, or organ systems typically produce these sensations.",
              "suggestedMedicines": [
                {
                  "name": "Specific medicine or tablet name (e.g. Acetaminophen 500mg Tablet or Cetirizine 10mg Tablet)",
                  "tabletType": "Oral Tablet / Film-Coated Tablet / Chewable Tablet / Capsule",
                  "activeIngredient": "Active drug ingredient",
                  "purpose": "Precise reason why this tablet/medicine is suggested for the reported symptoms",
                  "dosageGuidance": "Standard adult OTC dosage advice (e.g. 1 tablet every 4-6h with water after meals; max 4000mg/24h. Always consult pharmacist)",
                  "precautions": "Safety notes regarding allergies, drug interactions, or organ safety",
                  "suitabilityBadge": "First-Line OTC Option"
                }
              ],
              "warningSigns": ["Specific emergency indicator 1", "Specific indicator 2", "When to go to urgent care"],
              "allergyConcerns": ["Precautions regarding user's reported allergies or medicine sensitivities"],
              "recommendedNextSteps": ["Self-care step 1", "Hydration/rest advice", "Symptom monitoring guideline"],
              "whenToConsultProfessional": "Clear criteria for when an in-person physician or pharmacist evaluation is essential."
            }

            STRICT MEDICAL SAFETY RULES:
            1. Do NOT provide a definitive diagnosis or claim certainty.
            2. Do NOT prescribe personalized prescription regimens; provide standard adult OTC educational guidance.
            3. Emphasize that this is educational information and user must consult a doctor or pharmacist.
            4. Output ONLY valid JSON without markdown wrapping.
        """.trimIndent()
    }

    private fun parseAiResponse(
        rawText: String,
        symptoms: List<Symptom>,
        allergyInfo: AllergyInfo,
        answers: FollowUpAnswers,
        medicines: List<Medicine>,
        isEmergency: Boolean
    ): AssessmentResult? {
        return try {
            // Remove code fences if present
            val cleanJson = rawText.trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()

            val json = JSONObject(cleanJson)
            val summary = json.optString("symptomsSummary", "Educational review of reported symptoms.")
            val causes = json.optJSONArray("possibleGeneralCauses")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
            val physiology = json.optString("physiologicalExplanation", "")
            val warnings = json.optJSONArray("warningSigns")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
            val allergyNotes = json.optJSONArray("allergyConcerns")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
            val nextSteps = json.optJSONArray("recommendedNextSteps")?.let { arr ->
                (0 until arr.length()).map { arr.getString(it) }
            } ?: emptyList()
            val whenToSeeDoc = json.optString("whenToConsultProfessional", "Consult a doctor if symptoms persist or worsen.")

            val allCatalogueMeds = MedicineCatalogueData.getAllMedicines()
            val parsedSuggestedMeds = mutableListOf<com.example.data.model.SuggestedMedicine>()

            json.optJSONArray("suggestedMedicines")?.let { arr ->
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val medName = obj.optString("name", "")
                    val activeIng = obj.optString("activeIngredient", "")
                    if (medName.isNotBlank()) {
                        val matchedCatalogue = allCatalogueMeds.find { catMed ->
                            catMed.medicineName.contains(activeIng, ignoreCase = true) ||
                                    catMed.genericName.contains(activeIng, ignoreCase = true) ||
                                    medName.contains(catMed.medicineName.take(6), ignoreCase = true) ||
                                    medName.contains(catMed.genericName.take(6), ignoreCase = true)
                        }
                        parsedSuggestedMeds.add(
                            com.example.data.model.SuggestedMedicine(
                                id = "ai_sug_$i",
                                name = medName,
                                tabletType = obj.optString("tabletType", "Oral Tablet"),
                                activeIngredient = activeIng,
                                purpose = obj.optString("purpose", "Symptomatic relief for reported conditions."),
                                dosageGuidance = obj.optString("dosageGuidance", "Follow manufacturer's packaging instructions and consult a pharmacist."),
                                precautions = obj.optString("precautions", "Check ingredients for allergies and do not exceed recommended daily limits."),
                                suitabilityBadge = obj.optString("suitabilityBadge", "Suggested Tablet / Medicine"),
                                catalogueMedicineId = matchedCatalogue?.id
                            )
                        )
                    }
                }
            }

            // Fallback to clinical decision engine if AI suggested medicines was empty
            val finalSuggestedMedicines = if (parsedSuggestedMeds.isNotEmpty()) {
                parsedSuggestedMeds
            } else {
                determineSuggestedMedicines(symptoms, allergyInfo, answers)
            }

            AssessmentResult(
                id = UUID.randomUUID().toString(),
                timestamp = System.currentTimeMillis(),
                symptomsSummary = summary,
                possibleGeneralCauses = causes.ifEmpty { listOf("Common self-limiting conditions", "Seasonal viral syndromes") },
                physiologicalExplanation = physiology,
                warningSigns = warnings,
                allergyConcerns = allergyNotes,
                recommendedNextSteps = nextSteps,
                whenToConsultProfessional = whenToSeeDoc,
                suggestedMedicines = finalSuggestedMedicines,
                educationalMedicines = medicines,
                isEmergencyFlagged = isEmergency,
                selectedSymptoms = symptoms,
                allergyInfo = allergyInfo,
                followUpAnswers = answers,
                rawAiResponse = rawText
            )
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse AI JSON response: ${e.message}")
            null
        }
    }

    private fun generateClinicalKnowledgeResult(
        selectedSymptoms: List<Symptom>,
        allergyInfo: AllergyInfo,
        followUpAnswers: FollowUpAnswers,
        relevantMedicines: List<Medicine>,
        hasRedFlag: Boolean
    ): AssessmentResult {
        val symptomNames = selectedSymptoms.map { it.name }
        val categories = selectedSymptoms.map { it.category }.distinct()

        // Detailed clinical reasoning synthesis
        val summary = buildString {
            append("You reported experiencing ${symptomNames.joinToString(", ")}. ")
            if (followUpAnswers.duration.isNotBlank()) {
                append("These symptoms have been present for ${followUpAnswers.duration} with a ${followUpAnswers.onset.lowercase().ifBlank { "gradual" }} onset. ")
            }
            if (followUpAnswers.severity.isNotBlank()) {
                append("Current subjective severity is rated as ${followUpAnswers.severity}. ")
            }
            if (followUpAnswers.progression.isNotBlank()) {
                append("The clinical trajectory is currently noted as ${followUpAnswers.progression.lowercase()}.")
            }
        }

        val causes = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val nextSteps = mutableListOf<String>()
        val allergyNotes = mutableListOf<String>()

        // Analyze by categories & symptoms
        if (categories.contains("Respiratory") || categories.contains("Throat and Neck") || categories.contains("Nose and Sinuses")) {
            causes.add("Upper or lower respiratory tract inflammation (viral rhinosinusitis, pharyngitis, or bronchitis)")
            causes.add("Environmental allergen exposure triggering histamine release and mucosal edema")
            causes.add("Post-nasal irritation secondary to indoor heating, dust, or air pollutants")
            nextSteps.add("Maintain generous hydration with warm fluids, herbal teas, or broths to soothe throat lining and thin bronchial mucus")
            nextSteps.add("Use a cool-mist room humidifier or saline nasal rinse (isotonic spray) to hydrate nasal mucosa")
            nextSteps.add("Rest your voice and avoid exposure to tobacco smoke, aerosol sprays, and chemical irritants")
        }

        if (categories.contains("Head and Brain")) {
            causes.add("Cervicogenic tension or muscle contraction headache linked to prolonged screen posture or physical stress")
            causes.add("Vascular headache or migraine pathway with transient neurovascular sensitivity")
            causes.add("Systemic response to dehydration, eye fatigue, or irregular sleep-wake patterns")
            nextSteps.add("Apply a cool gel pack or warm compress across temples, forehead, or back of neck according to comfort")
            nextSteps.add("Rest in a quiet, dimly lit room with limited screen exposure and minimal sound stimulation")
            nextSteps.add("Ensure consistent fluid intake throughout the day with balanced electrolyte replenishment")
        }

        if (categories.contains("Digestive System") || categories.contains("Abdomen")) {
            causes.add("Acute viral or bacterial gastroenteritis with temporary mucosal irritation")
            causes.add("Functional dyspepsia or gastroesophageal acid reflux (GERD) aggravated by dietary triggers")
            causes.add("Food intolerances (lactose, high FODMAP carbohydrates, or dietary fats)")
            nextSteps.add("Follow the BRAT diet (bananas, rice, applesauce, toast) and take small, frequent sips of electrolyte fluids")
            nextSteps.add("Avoid large meals, carbonated beverages, spicy, fried, acidic, or highly seasoned foods")
            nextSteps.add("Remain upright for at least 2 to 3 hours after eating to reduce acid regurgitation")
        }

        if (categories.contains("Skin and Hair") || categories.contains("Allergies")) {
            causes.add("Contact dermatitis from laundry detergents, fragrances, cosmetics, or new topicals")
            causes.add("Atopic flare or urticaria driven by mast-cell histamine degranulation")
            causes.add("Eczematous barrier compromise exacerbated by dry climate or hot water bathing")
            nextSteps.add("Wash skin with lukewarm water and a fragrance-free, gentle hypoallergenic syndet cleanser")
            nextSteps.add("Apply an unscented ceramide-rich barrier cream to lock in stratum corneum moisture")
            nextSteps.add("Avoid scratching irritated areas to prevent secondary bacterial superinfections")
        }

        if (categories.contains("Bones and Joints") || categories.contains("Muscles")) {
            causes.add("Musculoskeletal strain, mechanical overload, or repetitive motion micro-trauma")
            causes.add("Inflammatory arthropathy or localized soft tissue inflammation (tendinitis/bursitis)")
            causes.add("Post-viral reactive myalgia or systemic inflammatory response")
            nextSteps.add("Utilize the R.I.C.E. protocol (Rest, Ice for acute flares, gentle Compression, and Elevation)")
            nextSteps.add("Perform gentle range-of-motion stretching once acute pain decreases")
        }

        if (causes.isEmpty()) {
            causes.add("Self-limiting acute viral syndrome or non-specific physiological response")
            causes.add("Environmental stress, sleep disruption, or physical fatigue")
            causes.add("Mild localized tissue irritation or functional somatic response")
            nextSteps.add("Prioritize 7-9 hours of restorative sleep in a cool, quiet room")
            nextSteps.add("Keep a symptom diary tracking potential triggers, timing, and relieving factors")
        }

        // Warning signs & Red Flags
        if (hasRedFlag) {
            warnings.add("CRITICAL / RED FLAG: One or more selected symptoms may indicate an urgent health emergency.")
            warnings.add("Sudden intense shortness of breath, crushing central chest pain, or radiating arm/jaw discomfort requires calling emergency services (911/112) immediately.")
            warnings.add("Sudden one-sided weakness, facial drooping, or speech slurring requires immediate emergency evaluation.")
        } else {
            warnings.add("Seek urgent care if you develop a persistent high fever (>103°F / 39.4°C) unresponsive to antipyretics.")
            warnings.add("Watch for inability to retain liquids for over 24 hours leading to marked dehydration (dark urine, dizziness).")
            warnings.add("Noticeable progression or sudden worsening of symptoms beyond 5-7 days requires doctor evaluation.")
        }

        // Allergy safety checks
        if (allergyInfo.selectedAllergyTypes.contains("Medicine allergies") && allergyInfo.medicineName.isNotBlank()) {
            allergyNotes.add("ATTENTION: You reported an allergy to '${allergyInfo.medicineName}'. Always inform prescribing physicians and dispensing pharmacists of this history.")
            allergyNotes.add("Cross-check all active pharmaceutical ingredients and excipients before consuming any over-the-counter remedy.")
        } else if (allergyInfo.hasNoKnownAllergies) {
            allergyNotes.add("No specific drug or food allergies were reported. Maintain standard vigilance with any new medication.")
        } else if (allergyInfo.isUnanswered) {
            allergyNotes.add("Note: Allergy status was not specified. Always verify that you have no allergy before taking any medication.")
        }

        // When to consult professional
        val whenToSeeDoc = if (hasRedFlag) {
            "Emergency medical evaluation is strongly recommended without delay. Do not wait to see if red-flag symptoms resolve on their own."
        } else {
            "Schedule an appointment with a primary care physician if your symptoms fail to improve after 3 to 5 days, or visit your local community pharmacist for guidance on safe, non-prescription supportive measures."
        }

        val physiologicalText = buildString {
            append("When foreign antigens, physical strain, or pathogens perturb bodily homeostasis, immune cells (macrophages, mast cells, neutrophils) release signaling cytokines (interleukins, TNF-alpha) and inflammatory mediators (prostaglandins, histamines). ")
            append("These molecules cause local vasodilation, increased capillary permeability, and stimulation of peripheral nociceptors, which the central nervous system perceives as pain, swelling, congestion, or fever.")
        }

        val suggestedMedicines = determineSuggestedMedicines(
            symptoms = selectedSymptoms,
            allergyInfo = allergyInfo,
            followUpAnswers = followUpAnswers
        )

        return AssessmentResult(
            id = UUID.randomUUID().toString(),
            timestamp = System.currentTimeMillis(),
            symptomsSummary = summary,
            possibleGeneralCauses = causes,
            physiologicalExplanation = physiologicalText,
            warningSigns = warnings,
            allergyConcerns = allergyNotes,
            recommendedNextSteps = nextSteps,
            whenToConsultProfessional = whenToSeeDoc,
            suggestedMedicines = suggestedMedicines,
            educationalMedicines = relevantMedicines,
            isEmergencyFlagged = hasRedFlag,
            selectedSymptoms = selectedSymptoms,
            allergyInfo = allergyInfo,
            followUpAnswers = followUpAnswers,
            rawAiResponse = ""
        )
    }

    private fun determineSuggestedMedicines(
        symptoms: List<Symptom>,
        allergyInfo: AllergyInfo,
        followUpAnswers: FollowUpAnswers
    ): List<com.example.data.model.SuggestedMedicine> {
        val results = mutableListOf<com.example.data.model.SuggestedMedicine>()
        val categories = symptoms.map { it.category }.toSet()
        val symptomNames = symptoms.map { it.name.lowercase() }
        val reportedAllergy = (allergyInfo.medicineName + " " + allergyInfo.selectedAllergyTypes.joinToString(" ")).lowercase()

        val isAspirinOrNsaidAllergic = reportedAllergy.contains("ibuprofen") ||
                reportedAllergy.contains("nsaid") ||
                reportedAllergy.contains("aspirin")
        val isParacetamolAllergic = reportedAllergy.contains("paracetamol") ||
                reportedAllergy.contains("acetaminophen")

        // 1. Pain / Fever / Headache / Body aches
        val hasFeverOrAches = categories.contains("Head and Brain") ||
                categories.contains("Bones and Joints") ||
                categories.contains("Muscles") ||
                symptomNames.any { it.contains("fever") || it.contains("headache") || it.contains("ache") || it.contains("pain") || it.contains("sore") }

        if (hasFeverOrAches) {
            if (!isParacetamolAllergic) {
                results.add(
                    com.example.data.model.SuggestedMedicine(
                        id = "sug_pcm",
                        name = "Acetaminophen (Paracetamol) 500mg Tablet",
                        tabletType = "Oral Tablet",
                        activeIngredient = "Acetaminophen / Paracetamol 500mg",
                        purpose = "First-line tablet to reduce fever and relieve mild-to-moderate headache, muscle aches, and body pain without causing gastric irritation.",
                        dosageGuidance = "General adult OTC guidance: Take 1 to 2 tablets (500–1000 mg) every 4 to 6 hours with a glass of water as needed. Do not exceed 4,000 mg (8 tablets) in 24 hours.",
                        precautions = "Avoid alcohol during use. Do NOT combine with other cough or cold remedies containing acetaminophen to prevent accidental liver damage.",
                        suitabilityBadge = if (isAspirinOrNsaidAllergic) "Safe Non-NSAID Alternative" else "First-Line OTC Tablet",
                        catalogueMedicineId = "med_01"
                    )
                )
            } else if (!isAspirinOrNsaidAllergic) {
                results.add(
                    com.example.data.model.SuggestedMedicine(
                        id = "sug_ibu",
                        name = "Ibuprofen 400mg Tablet",
                        tabletType = "Film-Coated Tablet",
                        activeIngredient = "Ibuprofen 400mg",
                        purpose = "Anti-inflammatory pain relief tablet targeting localized tissue swelling, joint tenderness, and muscular stiffness.",
                        dosageGuidance = "General adult OTC guidance: Take 1 tablet (400 mg) every 6 to 8 hours strictly after a meal or with a glass of milk to protect stomach lining.",
                        precautions = "Take with food. Contraindicated with active stomach ulcers, cardiovascular disease, or aspirin/NSAID hypersensitivity.",
                        suitabilityBadge = "Anti-Inflammatory Tablet",
                        catalogueMedicineId = "med_02"
                    )
                )
            }
        }

        // 2. Respiratory / Cold / Cough / Congestion / Allergies
        val hasRespiratoryOrAllergy = categories.contains("Respiratory") ||
                categories.contains("Throat and Neck") ||
                categories.contains("Nose and Sinuses") ||
                categories.contains("Allergies") ||
                symptomNames.any { it.contains("cough") || it.contains("sneeze") || it.contains("runny") || it.contains("congestion") || it.contains("itch") }

        if (hasRespiratoryOrAllergy) {
            val hasCoughPhlegm = symptomNames.any { it.contains("cough") || it.contains("phlegm") || it.contains("mucus") || it.contains("chest") }
            val hasAllergyRhinitis = symptomNames.any { it.contains("sneeze") || it.contains("runny") || it.contains("itch") || it.contains("allergy") || it.contains("sinus") }

            if (hasAllergyRhinitis || (!hasCoughPhlegm && results.size < 2)) {
                val isCetirizineAllergic = reportedAllergy.contains("cetirizine") || reportedAllergy.contains("antihistamine")
                if (!isCetirizineAllergic) {
                    results.add(
                        com.example.data.model.SuggestedMedicine(
                            id = "sug_cet",
                            name = "Cetirizine 10mg Tablet",
                            tabletType = "Film-Coated Tablet",
                            activeIngredient = "Cetirizine Hydrochloride 10mg",
                            purpose = "Second-generation antihistamine tablet providing 24-hour relief from sneezing, runny nose, allergic itch, and watery eyes.",
                            dosageGuidance = "General adult OTC guidance: Take 1 tablet (10 mg) once daily with water, preferably in the evening.",
                            precautions = "May cause mild somnolence in some individuals. Avoid alcoholic beverages and exercise caution when driving.",
                            suitabilityBadge = "Non-Drowsy Antihistamine",
                            catalogueMedicineId = "med_03"
                        )
                    )
                } else {
                    results.add(
                        com.example.data.model.SuggestedMedicine(
                            id = "sug_lor",
                            name = "Loratadine 10mg Tablet",
                            tabletType = "Oral Tablet",
                            activeIngredient = "Loratadine 10mg",
                            purpose = "Non-drowsy antihistamine tablet for allergic rhinitis, hay fever, and allergic skin hives.",
                            dosageGuidance = "Take 1 tablet (10 mg) once daily with water.",
                            precautions = "Do not exceed once-daily dosage. Consult a doctor or pharmacist if liver or kidney impairment exists.",
                            suitabilityBadge = "Non-Drowsy Antihistamine",
                            catalogueMedicineId = "med_04"
                        )
                    )
                }
            }

            if (hasCoughPhlegm && results.size < 3) {
                results.add(
                    com.example.data.model.SuggestedMedicine(
                        id = "sug_gua",
                        name = "Guaifenesin 400mg Expectorant Tablet",
                        tabletType = "Mucus Relief Tablet",
                        activeIngredient = "Guaifenesin 400mg",
                        purpose = "Expectorant tablet that thins and loosens thick bronchial mucus, making coughs more productive and clearing airways.",
                        dosageGuidance = "General adult OTC guidance: Take 1 tablet (400 mg) every 4 hours with a full glass of water. Maintain steady fluid intake.",
                        precautions = "Must drink ample water to support expectoration. Consult a physician if cough lasts longer than 7 days.",
                        suitabilityBadge = "Chest Congestion Tablet",
                        catalogueMedicineId = "med_09"
                    )
                )
            }
        }

        // 3. Digestive / Acidity / Heartburn / Upset Stomach
        val hasDigestive = categories.contains("Digestive System") ||
                categories.contains("Abdomen") ||
                symptomNames.any { it.contains("acid") || it.contains("heartburn") || it.contains("stomach") || it.contains("nausea") || it.contains("indigestion") || it.contains("diarrhea") }

        if (hasDigestive && results.size < 3) {
            val hasDiarrhea = symptomNames.any { it.contains("diarrhea") || it.contains("loose") }
            if (hasDiarrhea) {
                results.add(
                    com.example.data.model.SuggestedMedicine(
                        id = "sug_lop",
                        name = "Loperamide 2mg Tablet",
                        tabletType = "Anti-Diarrheal Tablet",
                        activeIngredient = "Loperamide Hydrochloride 2mg",
                        purpose = "Rapid relief of acute non-specific diarrhea by slowing intestinal hyper-motility.",
                        dosageGuidance = "General adult OTC guidance: Take 2 tablets (4 mg) initially, followed by 1 tablet (2 mg) after each unformed stool. Maximum 8 mg (4 tablets) per 24 hours.",
                        precautions = "Drink oral rehydration solutions. Do NOT use if blood or mucus is present in stool, or with high fever.",
                        suitabilityBadge = "Anti-Diarrheal Tablet",
                        catalogueMedicineId = "med_11"
                    )
                )
            } else {
                results.add(
                    com.example.data.model.SuggestedMedicine(
                        id = "sug_fam",
                        name = "Famotidine 20mg Tablet",
                        tabletType = "Acid Reducer Tablet",
                        activeIngredient = "Famotidine 20mg",
                        purpose = "H2-blocker tablet providing fast relief and prevention of heartburn, acid indigestion, and sour stomach.",
                        dosageGuidance = "General adult OTC guidance: Take 1 tablet (10–20 mg) with water 15 to 60 minutes before meals or at bedtime as needed.",
                        precautions = "Do not take more than 2 tablets in 24 hours. Limit OTC duration to 14 days without physician review.",
                        suitabilityBadge = "Acid Reducer Tablet",
                        catalogueMedicineId = "med_07"
                    )
                )
            }
        }

        // Baseline fallback if nothing specific matched or list is empty
        if (results.isEmpty()) {
            if (!isParacetamolAllergic) {
                results.add(
                    com.example.data.model.SuggestedMedicine(
                        id = "sug_pcm_default",
                        name = "Acetaminophen (Paracetamol) 500mg Tablet",
                        tabletType = "Oral Tablet",
                        activeIngredient = "Acetaminophen 500mg",
                        purpose = "First-line baseline pain and discomfort reliever for mild non-specific symptoms.",
                        dosageGuidance = "Take 1 tablet (500 mg) every 4 to 6 hours with a glass of water as needed. Do not exceed 4,000 mg in 24 hours.",
                        precautions = "Always verify you are not taking other medicines containing acetaminophen. Consult your pharmacist.",
                        suitabilityBadge = "First-Line OTC Option",
                        catalogueMedicineId = "med_01"
                    )
                )
            } else {
                results.add(
                    com.example.data.model.SuggestedMedicine(
                        id = "sug_cet_default",
                        name = "Cetirizine 10mg Tablet",
                        tabletType = "Film-Coated Tablet",
                        activeIngredient = "Cetirizine Hydrochloride 10mg",
                        purpose = "Supportive antihistamine tablet for symptom relief.",
                        dosageGuidance = "Take 1 tablet (10 mg) once daily with water.",
                        precautions = "Consult a pharmacist before use if taking sedatives or other medications.",
                        suitabilityBadge = "First-Line OTC Option",
                        catalogueMedicineId = "med_03"
                    )
                )
            }
        }

        return results
    }

    private fun findEducationalMedicinesForSymptoms(symptoms: List<Symptom>): List<Medicine> {
        val allMeds = MedicineCatalogueData.getAllMedicines()
        val categories = symptoms.map { it.category }.toSet()
        val names = symptoms.map { it.name.lowercase() }

        val matched = mutableListOf<Medicine>()

        if (categories.contains("Respiratory") || categories.contains("Throat and Neck") || categories.contains("Nose and Sinuses")) {
            allMeds.find { it.genericName.contains("Dextromethorphan", true) }?.let { matched.add(it) }
            allMeds.find { it.genericName.contains("Guaifenesin", true) }?.let { matched.add(it) }
            allMeds.find { it.genericName.contains("Pseudoephedrine", true) }?.let { matched.add(it) }
            allMeds.find { it.genericName.contains("Cetirizine", true) }?.let { matched.add(it) }
        }

        if (categories.contains("Head and Brain") || categories.contains("Bones and Joints") || categories.contains("Muscles")) {
            allMeds.find { it.genericName.contains("Acetaminophen", true) }?.let { matched.add(it) }
            allMeds.find { it.genericName.contains("Ibuprofen", true) }?.let { matched.add(it) }
        }

        if (categories.contains("Digestive System") || categories.contains("Abdomen")) {
            allMeds.find { it.genericName.contains("Famotidine", true) }?.let { matched.add(it) }
            allMeds.find { it.genericName.contains("Omeprazole", true) }?.let { matched.add(it) }
            allMeds.find { it.genericName.contains("Loperamide", true) }?.let { matched.add(it) }
            allMeds.find { it.genericName.contains("Electrolyte", true) }?.let { matched.add(it) }
        }

        if (categories.contains("Skin and Hair") || categories.contains("Allergies")) {
            allMeds.find { it.genericName.contains("Hydrocortisone", true) }?.let { matched.add(it) }
            allMeds.find { it.genericName.contains("Loratadine", true) }?.let { matched.add(it) }
            allMeds.find { it.genericName.contains("Cetirizine", true) }?.let { matched.add(it) }
        }

        // Default educational set if specific match was sparse
        if (matched.size < 2) {
            allMeds.find { it.genericName.contains("Acetaminophen", true) }?.let { matched.add(it) }
            allMeds.find { it.genericName.contains("Cetirizine", true) }?.let { matched.add(it) }
            allMeds.find { it.genericName.contains("Electrolyte", true) }?.let { matched.add(it) }
        }

        return matched.distinctBy { it.id }.take(4)
    }
}

