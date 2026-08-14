package com.nutriscanner.app.ui.scan

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.nutriscanner.app.ui.ScanFlowViewModel
import java.util.concurrent.Executor
import java.util.concurrent.Executors

@Composable
fun ScanScreen(
    viewModel: ScanFlowViewModel,
    hasCameraPermission: Boolean,
    onRequestCameraPermission: () -> Unit,
    onFactsReady: () -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val imageCapture = remember { ImageCapture.Builder().build() }
    val cameraExecutor: Executor = remember { Executors.newSingleThreadExecutor() }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        uri?.let {
            val bitmap = context.contentResolver.openInputStream(it)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            }
            bitmap?.let(viewModel::onImageCaptured)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (hasCameraPermission) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        cameraProvider.unbindAll()
                        cameraProvider.bindToLifecycle(
                            ctx as androidx.lifecycle.LifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview,
                            imageCapture,
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
            )
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(24.dp),
        ) {
            if (state.isProcessingImage) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Reading label...", color = MaterialTheme.colorScheme.onSurface)
            } else {
                if (hasCameraPermission) {
                    Button(onClick = {
                        capturePhoto(imageCapture, cameraExecutor) { bitmap ->
                            viewModel.onImageCaptured(bitmap)
                        }
                    }) {
                        Text("Capture label")
                    }
                } else {
                    Text(
                        "Camera access is off, so live capture isn't available. You can still import a label photo below.",
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    OutlinedButton(
                        onClick = onRequestCameraPermission,
                        modifier = Modifier.padding(top = 8.dp),
                    ) {
                        Text("Enable camera")
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }) {
                    Text("Choose from gallery")
                }
            }

            state.errorMessage?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    // Once OCR + parsing finish, hand off to the result screen.
    if (state.facts != null && !state.isProcessingImage) {
        onFactsReady()
    }
}

private fun capturePhoto(
    imageCapture: ImageCapture,
    executor: Executor,
    onCaptured: (Bitmap) -> Unit,
) {
    imageCapture.takePicture(
        executor,
        object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                val bitmap = image.toBitmap()
                image.close()
                onCaptured(bitmap)
            }

            override fun onError(exception: ImageCaptureException) {
                // Surfaced to the user via the view model's error state on the next
                // gallery/camera attempt; nothing to clean up here.
            }
        },
    )
}

/** ImageProxy -> Bitmap for the common JPEG capture format used by ImageCapture.OnImageCapturedCallback. */
private fun ImageProxy.toBitmap(): Bitmap {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
}
