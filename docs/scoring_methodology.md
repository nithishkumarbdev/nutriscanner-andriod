# Scoring methodology

NutriScanner's health score is an original, simplified formula. It is loosely
inspired by the general shape of the publicly documented Nutri-Score
system, negative points for things you want less of, positive points for
things you want more of, but the specific thresholds, point scale, and band
cutoffs below are our own. **This is not the official Nutri-Score algorithm
and NutriScanner is not affiliated with Nutri-Score or its maintainers.**
Real Nutri-Score also factors in fruit/vegetable/nut/legume content as a
percentage, which isn't something a nutrition facts panel exposes, so that
component doesn't exist in this formula. See `known_limitations.md` for the
full list of what this does and doesn't account for.

## Why per-100g

Serving sizes vary wildly between products, so comparing raw per-serving
numbers is misleading (a 30g serving and a 300g serving of the same
sugar-per-100g product look completely different if you only look at grams
of sugar per serving). Every nutrient is normalized to "per 100g" before
scoring:

```
perHundredGrams = amountPerServing * (100 / servingSizeGrams)
```

## Negative points (0-5 each, higher is worse)

Each nutrient below is bucketed against five ascending thresholds. The value
is compared against each threshold in order; the point count is the index of
the first threshold it's less than or equal to. A value above every
threshold scores the maximum, 5.

| Points | Calories (kcal/100g) | Sugar (g/100g) | Saturated fat (g/100g) | Sodium (mg/100g) |
|---|---|---|---|---|
| 0 | ≤ 80 | ≤ 5 | ≤ 1 | ≤ 90 |
| 1 | ≤ 160 | ≤ 9 | ≤ 2 | ≤ 180 |
| 2 | ≤ 240 | ≤ 13.5 | ≤ 3 | ≤ 270 |
| 3 | ≤ 320 | ≤ 18 | ≤ 4 | ≤ 360 |
| 4 | ≤ 400 | ≤ 22.5 | ≤ 5 | ≤ 450 |
| 5 | above 400 | above 22.5 | above 5 | above 450 |

**Reasoning for the tiers**: each column is split into five roughly even
steps up to a value that represents a genuinely calorie/sugar/fat/sodium
dense food, so a whole-food item lands in the low tiers and a
candy-or-chips-style item lands in the high tiers. The exact numbers are a
simplification, not a clinical recommendation, and are explicitly called out
in the UI as such.

`negativeTotal = caloriePoints + sugarPoints + saturatedFatPoints + sodiumPoints`
(range: 0-20)

## Positive points (0-5 each, higher is better)

| Points | Fiber (g/100g) | Protein (g/100g) |
|---|---|---|
| 0 | ≤ 0.9 | ≤ 1.6 |
| 1 | ≤ 1.9 | ≤ 3.2 |
| 2 | ≤ 2.8 | ≤ 4.8 |
| 3 | ≤ 3.7 | ≤ 6.4 |
| 4 | ≤ 4.7 | ≤ 8.0 |
| 5 | above 4.7 | above 8.0 |

`positiveTotal = fiberPoints + proteinPoints` (range: 0-10)

## Final numeric score and band

```
numericScore = negativeTotal - positiveTotal   // range roughly -10 to 20
```

| Band | Numeric score range | Meaning |
|---|---|---|
| A | ≤ -1 | Low in the tracked negatives, meaningfully high in fiber/protein |
| B | 0 to 3 | Generally favorable |
| C | 4 to 9 | Middle of the road |
| D | 10 to 14 | Leans unfavorable |
| E | ≥ 15 | High in the tracked negatives |

## Worked example

A 100g serving with 450 kcal, 55g sugar, 8g saturated fat, 380mg sodium, 1g
fiber, 2g protein:

- Calories 450 > 400 -> 5 points
- Sugar 55 > 22.5 -> 5 points
- Saturated fat 8 > 5 -> 5 points
- Sodium 380 falls in the ≤450 bucket -> 4 points
- Fiber 1 falls in the ≤1.9 bucket -> 1 point
- Protein 2 falls in the ≤3.2 bucket -> 1 point

`negativeTotal = 19`, `positiveTotal = 2`, `numericScore = 17` -> **Band E**.

This exact profile is one of the reference cases in
`HealthScorerTest.kt`, along with a favorable profile (light on the
negatives, rich in fiber and protein) that resolves to Band A.

## What this formula deliberately leaves out

- No fruit/vegetable/nut/legume percentage component (not extractable from
  an OCR'd panel).
- No product-category-specific thresholds (real Nutri-Score treats
  beverages differently from solid foods; this formula uses one threshold
  set for everything, so a very dilute product like a drink can score
  better than its sugar content alone might suggest, since calories and
  sodium per 100g are naturally low for something that's mostly water).
- Total fat is extracted and shown to the user but does not feed the score;
  only saturated fat does, consistent with the negative-points list above.
