package com.fveye.qr

import android.content.Context
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector
import java.util.*

class QrScanner(val context: Context) {

    private lateinit var barcodeDetector: BarcodeDetector

    private fun initDetector(){
        barcodeDetector = BarcodeDetector.Builder(context)
                .setBarcodeFormats(Barcode.QR_CODE)
                .build()

    }

    fun detect(frame : Frame) : Barcode{
        if(!barcodeDetector.isOperational || Objects.isNull(barcodeDetector)){
            initDetector()
        }
        val barcode = barcodeDetector.detect(frame)
        return barcode.valueAt(0)
    }
}