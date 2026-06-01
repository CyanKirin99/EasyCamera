package com.example.easycamera.camera

import android.content.Context
import android.media.MediaActionSound
import android.view.ViewGroup
import androidx.camera.core.AspectRatio
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import java.io.File

@Composable
fun rememberImageCaptureState(): MutableState<ImageCapture?> {
    return remember { mutableStateOf(null) }
}

@Composable
fun CameraPreview(
    modifier: Modifier,
    imageCaptureState: MutableState<ImageCapture?>
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val previewView = remember { PreviewView(context) }
    previewView.scaleType = PreviewView.ScaleType.FILL_CENTER
    previewView.implementationMode = PreviewView.ImplementationMode.COMPATIBLE
    previewView.minimumWidth = 0
    previewView.minimumHeight = 0

    DisposableEffect(lifecycleOwner) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        val cameraReadyListener = Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val imageCapture = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setTargetAspectRatio(AspectRatio.RATIO_4_3)
                .build()
            imageCaptureState.value = imageCapture

            val cameraSelector = androidx.camera.core.CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner, cameraSelector, preview, imageCapture
                )
            } catch (e: Exception) {
                // Camera binding failed - silently handled
            }
        }
        cameraProviderFuture.addListener(cameraReadyListener, ContextCompat.getMainExecutor(context))

        onDispose {
            // ProcessCameraProvider manages its own lifecycle cleanup
        }
    }

    AndroidView(
        factory = {
            previewView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            previewView
        },
        modifier = modifier.clipToBounds()
    )
}

fun takePhoto(
    imageCapture: ImageCapture,
    context: Context,
    outputFile: File,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    outputFile.parentFile?.mkdirs()
    val outputOptions = ImageCapture.OutputFileOptions.Builder(outputFile).build()
    MediaActionSound().play(MediaActionSound.SHUTTER_CLICK)
    imageCapture.takePicture(
        outputOptions,
        ContextCompat.getMainExecutor(context),
        object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                onSuccess()
            }

            override fun onError(exception: ImageCaptureException) {
                onError("拍照失败：${exception.localizedMessage ?: "未知错误"}")
            }
        }
    )
}