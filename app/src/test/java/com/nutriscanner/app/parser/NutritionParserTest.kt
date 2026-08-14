package com.nutriscanner.app.parser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Test

class NutritionParserTest {

    private val parser = NutritionParser()

    @Test
    fun `parses a clean well formatted label`() {
        val text = """
            Nutrition Facts
            Serving Size 2/3 cup (55g)
            Calories 230
            Total Fat 8g
            Saturated Fat 1g
            Sodium 160mg
            Total Sugars 12g
            Dietary Fiber 4g
            Protein 3g
        """.trimIndent()

        val facts = parser.parse(text)

        assertEquals(55.0, facts.servingSizeGrams)
        assertEquals(230.0, facts.calories)
        assertEquals(8.0, facts.totalFatGrams)
        assertEquals(1.0, facts.saturatedFatGrams)
        assertEquals(160.0, facts.sodiumMilligrams)
        assertEquals(12.0, facts.sugarGrams)
        assertEquals(4.0, facts.fiberGrams)
        assertEquals(3.0, facts.proteinGrams)
        assert(facts.isCompleteEnoughToScore())
    }

    @Test
    fun `recovers values from realistic OCR noise`() {
        // Simulates common ML Kit misreads: 0 read as O, g read as 9,
        // mg read as m9, and inconsistent spacing.
        val text = """
            Nutrition Facts
            Serving Size 1 bar (4Og)
            Calories 19O
            Total Fat 79
            Sat Fat O.5g
            Sodium 95m9
            Sugars 6g
            Dietary Fibre 5g
            Protein 8g
        """.trimIndent()

        val facts = parser.parse(text)

        assertEquals(40.0, facts.servingSizeGrams)
        assertEquals(190.0, facts.calories)
        assertEquals(7.0, facts.totalFatGrams)
        assertEquals(0.5, facts.saturatedFatGrams)
        assertEquals(95.0, facts.sodiumMilligrams)
        assertEquals(6.0, facts.sugarGrams)
        assertEquals(5.0, facts.fiberGrams)
        assertEquals(8.0, facts.proteinGrams)
    }

    @Test
    fun `leaves unparseable fields blank instead of guessing`() {
        // No fat lines at all, e.g. that part of the label was cropped out
        // of frame. The parser should not invent a value for it.
        val text = """
            Nutrition Facts
            Serving Size 30g
            Calories 120
            Sodium 200mg
            Total Sugars 5g
            Dietary Fiber 3g
            Protein 4g
        """.trimIndent()

        val facts = parser.parse(text)

        assertEquals(30.0, facts.servingSizeGrams)
        assertNull(facts.totalFatGrams)
        assertNull(facts.saturatedFatGrams)
        assertFalse(facts.isCompleteEnoughToScore())
    }

    @Test
    fun `normalizes decimal commas used on some label formats`() {
        val facts = parser.parse("Total Fat 2,5g")

        assertEquals(2.5, facts.totalFatGrams)
    }

    @Test
    fun `does not let a saturated fat line also match as total fat`() {
        val text = """
            Saturated Fat 2g
            Total Fat 9g
        """.trimIndent()

        val facts = parser.parse(text)

        assertEquals(2.0, facts.saturatedFatGrams)
        assertEquals(9.0, facts.totalFatGrams)
    }
}
