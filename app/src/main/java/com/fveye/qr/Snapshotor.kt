package com.fveye.qr

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File


class Snapshotor(private val context: Context, private val preview: PreviewView,
                 private val lifecycleOwner: LifecycleOwner, private val outputDirectory: File) {


    private lateinit var imageCapture: ImageCapture

    fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                    .build()
                    .also {
                        it.setSurfaceProvider(preview.createSurfaceProvider())
                    }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            imageCapture = ImageCapture.Builder()
                    .build()

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, imageCapture)

            } catch (exc: Exception) {
                Log.e("TAG", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val photoFile = File(
                outputDirectory,
                "test" + ".jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        imageCapture.takePicture(
                outputOptions, ContextCompat.getMainExecutor(context), object : ImageCapture.OnImageSavedCallback {
            override fun onError(exc: ImageCaptureException) {
                Log.e("TAG", "Photo capture failed: ${exc.message}", exc)
            }

            override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                val savedUri = Uri.fromFile(photoFile)
                val msg = "Photo capture succeeded: $savedUri"
                Log.d("TAG", msg)
            }
        })
    }
}