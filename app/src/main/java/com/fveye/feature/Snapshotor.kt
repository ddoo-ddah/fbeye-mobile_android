package com.fveye.feature

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.hardware.SensorManager
import android.os.Build
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import com.fveye.network.ImageClient
import com.google.mlkit.vision.common.InputImage
import java.io.File


class Snapshotor(private val context: Context, private val previewView: PreviewView,
                 private val lifecycleOwner: LifecycleOwner, private val point: Point) {

    companion object {
        private var currentRotation: Int = 0
    }

    private val qrScanner = QrScanner()
    private lateinit var orientationEventListener: OrientationEventListener

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnsafeExperimentalUsageError", "RestrictedApi")
    fun startCameraWithAnalysis() {
        orientationEventListener =
                object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
                    override fun onOrientationChanged(arg0: Int) {
                        val rotation = (360 - (arg0 + 45) % 360) / 90 % 4 * 90
                        currentRotation = rotation
                    }
                }

        orientationEventListener.enable()
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

            val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(ContextCompat.getMainExecutor(context), { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, currentRotation)
                                qrScanner.detect(image)
                            }
                            imageProxy.close()
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

    fun destroy(){
        orientationEventListener.disable()
    }
}