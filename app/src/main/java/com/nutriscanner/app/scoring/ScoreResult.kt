package com.nutriscanner.app.scoring

/**
 * Letter band for the final score. A is best, E is worst. Thresholds are
 * defined in [HealthScorer] and documented in full in
 * docs/scoring_methodology.md. This is our own simplified banding, not the
 * official Nutri-Score scale.
 */
enum class ScoreBand { A, B, C, D, E }

/** Per-nutrient point breakdown, each already bucketed 0-5. See docs/scoring_methodology.md. */
data class ScoreBreakdown(
    val caloriePoints: Int,
    val sugarPoints: Int,
    val saturatedFatPoints: Int,
    val sodiumPoints: Int,
    val fiberPoints: Int,
    val proteinPoints: Int,
) {
    val negativeTotal: Int get() = caloriePoints + sugarPoints + saturatedFatPoints + sodiumPoints
    val positiveTotal: Int get() = fiberPoints + proteinPoints
}

data class ScoreResult(
    val band: ScoreBand,
    val numericScore: Int,
    val breakdown: ScoreBreakdown,
)
