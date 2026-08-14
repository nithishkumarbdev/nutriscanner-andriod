package com.nutriscanner.app.ui

import android.graphics.Bitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nutriscanner.app.auth.AuthManager
import com.nutriscanner.app.data.ScanRecord
import com.nutriscanner.app.data.ScanRepository
import com.nutriscanner.app.ocr.TextRecognitionService
import com.nutriscanner.app.parser.NutritionFacts
import com.nutriscanner.app.parser.NutritionParser
import com.nutriscanner.app.scoring.HealthScorer
import com.nutriscanner.app.scoring.ScoreResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream

/** What the scan screen and result screen both observe. Held here so the extracted facts survive navigation between them. */
data class ScanFlowState(
    val isProcessingImage: Boolean = false,
    val facts: NutritionFacts? = null,
    val capturedPhoto: Bitmap? = null,
    val scoreResult: ScoreResult? = null,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
)

class ScanFlowViewModel(
    private val textRecognitionService: TextRecognitionService = TextRecognitionService(),
    private val parser: NutritionParser = NutritionParser(),
    private val scorer: HealthScorer = HealthScorer(),
    private val repository: ScanRepository = ScanRepository(),
    private val authManager: AuthManager = AuthManager(),
) : ViewModel() {

    private val _state = MutableStateFlow(ScanFlowState())
    val state: StateFlow<ScanFlowState> = _state.asStateFlow()

    fun onImageCaptured(bitmap: Bitmap) {
        _state.value = _state.value.copy(isProcessingImage = true, capturedPhoto = bitmap, errorMessage = null)
        viewModelScope.launch {
            try {
                val rawText = textRecognitionService.recognize(bitmap)
                val facts = parser.parse(rawText)
                _state.value = _state.value.copy(isProcessingImage = false, facts = facts)
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    isProcessingImage = false,
                    errorMessage = "Couldn't read that label. Try a closer, well-lit shot.",
                )
            }
        }
    }

    /** Called from the result screen when the user edits a field ML Kit missed or misread. */
    fun updateFacts(updated: NutritionFacts) {
        _state.value = _state.value.copy(facts = updated, scoreResult = null)
    }

    fun confirmAndScore() {
        val facts = _state.value.facts ?: return
        if (!facts.isCompleteEnoughToScore()) {
            _state.value = _state.value.copy(errorMessage = "Fill in every field before scoring.")
            return
        }
        val result = scorer.score(facts)
        _state.value = _state.value.copy(scoreResult = result, errorMessage = null)
    }

    fun saveScan(productLabel: String) {
        val facts = _state.value.facts ?: return
        val result = _state.value.scoreResult ?: return
        _state.value = _state.value.copy(isSaving = true)
        viewModelScope.launch {
            try {
                val uid = authManager.ensureSignedIn()
                val photoUrl = _state.value.capturedPhoto?.let { bmp ->
                    repository.uploadLabelPhoto(uid, bitmapToJpegBytes(bmp))
                }
                val record = ScanRecord(
                    productLabel = productLabel,
                    servingSizeGrams = facts.servingSizeGrams ?: 0.0,
                    calories = facts.calories ?: 0.0,
                    totalFatGrams = facts.totalFatGrams ?: 0.0,
                    saturatedFatGrams = facts.saturatedFatGrams ?: 0.0,
                    sugarGrams = facts.sugarGrams ?: 0.0,
                    sodiumMilligrams = facts.sodiumMilligrams ?: 0.0,
                    fiberGrams = facts.fiberGrams ?: 0.0,
                    proteinGrams = facts.proteinGrams ?: 0.0,
                    scoreBand = result.band.name,
                    numericScore = result.numericScore,
                    photoUrl = photoUrl,
                    scannedAtEpochMillis = System.currentTimeMillis(),
                )
                repository.saveScan(uid, record)
                _state.value = ScanFlowState() // reset for the next scan
            } catch (t: Throwable) {
                _state.value = _state.value.copy(isSaving = false, errorMessage = "Couldn't save that scan. Check your connection and try again.")
            }
        }
    }

    private fun bitmapToJpegBytes(bitmap: Bitmap): ByteArray {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
        return stream.toByteArray()
    }
}
