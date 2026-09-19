package com.cocode.linkqrwallet.ui.screens

import android.Manifest
import android.os.Handler
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import com.cocode.linkqrwallet.data.UrlSafety
import com.cocode.linkqrwallet.data.UrlUtils
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanQrScreen(
    onResult: (String) -> Unit,
    onClose: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val hasResult = remember { AtomicBoolean(false) }
    var previewView: PreviewView? by remember { mutableStateOf(null) }
    var errorMessage: String? by remember { mutableStateOf(null) }
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraProvider: ProcessCameraProvider? by remember { mutableStateOf(null) }
    val mainHandler = remember { Handler(Looper.getMainLooper()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            cameraProvider?.unbindAll()
            cameraExecutor.shutdown()
        }
    }

    LaunchedEffect(hasPermission, previewView) {
        val view = previewView ?: return@LaunchedEffect
        if (!hasPermission) return@LaunchedEffect

        val provider = getCameraProvider(context)
        cameraProvider = provider
        val preview = Preview.Builder().build().apply {
            setSurfaceProvider(view.surfaceProvider)
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(
            cameraExecutor,
            qrFrameAnalyzer(hasResult) { value ->
                if (!hasResult.compareAndSet(false, true)) return@qrFrameAnalyzer

                val normalized = UrlUtils.normalizeUrl(value)
                if (normalized == null) {
                    mainHandler.post {
                        errorMessage = "QR does not contain a valid URL."
                    }
                    mainHandler.postDelayed({
                        hasResult.set(false)
                        errorMessage = null
                    }, 1200)
                    return@qrFrameAnalyzer
                }
                val safety = UrlSafety.check(normalized)
                if (!safety.isSafe) {
                    mainHandler.post {
                        errorMessage = safety.reason ?: "Unsafe URL blocked."
                    }
                    mainHandler.postDelayed({
                        hasResult.set(false)
                        errorMessage = null
                    }, 1200)
                    return@qrFrameAnalyzer
                }
                // onResult ultimately navigates (navController.navigate requires the
                // main thread), but this whole callback runs on cameraExecutor -- the
                // compareAndSet above already claimed the one-shot result on that
                // background thread, so hopping to main here can't let a duplicate
                // frame navigate twice.
                mainHandler.post { onResult(normalized) }
            }
        )

        provider.unbindAll()
        provider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR") },
                navigationIcon = {
                    TextButton(onClick = onClose) { Text("Back") }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!hasPermission) {
                CameraPermissionRequest(
                    onRequestPermission = {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                )
            } else {
                QrPreviewOverlay(
                    errorMessage = errorMessage,
                    onPreviewViewCreated = { view -> previewView = view }
                )
            }
        }
    }
}

private suspend fun getCameraProvider(context: android.content.Context): ProcessCameraProvider {
    val future = ProcessCameraProvider.getInstance(context)
    return suspendCancellableCoroutine { cont ->
        future.addListener(
            { cont.resume(future.get()) },
            ContextCompat.getMainExecutor(context)
        )
    }
}
