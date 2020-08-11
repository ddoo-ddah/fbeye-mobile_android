package com.fveye.feature

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageProxy
import com.google.android.gms.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class QrScanner {

    private var options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()
    private var scanner = BarcodeScanning.getClient(options)

    @SuppressLint("UnsafeExperimentalUsageError")
    fun detect(imageProxy: ImageProxy) {

        val mediaImage = imageProxy.image
        val input = InputImage.fromMediaImage(mediaImage!!, imageProxy.imageInfo.rotationDegrees)

        val result = scanner.process(input)
                .addOnSuccessListener { barcodes ->
                    //DoSomeThing
                    if (barcodes.size > 0){
                        Log.d("Qr data", barcodes[0].displayValue.toString())
                    }
                    imageProxy.close()
                }
                .addOnFailureListener {
                    imageProxy.close()
                }
    }
}