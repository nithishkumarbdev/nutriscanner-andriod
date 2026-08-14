package com.nutriscanner.app.scoring

import com.nutriscanner.app.parser.NutritionFacts
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HealthScorerTest {

    private val scorer = HealthScorer()

    @Test
    fun `high sugar low fiber profile scores poorly`() {
        // Synthetic reference profile, not a real product: dense in
        // calories, sugar, saturated fat and sodium, thin on fiber/protein.
        val facts = NutritionFacts(
            servingSizeGrams = 100.0,
            calories = 450.0,
            totalFatGrams = 1.0,
            saturatedFatGrams = 8.0,
            sugarGrams = 55.0,
            sodiumMilligrams = 380.0,
            fiberGrams = 1.0,
            proteinGrams = 2.0,
        )

        val result = scorer.score(facts)

        assertEquals(17, result.numericScore)
        assertEquals(ScoreBand.E, result.band)
    }

    @Test
    fun `high fiber low sugar low sodium profile scores well`() {
        // Synthetic reference profile: light on calories, sugar, saturated
        // fat and sodium, rich in fiber and protein.
        val facts = NutritionFacts(
            servingSizeGrams = 100.0,
            calories = 90.0,
            totalFatGrams = 2.0,
            saturatedFatGrams = 0.3,
            sugarGrams = 2.0,
            sodiumMilligrams = 40.0,
            fiberGrams = 6.0,
            proteinGrams = 5.0,
        )

        val result = scorer.score(facts)

        assertEquals(-7, result.numericScore)
        assertEquals(ScoreBand.A, result.band)
    }

    @Test
    fun `normalizes per 100g against a serving size other than 100`() {
        // Same underlying density as the "scores well" case above, just
        // expressed as a 50g serving instead of 100g, to check the per-100g
        // normalization math independent of the serving size chosen.
        val facts = NutritionFacts(
            servingSizeGrams = 50.0,
            calories = 45.0,
            totalFatGrams = 1.0,
            saturatedFatGrams = 0.15,
            sugarGrams = 1.0,
            sodiumMilligrams = 20.0,
            fiberGrams = 3.0,
            proteinGrams = 2.5,
        )

        val result = scorer.score(facts)

        assertEquals(-7, result.numericScore)
        assertEquals(ScoreBand.A, result.band)
    }

    @Test
    fun `refuses to score incomplete facts`() {
        val incomplete = NutritionFacts(servingSizeGrams = 100.0, calories = 200.0)

        assertThrows(IllegalArgumentException::class.java) {
            scorer.score(incomplete)
        }
    }
}
