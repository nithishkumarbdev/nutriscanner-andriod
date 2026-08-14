package com.nutriscanner.app.parser

/**
 * Turns raw ML Kit OCR text from a nutrition facts panel into [NutritionFacts].
 *
 * This is intentionally line-based and alias-heavy rather than a single strict
 * regex over the whole block. Real labels vary a lot in wording ("Total Fat"
 * vs "Fat Total", "Sat Fat" vs "Saturated Fat"), and OCR breaks lines and
 * characters in inconsistent ways, so matching per line against a set of
 * known aliases holds up far better than trying to model the whole panel
 * as one pattern.
 */
class NutritionParser {

    fun parse(rawText: String): NutritionFacts {
        val lines = rawText
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        var servingSize: Double? = null
        var calories: Double? = null
        var totalFat: Double? = null
        var satFat: Double? = null
        var sugar: Double? = null
        var sodium: Double? = null
        var fiber: Double? = null
        var protein: Double? = null

        for (line in lines) {
            val normalized = normalizeLine(line)

            if (servingSize == null) {
                servingSize = matchServingSize(normalized)
            }
            if (calories == null) {
                calories = matchLabeled(normalized, CALORIE_ALIASES)
            }
            if (satFat == null) {
                // Check saturated fat before total fat: "sat fat" contains "fat"
                // and would otherwise get swallowed by the total-fat alias list.
                satFat = matchLabeled(normalized, SATURATED_FAT_ALIASES)
            }
            if (totalFat == null && satFat == null) {
                totalFat = matchLabeled(normalized, TOTAL_FAT_ALIASES)
            } else if (totalFat == null) {
                // A line already claimed by sat-fat shouldn't also match total fat.
                val claimedBySatFat = SATURATED_FAT_ALIASES.any { normalized.contains(it) }
                if (!claimedBySatFat) {
                    totalFat = matchLabeled(normalized, TOTAL_FAT_ALIASES)
                }
            }
            if (sugar == null) {
                sugar = matchLabeled(normalized, SUGAR_ALIASES)
            }
            if (sodium == null) {
                sodium = matchLabeled(normalized, SODIUM_ALIASES)
            }
            if (fiber == null) {
                fiber = matchLabeled(normalized, FIBER_ALIASES)
            }
            if (protein == null) {
                protein = matchLabeled(normalized, PROTEIN_ALIASES)
            }
        }

        return NutritionFacts(
            servingSizeGrams = servingSize,
            calories = calories,
            totalFatGrams = totalFat,
            saturatedFatGrams = satFat,
            sugarGrams = sugar,
            sodiumMilligrams = sodium,
            fiberGrams = fiber,
            proteinGrams = protein,
        )
    }

    /**
     * Fixes the OCR confusions that actually show up on label scans: a
     * capital O or lowercase o inside a run of digits is a misread zero, an
     * l/I/| next to digits is a misread one, and "9" directly touching a
     * digit where a unit is expected is almost always a misread "g" (grams).
     * We only touch characters that are adjacent to digits, so this doesn't
     * mangle ordinary words elsewhere on the line.
     */
    private fun normalizeLine(line: String): String {
        var s = line.lowercase()

        // Collapse "1 2 g" -> "12g" style OCR spacing noise between digits.
        s = s.replace(Regex("(?<=\\d)\\s+(?=\\d)"), "")

        // "m9" at a word boundary is almost always a misread "mg" unit, so fix
        // it before the generic trailing-9 rule below can touch it.
        s = s.replace(Regex("m9\\b"), "mg")

        // A digit-adjacent o/O is a misread zero. Unit letters (g, mg, kcal,
        // cal) never contain "o", so this can't collide with a real unit.
        // One-sided adjacency (not requiring digits on both sides) is needed
        // to catch a misread trailing zero like "23O" -> "230".
        s = s.replace(Regex("(?<=\\d)[oO]"), "0")
        s = s.replace(Regex("[oO](?=\\d)"), "0")
        // Leading O immediately before a decimal point, e.g. "O.5g" -> "0.5g".
        s = s.replace(Regex("\\b[oO](?=\\.\\d)"), "0")

        // l/I/| -> 1 only when sandwiched by digits on both sides, so words
        // like "total" (which contains an "l") are never touched.
        s = s.replace(Regex("(?<=\\d)[lI|](?=\\d)"), "1")

        // A trailing 9 immediately after a digit, at the end of a number, is
        // almost always a misread "g" unit rather than the digit nine
        // (restricting to end-of-number position avoids corrupting ordinary
        // quantities like "9g" for nine grams elsewhere on the line).
        s = s.replace(Regex("(?<=\\d)9(?=\\s|$)"), "g")

        // Decimal commas used on some labels: "2,5g" -> "2.5g".
        s = s.replace(Regex("(?<=\\d),(?=\\d)"), ".")

        return s
    }

    private fun matchServingSize(line: String): Double? {
        if (!SERVING_SIZE_ALIASES.any { line.contains(it) }) return null
        // Prefer a gram amount in parentheses, e.g. "serving size 2/3 cup (55g)".
        PAREN_GRAMS_REGEX.find(line)?.let { return it.groupValues[1].toDoubleOrNull() }
        // Fall back to a bare "NNg" anywhere on the line.
        GRAMS_REGEX.find(line)?.let { return it.groupValues[1].toDoubleOrNull() }
        return null
    }

    private fun matchLabeled(line: String, aliases: List<String>): Double? {
        val alias = aliases.firstOrNull { line.contains(it) } ?: return null
        val afterLabel = line.substringAfter(alias)
        val match = NUMBER_WITH_UNIT_REGEX.find(afterLabel) ?: NUMBER_WITH_UNIT_REGEX.find(line)
        return match?.groupValues?.get(1)?.toDoubleOrNull()
    }

    companion object {
        private val PAREN_GRAMS_REGEX = Regex("\\(([\\d.]+)\\s*g\\)")
        private val GRAMS_REGEX = Regex("([\\d.]+)\\s*g\\b")
        private val NUMBER_WITH_UNIT_REGEX = Regex("([\\d.]+)\\s*(?:g|mg|kcal|cal)?\\b")

        private val SERVING_SIZE_ALIASES = listOf("serving size", "servings size", "per serving")
        private val CALORIE_ALIASES = listOf("calories", "calorie", "kcal", "energy")
        private val TOTAL_FAT_ALIASES = listOf("total fat", "fat total", "total fats")
        private val SATURATED_FAT_ALIASES = listOf("saturated fat", "sat fat", "sat. fat", "of which saturates")
        private val SUGAR_ALIASES = listOf("total sugars", "sugars", "sugar", "of which sugars")
        private val SODIUM_ALIASES = listOf("sodium", "salt")
        private val FIBER_ALIASES = listOf("dietary fiber", "dietary fibre", "fiber", "fibre")
        private val PROTEIN_ALIASES = listOf("protein", "proteins")
    }
}
