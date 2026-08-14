package com.nutriscanner.app.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

/**
 * Thin wrapper around ML Kit's on-device text recognizer. This runs entirely
 * on-device: no image or extracted text ever leaves the phone during OCR,
 * since the Latin text recognizer model ships as part of the ML Kit library
 * and does not call out to a server. This is Google's model integrated
 * as-is, not something trained for this app.
 */
class TextRecognitionService {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    /** Returns the raw recognized text block, line breaks preserved, for the parser to consume. */
    suspend fun recognize(bitmap: Bitmap): String {
        val image = InputImage.fromBitmap(bitmap, 0)
        val result = recognizer.process(image).await()
        return result.text
    }
}
