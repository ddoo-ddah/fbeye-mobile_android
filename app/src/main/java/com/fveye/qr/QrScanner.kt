package com.fveye.qr

import android.content.Context
import android.graphics.BitmapFactory
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.io.File
import java.util.*

class QrScanner(val context: Context, outputDirectory : File) {

    private lateinit var barcodeDetector: BarcodeDetector
    val photoFile = File(
            outputDirectory,
            "test"+".jpg")
    private fun initDetector(){
        barcodeDetector = BarcodeDetector.Builder(context)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build()

    }

    fun detect() : Barcode{
        if(!barcodeDetector.isOperational || Objects.isNull(barcodeDetector)){
            initDetector()
        }
        val bitmap = BitmapFactory.decodeFile(photoFile.absolutePath)
        val frame = Frame.Builder().setBitmap(bitmap).build()
        val barcode = barcodeDetector.detect(frame)
        return barcode.valueAt(0)
    }
}