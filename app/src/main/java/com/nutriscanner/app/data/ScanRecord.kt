package com.nutriscanner.app.data

import com.nutriscanner.app.parser.NutritionFacts
import com.nutriscanner.app.scoring.ScoreBand

/**
 * One saved scan, as stored in Firestore under users/{uid}/scans/{id}.
 * No-arg constructor and var properties are required for Firestore's
 * reflection-based POJO mapping (toObject/DocumentSnapshot.toObject).
 */
data class ScanRecord(
    var id: String = "",
    var productLabel: String = "",
    var servingSizeGrams: Double = 0.0,
    var calories: Double = 0.0,
    var totalFatGrams: Double = 0.0,
    var saturatedFatGrams: Double = 0.0,
    var sugarGrams: Double = 0.0,
    var sodiumMilligrams: Double = 0.0,
    var fiberGrams: Double = 0.0,
    var proteinGrams: Double = 0.0,
    var scoreBand: String = "",
    var numericScore: Int = 0,
    var photoUrl: String? = null,
    var scannedAtEpochMillis: Long = 0L,
) {
    fun toNutritionFacts(): NutritionFacts = NutritionFacts(
        servingSizeGrams = servingSizeGrams,
        calories = calories,
        totalFatGrams = totalFatGrams,
        saturatedFatGrams = saturatedFatGrams,
        sugarGrams = sugarGrams,
        sodiumMilligrams = sodiumMilligrams,
        fiberGrams = fiberGrams,
        proteinGrams = proteinGrams,
    )

    fun band(): ScoreBand = ScoreBand.valueOf(scoreBand)
}
