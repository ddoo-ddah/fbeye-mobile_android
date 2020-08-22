package com.fveye.feature

import android.annotation.SuppressLint
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.fveye.network.Client
import com.google.android.gms.tasks.Task
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

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnsafeExperimentalUsageError")
    fun detect(image : InputImage): Task<MutableList<com.google.mlkit.vision.barcode.Barcode>> {

        return scanner.process(image)
    }
}