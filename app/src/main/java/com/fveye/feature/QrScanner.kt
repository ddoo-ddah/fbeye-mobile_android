package com.fveye.feature

import android.annotation.SuppressLint
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.camera.core.ImageProxy
import com.fveye.network.CoroutineClient
import com.fveye.network.ImageClient
import com.google.android.gms.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class QrScanner {

    private var options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()
    private var scanner = BarcodeScanning.getClient(options)
    private val imageClient = ImageClient()

    fun init(){
        imageClient.startClient()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnsafeExperimentalUsageError")
    fun detect(imageProxy: ImageProxy) {

        val mediaImage = imageProxy.image
        val input = InputImage.fromMediaImage(mediaImage!!, imageProxy.imageInfo.rotationDegrees)



        imageClient.write(mediaImage)

        val result = scanner.process(input)
                .addOnSuccessListener { barcodes ->

                    if (barcodes.size > 0) {
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

    fun destroy(){
        imageClient.destroy()
    }
}