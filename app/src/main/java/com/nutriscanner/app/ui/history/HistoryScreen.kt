package com.nutriscanner.app.ui.history

import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nutriscanner.app.data.ScanRecord
import com.nutriscanner.app.scoring.ScoreBand
import com.nutriscanner.app.ui.theme.ScoreColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun HistoryScreen(
    onScanTapped: (ScanRecord) -> Unit,
    viewModel: HistoryViewModel = viewModel(),
) {
    val state by viewModel.state.collectAsState()

    if (state.isLoading) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
        return
    }

    if (state.scans.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                "No scans yet. Head to the scan tab to check your first label.",
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        items(state.scans, key = { it.id }) { scan ->
            ScanHistoryRow(scan, onClick = { onScanTapped(scan) })
        }
    }
}

@Composable
private fun ScanHistoryRow(scan: ScanRecord, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()) }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(scan.productLabel.ifBlank { "Unnamed scan" }, style = MaterialTheme.typography.titleMedium)
                Text(dateFormat.format(Date(scan.scannedAtEpochMillis)), style = MaterialTheme.typography.bodySmall)
            }
            val color = when (scan.band()) {
                ScoreBand.A, ScoreBand.B -> ScoreColors.Good
                ScoreBand.C -> ScoreColors.Mid
                ScoreBand.D, ScoreBand.E -> ScoreColors.Poor
            }
            Text(scan.scoreBand, style = MaterialTheme.typography.headlineSmall, color = color)
        }
    }
}
