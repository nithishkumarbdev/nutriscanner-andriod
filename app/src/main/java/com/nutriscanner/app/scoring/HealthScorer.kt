package com.nutriscanner.app.scoring

import com.nutriscanner.app.parser.NutritionFacts
import kotlin.math.roundToInt

/**
 * Computes a health score from parsed nutrition facts.
 *
 * This is an original, simplified formula loosely inspired by the general
 * shape of the publicly documented Nutri-Score approach (negative points for
 * calories, sugar, saturated fat, and sodium per 100g; positive points for
 * fiber and protein per 100g). It is not the official Nutri-Score algorithm,
 * is not affiliated with it, and does not include the fruit/vegetable/nut
 * content component real Nutri-Score uses, since that isn't extractable from
 * an OCR'd nutrition panel. Every threshold below is our own and is written
 * out in full in docs/scoring_methodology.md alongside the reasoning for it.
 */
class HealthScorer {

    fun score(facts: NutritionFacts): ScoreResult {
        require(facts.isCompleteEnoughToScore()) {
            "Cannot score incomplete nutrition facts; resolve blanks on the result screen first."
        }
        val servingGrams = facts.servingSizeGrams!!
        require(servingGrams > 0) { "Serving size must be greater than zero to normalize per 100g." }

        val per100 = 100.0 / servingGrams
        val caloriesPer100 = facts.calories!! * per100
        val sugarPer100 = facts.sugarGrams!! * per100
        val satFatPer100 = facts.saturatedFatGrams!! * per100
        val sodiumPer100 = facts.sodiumMilligrams!! * per100
        val fiberPer100 = facts.fiberGrams!! * per100
        val proteinPer100 = facts.proteinGrams!! * per100

        val breakdown = ScoreBreakdown(
            caloriePoints = bucket(caloriesPer100, CALORIE_THRESHOLDS),
            sugarPoints = bucket(sugarPer100, SUGAR_THRESHOLDS),
            saturatedFatPoints = bucket(satFatPer100, SATURATED_FAT_THRESHOLDS),
            sodiumPoints = bucket(sodiumPer100, SODIUM_THRESHOLDS),
            fiberPoints = bucket(fiberPer100, FIBER_THRESHOLDS),
            proteinPoints = bucket(proteinPer100, PROTEIN_THRESHOLDS),
        )

        val numericScore = breakdown.negativeTotal - breakdown.positiveTotal
        return ScoreResult(
            band = bandFor(numericScore),
            numericScore = numericScore,
            breakdown = breakdown,
        )
    }

    /**
     * Finds the first threshold the value is <= to and returns its index as
     * the point count (0-5). A value above every threshold gets the max
     * point count, `thresholds.size`.
     */
    private fun bucket(value: Double, thresholds: DoubleArray): Int {
        val rounded = (value * 100.0).roundToInt() / 100.0 // avoid float-precision edge flips at boundaries
        for (i in thresholds.indices) {
            if (rounded <= thresholds[i]) return i
        }
        return thresholds.size
    }

    private fun bandFor(numericScore: Int): ScoreBand = when {
        numericScore <= BAND_A_MAX -> ScoreBand.A
        numericScore <= BAND_B_MAX -> ScoreBand.B
        numericScore <= BAND_C_MAX -> ScoreBand.C
        numericScore <= BAND_D_MAX -> ScoreBand.D
        else -> ScoreBand.E
    }

    companion object {
        // Each array holds the upper bound for point values 0..(size-1); a
        // value above the last entry scores `size` points. Full reasoning
        // for every number here is in docs/scoring_methodology.md.
        private val CALORIE_THRESHOLDS = doubleArrayOf(80.0, 160.0, 240.0, 320.0, 400.0) // kcal/100g
        private val SUGAR_THRESHOLDS = doubleArrayOf(5.0, 9.0, 13.5, 18.0, 22.5) // g/100g
        private val SATURATED_FAT_THRESHOLDS = doubleArrayOf(1.0, 2.0, 3.0, 4.0, 5.0) // g/100g
        private val SODIUM_THRESHOLDS = doubleArrayOf(90.0, 180.0, 270.0, 360.0, 450.0) // mg/100g
        private val FIBER_THRESHOLDS = doubleArrayOf(0.9, 1.9, 2.8, 3.7, 4.7) // g/100g, higher is better
        private val PROTEIN_THRESHOLDS = doubleArrayOf(1.6, 3.2, 4.8, 6.4, 8.0) // g/100g, higher is better

        private const val BAND_A_MAX = -1
        private const val BAND_B_MAX = 3
        private const val BAND_C_MAX = 9
        private const val BAND_D_MAX = 14
    }
}
