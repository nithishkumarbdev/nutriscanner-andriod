package com.nutriscanner.app.ui.result

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.nutriscanner.app.scoring.ScoreBand
import com.nutriscanner.app.ui.ScanFlowViewModel
import com.nutriscanner.app.ui.theme.ScoreColors

@Composable
fun ResultScreen(
    viewModel: ScanFlowViewModel,
    onSaved: () -> Unit,
) {
    val state by viewModel.state.collectAsState()
    val facts = state.facts ?: return
    var productLabel by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
    ) {
        Text("Extracted nutrition facts", style = MaterialTheme.typography.titleMedium)
        Text(
            "Fields ML Kit couldn't read confidently were left blank. Fill in or correct anything before scoring.",
            style = MaterialTheme.typography.bodySmall,
        )

        OutlinedTextField(
            value = productLabel,
            onValueChange = { productLabel = it },
            label = { Text("Product name (optional)") },
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        )

        NumberField("Serving size (g)", facts.servingSizeGrams) {
            viewModel.updateFacts(facts.copy(servingSizeGrams = it))
        }
        NumberField("Calories", facts.calories) {
            viewModel.updateFacts(facts.copy(calories = it))
        }
        NumberField("Total fat (g)", facts.totalFatGrams) {
            viewModel.updateFacts(facts.copy(totalFatGrams = it))
        }
        NumberField("Saturated fat (g)", facts.saturatedFatGrams) {
            viewModel.updateFacts(facts.copy(saturatedFatGrams = it))
        }
        NumberField("Sugar (g)", facts.sugarGrams) {
            viewModel.updateFacts(facts.copy(sugarGrams = it))
        }
        NumberField("Sodium (mg)", facts.sodiumMilligrams) {
            viewModel.updateFacts(facts.copy(sodiumMilligrams = it))
        }
        NumberField("Fiber (g)", facts.fiberGrams) {
            viewModel.updateFacts(facts.copy(fiberGrams = it))
        }
        NumberField("Protein (g)", facts.proteinGrams) {
            viewModel.updateFacts(facts.copy(proteinGrams = it))
        }

        Button(
            onClick = { viewModel.confirmAndScore() },
            enabled = facts.isCompleteEnoughToScore(),
            modifier = Modifier.padding(top = 16.dp),
        ) {
            Text("Compute score")
        }

        state.scoreResult?.let { result ->
            Card(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    val color = when (result.band) {
                        ScoreBand.A, ScoreBand.B -> ScoreColors.Good
                        ScoreBand.C -> ScoreColors.Mid
                        ScoreBand.D, ScoreBand.E -> ScoreColors.Poor
                    }
                    Text("Score: ${result.band}", style = MaterialTheme.typography.headlineMedium, color = color)
                    Text("Numeric: ${result.numericScore}", style = MaterialTheme.typography.bodyMedium)
                    Text("Negative points: ${result.breakdown.negativeTotal}", style = MaterialTheme.typography.bodySmall)
                    Text("Positive points: ${result.breakdown.positiveTotal}", style = MaterialTheme.typography.bodySmall)
                }
            }

            Button(
                onClick = {
                    viewModel.saveScan(productLabel.ifBlank { "Unnamed scan" })
                    onSaved()
                },
                enabled = !state.isSaving,
                modifier = Modifier.padding(top = 8.dp),
            ) {
                Text(if (state.isSaving) "Saving..." else "Save to history")
            }
        }

        state.errorMessage?.let {
            Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
        }
    }
}

/**
 * A text field bound to a nullable Double. Kept as local editable string
 * state so a partial edit like "1." doesn't get clobbered by re-parsing on
 * every keystroke; only a value that actually parses gets pushed upstream.
 */
@Composable
private fun NumberField(label: String, value: Double?, onChanged: (Double?) -> Unit) {
    var text by remember(value) { mutableStateOf(value?.toString() ?: "") }
    OutlinedTextField(
        value = text,
        onValueChange = { newText ->
            text = newText
            onChanged(newText.toDoubleOrNull())
        },
        label = { Text(label) },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
    )
}
