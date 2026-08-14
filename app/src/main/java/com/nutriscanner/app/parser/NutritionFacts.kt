package com.nutriscanner.app.parser

/**
 * Structured nutrition facts extracted from a label. All amounts are per
 * serving as printed on the label; the scorer normalizes to per-100g itself.
 * Any field the parser couldn't confidently read stays null so the result
 * screen can prompt the user to fill it in rather than showing a guess.
 */
data class NutritionFacts(
    val servingSizeGrams: Double? = null,
    val calories: Double? = null,
    val totalFatGrams: Double? = null,
    val saturatedFatGrams: Double? = null,
    val sugarGrams: Double? = null,
    val sodiumMilligrams: Double? = null,
    val fiberGrams: Double? = null,
    val proteinGrams: Double? = null,
) {
    /** True once every field the scorer needs has a value. */
    fun isCompleteEnoughToScore(): Boolean =
        servingSizeGrams != null &&
            calories != null &&
            totalFatGrams != null &&
            saturatedFatGrams != null &&
            sugarGrams != null &&
            sodiumMilligrams != null &&
            fiberGrams != null &&
            proteinGrams != null
}

/** One field the parser couldn't confidently extract, surfaced to the UI for manual entry. */
enum class NutritionField {
    SERVING_SIZE, CALORIES, TOTAL_FAT, SATURATED_FAT, SUGAR, SODIUM, FIBER, PROTEIN
}
