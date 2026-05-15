package com.thisismine.myapplication.ai

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

// Data Classes
data class MechanicSummaryRequest(
    val bikeId: String,
    val bikeName: String,
    val currentOdometer: Int,
    val purchaseDateIso: String,
    val lastServiceDateIso: String?,
    val lastServiceOdometer: Int?,
    val recentServices: List<ServiceEntry>,
    val recentParts: List<PartEntry>,
    val activeMaintainance: List<String>,
    val fuelEfficiency: String?,
    val notes: String?
)

data class ServiceEntry(
    val dateIso: String,
    val odometerKm: Int,
    val category: String,
    val description: String,
    val cost: Double?
)

data class PartEntry(
    val dateIso: String,
    val odometerKm: Int,
    val partName: String,
    val reason: String,
    val cost: Double?
)

data class MechanicSummaryResponse(
    val summary: String,
    val recentWork: String,
    val currentConcerns: String,
    val recommendedChecks: String,
    val confidence: String
)

data class PredictiveRulesSuggestion(
    val component: String,
    val suggestedInterval: String,
    val rationale: String,
    val riskIfSkipped: String,
    val confidence: String
)

data class PredictiveRulesResponse(
    val suggestions: List<PredictiveRulesSuggestion>,
    val analysisNotes: String
)

class GeminiAiClient(private val apiKey: String) {
    companion object {
        private const val TAG = "GeminiAiClient"
        // Set to the exact model available in your account (from model list)
        // Example from your model list: models/gemini-2.5-flash -> use "gemini-2.5-flash"
        private const val MODEL_NAME = "gemini-2.5-flash"
        // Use v1 for Gemini models that are not available under v1beta
        private const val BASE_URL = "https://generativelanguage.googleapis.com/v1/models"
    }

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    suspend fun generateMechanicSummary(request: MechanicSummaryRequest): Result<MechanicSummaryResponse> =
        withContext(Dispatchers.IO) {
            try {
                val prompt = buildMechanicSummaryPrompt(request)
                val response = generateText(prompt)
                val parsed = parseMechanicSummaryResponse(response)
                Result.success(parsed)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating mechanic summary", e)
                Result.failure(e)
            }
        }

    suspend fun generatePredictiveRules(
        bikeId: String,
        bikeName: String,
        currentOdometer: Int,
        serviceHistory: List<ServiceEntry>,
        partHistory: List<PartEntry>
    ): Result<PredictiveRulesResponse> =
        withContext(Dispatchers.IO) {
            try {
                val prompt = buildPredictiveRulesPrompt(
                    bikeId, bikeName, currentOdometer, serviceHistory, partHistory
                )
                val response = generateText(prompt)
                val parsed = parsePredictiveRulesResponse(response)
                Result.success(parsed)
            } catch (e: Exception) {
                Log.e(TAG, "Error generating predictive rules", e)
                Result.failure(e)
            }
        }

    private suspend fun generateText(prompt: String): String = withContext(Dispatchers.IO) {
        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7f)
                put("topK", 40)
                put("topP", 0.95f)
                put("maxOutputTokens", 2048)
            })
        }

        // We'll try multiple URL/method/body combinations if we get 404s, since
        // different models and API versions expect different endpoints and payload shapes.
        val bases = listOf(
            BASE_URL, // v1
            "https://generativelanguage.googleapis.com/v1beta/models" // v1beta fallback
        )
        val methods = listOf("generateText", "generate", "generateContent")

        // Use the 'contents/parts' request shape by default. The simpler `prompt` shape
        // is not accepted by some endpoints and produced a 400 BadRequest (unknown field).
        // Keep the fallback list small to avoid triggering predictable BadRequest errors.
        val contentsBody = requestBody.toString()

        var lastNon404Error: String? = null

        for (base in bases) {
            for (method in methods) {
                val url = "$base/$MODEL_NAME:$method?key=$apiKey"

                // try the single supported 'contents/parts' body (avoid 'prompt' which some
                // endpoints reject with BadRequest). If we need additional shapes later
                // we'll add them conditionally based on model discovery.
                val attempts = listOf(contentsBody)
                for (bodyStr in attempts) {
                    val req = Request.Builder()
                        .url(url)
                        .post(bodyStr.toRequestBody("application/json".toMediaType()))
                        .build()

                    httpClient.newCall(req).execute().use { resp ->
                        val code = resp.code
                        val respBody = resp.body?.string()
                        if (resp.isSuccessful && respBody != null) {
                            val json = JSONObject(respBody)

                            // Try multiple possible shapes returned by different API versions
                            // 1) v1/v1beta with candidates -> content.parts[].text
                            json.optJSONArray("candidates")?.let { candidates ->
                                if (candidates.length() > 0) {
                                    val firstCandidate = candidates.getJSONObject(0)
                                    val partText = firstCandidate.optJSONObject("content")
                                        ?.optJSONArray("parts")
                                        ?.optJSONObject(0)
                                        ?.optString("text")
                                    if (!partText.isNullOrEmpty()) return@withContext partText

                                    val candidateOutput = firstCandidate.optString("output")
                                    if (candidateOutput.isNotEmpty()) return@withContext candidateOutput
                                }
                            }

                            val topOutput = json.optString("output")
                            if (topOutput.isNotEmpty()) return@withContext topOutput

                            json.keys().asSequence().forEach { key ->
                                val value = json.optString(key)
                                if (value.isNotEmpty()) return@withContext value
                            }

                            throw Exception("No usable text found in response: $respBody")
                        } else {
                            // Not successful. Log the response body for easier debugging and
                            // remember non-404 errors for a final error message.
                            Log.w(TAG, "Generative API non-success response. url=$url code=$code body=${respBody}")
                            if (code != 404) {
                                // prefer structured error message when present
                                lastNon404Error = respBody ?: "HTTP $code"
                            }
                            // continue trying other combinations
                        }
                    }
                }
            }
        }

        // If we reach here, no combination produced a usable response
        val err = lastNon404Error ?: "Model not found (404) for all tried endpoints"
        throw Exception("API Error after trying alternate endpoints: $err")
    }

    private fun buildMechanicSummaryPrompt(request: MechanicSummaryRequest): String {
        val recentServicesText = request.recentServices.joinToString("\n") {
            "- ${it.dateIso} @ ${it.odometerKm}km: ${it.category} - ${it.description} ($$${it.cost})"
        }
        val recentPartsText = request.recentParts.joinToString("\n") {
            "- ${it.dateIso} @ ${it.odometerKm}km: ${it.partName} (${it.reason}) ($$${it.cost})"
        }
        val activeMaintenanceText = request.activeMaintainance.joinToString("\n") { "- $it" }

        return """
You are an experienced motorcycle mechanic preparing a summary of service history for a colleague.

MOTORCYCLE PROFILE:
- Name: ${request.bikeName}
- Current Odometer: ${request.currentOdometer} km
- Purchase Date: ${request.purchaseDateIso}
- Notes: ${request.notes ?: "None"}

RECENT SERVICES (last 12-24 months):
$recentServicesText

RECENT PARTS REPLACED:
$recentPartsText

ACTIVE MAINTENANCE ITEMS:
$activeMaintenanceText

FUEL EFFICIENCY: ${request.fuelEfficiency ?: "Unknown"}

Please provide:
1. A brief one-paragraph summary suitable for sharing with a mechanic
2. Notable recent work done
3. Current concerns or potential issues
4. Recommended checks before the next service
5. Confidence level (High/Medium/Low) in your assessment

Format your response as key-value pairs (key: value), one per line.
Keep language technical but accessible.
Focus on actionable insights, not speculation.
""".trimIndent()
    }

    private fun buildPredictiveRulesPrompt(
        bikeId: String,
        bikeName: String,
        currentOdometer: Int,
        serviceHistory: List<ServiceEntry>,
        partHistory: List<PartEntry>
    ): String {
        val serviceIntervals = serviceHistory.groupBy { it.category }
            .mapValues { (_, entries) ->
                val kmBetween = entries.zipWithNext().map { (a, b) -> b.odometerKm - a.odometerKm }
                val avgKm = if (kmBetween.isNotEmpty()) kmBetween.average().toInt() else 0
                val daysBetween = entries.zipWithNext().map { (a, b) ->
                    (java.time.LocalDate.parse(b.dateIso).toEpochDay() - 
                     java.time.LocalDate.parse(a.dateIso).toEpochDay()).toInt()
                }
                val avgDays = if (daysBetween.isNotEmpty()) daysBetween.average().toInt() else 0
                "Avg $avgKm km or $avgDays days"
            }

        val partIntervals = partHistory.groupBy { it.partName }
            .mapValues { (_, entries) ->
                val kmBetween = entries.zipWithNext().map { (a, b) -> b.odometerKm - a.odometerKm }
                if (kmBetween.isNotEmpty()) "${kmBetween.average().toInt()} km" else "Unknown"
            }

        val serviceHistoryText = serviceIntervals.entries.joinToString("\n") { (cat, interval) ->
            "- $cat: $interval"
        }
        val partHistoryText = partIntervals.entries.joinToString("\n") { (part, interval) ->
            "- $part: $interval between replacements"
        }

        return """
You are a motorcycle maintenance expert analyzing service patterns to recommend maintenance intervals.

MOTORCYCLE: $bikeName (ID: $bikeId)
CURRENT ODOMETER: $currentOdometer km

SERVICE FREQUENCY HISTORY:
$serviceHistoryText

PART REPLACEMENT HISTORY:
$partHistoryText

Based on this historical data, suggest maintenance intervals for common components not yet established.
For each suggestion:
1. Component name
2. Recommended interval (in km and/or months)
3. Rationale based on the historical pattern
4. Risk if interval is skipped
5. Confidence (High/Medium/Low)

Consider:
- Typical manufacturer recommendations for this class of bike
- Accelerated wear patterns if visible
- User's actual replacement spacing

Format each suggestion as:
COMPONENT: [name]
INTERVAL: [e.g., "6000 km or 6 months"]
RATIONALE: [brief explanation]
RISK: [consequence of skipping]
CONFIDENCE: [High/Medium/Low]
---
""".trimIndent()
    }

    private fun parseMechanicSummaryResponse(text: String): MechanicSummaryResponse {
        val lines = text.split("\n").filter { it.contains(":") }
        val map = lines.associate {
            val (key, value) = it.split(":", limit = 2)
            key.trim().lowercase() to value.trim()
        }
        return MechanicSummaryResponse(
            summary = map["summary"] ?: "No summary provided.",
            recentWork = map["recent work"] ?: map["notable recent work"] ?: "No recent work noted.",
            currentConcerns = map["concerns"] ?: map["current concerns"] ?: "No immediate concerns.",
            recommendedChecks = map["checks"] ?: map["recommended checks"] ?: "Standard maintenance checks.",
            confidence = map["confidence"] ?: "Medium"
        )
    }

    private fun parsePredictiveRulesResponse(text: String): PredictiveRulesResponse {
        val suggestionTexts = text.split("---").filter { it.isNotBlank() }
        val suggestions = suggestionTexts.mapNotNull { block ->
            val lines = block.split("\n").filter { it.contains(":") }
            val map = lines.associate {
                val (key, value) = it.split(":", limit = 2)
                key.trim().lowercase() to value.trim()
            }
            if (map.containsKey("component")) {
                PredictiveRulesSuggestion(
                    component = map["component"] ?: "Unknown",
                    suggestedInterval = map["interval"] ?: "Unknown",
                    rationale = map["rationale"] ?: "No rationale provided.",
                    riskIfSkipped = map["risk"] ?: "Potential maintenance issues.",
                    confidence = map["confidence"] ?: "Medium"
                )
            } else null
        }
        return PredictiveRulesResponse(
            suggestions = suggestions,
            analysisNotes = "Based on ${suggestions.size} components analyzed."
        )
    }
}

