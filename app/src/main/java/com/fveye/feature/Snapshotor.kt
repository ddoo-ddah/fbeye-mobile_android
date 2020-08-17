package com.fveye.feature

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.media.MediaRecorder
import android.util.Log
import android.util.Size
import androidx.camera.camera2.internal.VideoCaptureConfigProvider
import androidx.camera.core.*
import androidx.camera.core.impl.VideoCaptureConfig
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File


class Snapshotor(private val context: Context, private val previewView: PreviewView,
                 private val lifecycleOwner: LifecycleOwner, private val point: Point, var file :File) {

    @SuppressLint("UnsafeExperimentalUsageError", "RestrictedApi")
    fun startCamera() {

        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                    .setTargetResolution(Size(point.x, point.y))
                    .build()
                    .also {
                        it.setSurfaceProvider(previewView.createSurfaceProvider())
                    }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

//            var outStart = System.currentTimeMillis()
            //TODO oriantation 수정, 기존꺼 안돌아감
            val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(context), { imageProxy ->
//                            var start = System.currentTimeMillis()
//                            Log.d("sendQRData", (start-outStart).toString())
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                QrScanner().detect(imageProxy)
                            }
//                            var end = System.currentTimeMillis()
//                            outStart = end

                        })
                    }

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, analysis)

            } catch (exc: Exception) {
                Log.e("TAG", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }

}