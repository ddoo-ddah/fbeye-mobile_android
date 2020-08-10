package com.fveye.feature

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.io.File
import java.util.*

class QrScanner(val context: Context, outputDirectory: File) {

    private val photoFile = File(
            outputDirectory,
            "qrPhoto" + ".jpg")
    private var options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()
    private var scanner = BarcodeScanning.getClient(options)

    fun getImage() : InputImage{
        return InputImage.fromFilePath(context, Uri.fromFile(photoFile))
    }

    fun detect() {
        var image = getImage()
        if (Objects.isNull(image)){
            image = getImage()
        }
        val result = scanner.process(image).addOnSuccessListener {
            Log.d("Detect Result", it[0].displayValue.toString())
        }
        photoFile.delete()
    }
}