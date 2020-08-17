package com.fveye.feature

import android.annotation.SuppressLint
import android.util.Log
import androidx.camera.core.ImageProxy
import com.fveye.network.CoroutineClient
import com.google.android.gms.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject

class QrScanner {

    private var options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()
    private var scanner = BarcodeScanning.getClient(options)

    @SuppressLint("UnsafeExperimentalUsageError")
    fun detect(imageProxy: ImageProxy) {

        //TODO oriantation 수정, 기존꺼 안돌아감
        val mediaImage = imageProxy.image
        val input = InputImage.fromMediaImage(mediaImage!!, imageProxy.imageInfo.rotationDegrees)



        CoroutineScope(Dispatchers.IO).launch {
            CoroutineClient.getInstance().write("TES", mediaImage.toString())
        }

        val result = scanner.process(input)
                .addOnSuccessListener { barcodes ->

                    if (barcodes.size > 0){
                        CoroutineScope(Dispatchers.IO).launch {
                            CoroutineClient.getInstance().write(CoroutineClient.qrIdentifier, barcodes[0].displayValue.toString())
                        }
                    }
                    imageProxy.close()
                }
                .addOnFailureListener {
                    imageProxy.close()
                }
    }
}