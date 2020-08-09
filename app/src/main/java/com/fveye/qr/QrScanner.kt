package com.fveye.qr

import android.content.Context
import android.graphics.BitmapFactory
import android.util.Log
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.File
import java.util.*

class QrScanner(val context: Context, outputDirectory : File) {

    private var barcodeDetector: BarcodeDetector = BarcodeDetector.Builder(context)
            .setBarcodeFormats(Barcode.QR_CODE)
            .build()
    val photoFile = File(
            outputDirectory,
            "qrPhoto"+".jpg")

    fun detect() : Barcode?{

        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        val frame = Frame.Builder().setBitmap(bitmap).build()
        val barcode = barcodeDetector.detect(frame)
        if(barcode.size() > 0){
            Log.d("Detect Success", barcode.valueAt(0).displayValue)
            return barcode.valueAt(0)
        }
        return null
    }
}