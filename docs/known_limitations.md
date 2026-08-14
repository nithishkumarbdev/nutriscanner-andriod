# Known limitations

Being specific here is more useful than pretending these don't exist.

## OCR / parsing

- The parser is line-based and alias-matched. A label whose fields are
  scattered across the image in a layout ML Kit reads out of the expected
  order (columns read top-to-bottom instead of left-to-right, for example)
  can still produce correct per-line matches, since matching doesn't depend
  on line order, but a label where a single field's number and unit land on
  two different OCR lines will fail to match and gets left blank.
- The `9`-as-misread-`g` correction only fires when the `9` is the very
  last character of a number (followed by whitespace or end of line). A
  genuinely correct label value that happens to end in the digit 9 with
  nothing after it (rare for these fields, but possible) would be
  incorrectly converted. This trade-off is intentional and documented in
  `NutritionParser.normalizeLine`.
- No language support beyond the aliases listed in `NutritionParser`
  (English-language US/UK-style labels, plus a couple of metric spellings
  like "fibre"). Non-English labels are not handled.
- Serving size parsing only looks for a gram amount; a label whose serving
  size is expressed purely in a non-gram unit ("1 cup", "2 pieces") with no
  gram equivalent in parentheses will leave serving size blank.

## Scoring

- See the "What this formula deliberately leaves out" section in
  `scoring_methodology.md`: no fruit/vegetable/nut percentage, no
  per-category (beverage vs. solid food) thresholds.
- The formula was validated against the synthetic reference cases in
  `HealthScorerTest.kt`, not against a real product database. It has not
  been checked against how it would rank real-world grocery items.

## Firebase

- Anonymous auth means scan history is tied to the app install, not a
  portable account. Uninstalling the app or clearing app data loses the
  anonymous identity and, with it, access to that history in the Firestore
  rules as configured here.
- No Firestore security rules file is included in this repo. Before using
  this beyond a local demo, write rules that restrict
  `users/{uid}/scans/**` reads and writes to requests where
  `request.auth.uid == uid`.

## Testing

- Unit tests cover the parser and scorer only, per the project's testing
  requirements; there's no instrumented (on-device) test suite for the
  Compose screens, CameraX capture, or Firestore integration in this
  repo. `docs/architecture.md` explains why those two modules were the
  priority for pure-logic test coverage.

## Build

- `gradle/wrapper/gradle-wrapper.jar` is not checked into this repo. It's a
  small binary bootstrap file distributed by Gradle, and it wasn't
  possible to fetch it in the environment this project was assembled in.
  Run `gradle wrapper --gradle-version 8.7` once with a local Gradle
  install to generate it before using `./gradlew`. See the Setup section
  of the README.
