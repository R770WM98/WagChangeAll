package com.thisismine.myapplication.ai

import android.util.Log
import com.thisismine.myapplication.data.repository.WagChangeRepository
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

data class CachedAiResult<T>(
    val data: T,
    val cachedAt: LocalDateTime,
    val expiresAt: LocalDateTime
) {
    fun isExpired(): Boolean = LocalDateTime.now() > expiresAt
}

class AiRepository(
    private val geminiClient: GeminiAiClient,
    private val wagChangeRepository: WagChangeRepository
) {
    companion object {
        private const val TAG = "AiRepository"
        private const val CACHE_EXPIRY_HOURS = 24
    }

    private val mechanicSummaryCache = mutableMapOf<String, CachedAiResult<MechanicSummaryResponse>>()
    private val predictiveRulesCache = mutableMapOf<String, CachedAiResult<PredictiveRulesResponse>>()

    suspend fun getMechanicSummary(
        motorcycleId: String,
        forceRefresh: Boolean = false
    ): Result<MechanicSummaryResponse> {
        try {
            if (!forceRefresh && isCached(mechanicSummaryCache, motorcycleId)) {
                Log.d(TAG, "Returning cached mechanic summary for $motorcycleId")
                return Result.success(mechanicSummaryCache[motorcycleId]!!.data)
            }

            // Set the active motorcycle to the requested one
            wagChangeRepository.setActiveMotorcycle(motorcycleId)

            val bike = wagChangeRepository.getProfile()
            val services = wagChangeRepository.getMaintenanceEntries()
                .map {
                    ServiceEntry(
                        dateIso = it.dateIso,
                        odometerKm = it.odometerKm,
                        category = it.title,
                        description = it.notes ?: "",
                        cost = it.totalCostPhp
                    )
                }
                .sortedByDescending { it.dateIso }
                .take(10)

            val parts = wagChangeRepository.getPartReplacements()
                .map {
                    PartEntry(
                        dateIso = it.dateIso,
                        odometerKm = it.odometerKm,
                        partName = it.partName,
                        reason = it.notes ?: "Maintenance",
                        cost = it.totalCostPhp
                    )
                }
                .sortedByDescending { it.dateIso }
                .take(10)

            val reminders = wagChangeRepository.getReminders()
                .map { it.title }

            val lastService = services.firstOrNull()

            val request = MechanicSummaryRequest(
                bikeId = motorcycleId,
                bikeName = bike.name ?: "Unnamed Bike",
                currentOdometer = bike.currentOdometerKm,
                purchaseDateIso = bike.purchaseDateIso,
                lastServiceDateIso = lastService?.dateIso,
                lastServiceOdometer = lastService?.odometerKm,
                recentServices = services,
                recentParts = parts,
                activeMaintainance = reminders,
                fuelEfficiency = null,
                notes = bike.notes
            )

            val result = geminiClient.generateMechanicSummary(request)
            if (result.isSuccess) {
                mechanicSummaryCache[motorcycleId] = CachedAiResult(
                    data = result.getOrThrow(),
                    cachedAt = LocalDateTime.now(),
                    expiresAt = LocalDateTime.now().plus(CACHE_EXPIRY_HOURS.toLong(), ChronoUnit.HOURS)
                )
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error generating mechanic summary", e)
            return Result.failure(e)
        }
    }

    suspend fun getPredictiveRules(
        motorcycleId: String,
        forceRefresh: Boolean = false
    ): Result<PredictiveRulesResponse> {
        try {
            if (!forceRefresh && isCached(predictiveRulesCache, motorcycleId)) {
                Log.d(TAG, "Returning cached predictive rules for $motorcycleId")
                return Result.success(predictiveRulesCache[motorcycleId]!!.data)
            }

            // Set the active motorcycle to the requested one
            wagChangeRepository.setActiveMotorcycle(motorcycleId)

            val bike = wagChangeRepository.getProfile()
            val services = wagChangeRepository.getMaintenanceEntries()
                .map {
                    ServiceEntry(
                        dateIso = it.dateIso,
                        odometerKm = it.odometerKm,
                        category = it.title,
                        description = it.notes ?: "",
                        cost = it.totalCostPhp
                    )
                }
                .sortedByDescending { it.odometerKm }

            val parts = wagChangeRepository.getPartReplacements()
                .map {
                    PartEntry(
                        dateIso = it.dateIso,
                        odometerKm = it.odometerKm,
                        partName = it.partName,
                        reason = it.notes ?: "Maintenance",
                        cost = it.totalCostPhp
                    )
                }
                .sortedByDescending { it.odometerKm }

            if (services.isEmpty() && parts.isEmpty()) {
                return Result.failure(Exception("Insufficient service history to generate predictions"))
            }

            val result = geminiClient.generatePredictiveRules(
                bikeId = motorcycleId,
                bikeName = bike.name ?: "Unnamed Bike",
                currentOdometer = bike.currentOdometerKm,
                serviceHistory = services,
                partHistory = parts
            )
            if (result.isSuccess) {
                predictiveRulesCache[motorcycleId] = CachedAiResult(
                    data = result.getOrThrow(),
                    cachedAt = LocalDateTime.now(),
                    expiresAt = LocalDateTime.now().plus(CACHE_EXPIRY_HOURS.toLong(), ChronoUnit.HOURS)
                )
            }
            return result
        } catch (e: Exception) {
            Log.e(TAG, "Error generating predictive rules", e)
            return Result.failure(e)
        }
    }

    fun clearCache() {
        mechanicSummaryCache.clear()
        predictiveRulesCache.clear()
        Log.d(TAG, "AI cache cleared")
    }

    private fun <T> isCached(cache: Map<String, CachedAiResult<T>>, key: String): Boolean {
        val cached = cache[key] ?: return false
        return !cached.isExpired()
    }
}

