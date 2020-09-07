package xyz.fbeye.feature

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Point
import android.hardware.SensorManager
import android.os.Build
import android.os.CountDownTimer
import android.util.Log
import android.util.Size
import android.view.OrientationEventListener
import androidx.annotation.RequiresApi
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import xyz.fbeye.network.Client
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.timer
import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2

typealias faceListener = (fc: Task<List<Face>>) -> Unit

class Snapshotor(private val context: Context, private val previewView: PreviewView,
                 private val lifecycleOwner: LifecycleOwner) {

    companion object {
        private var currentRotation: Int = 0
    }

    private val qrScanner = QrScanner()
    private lateinit var orientationEventListener: OrientationEventListener
    private var setQrData: KFunction1<JSONObject, Unit>? = null
    private var isConveyed = AtomicBoolean(false)
    private var nextRotaion = 0
    private var isFirst = true
    private var times = 0
    private var timer: Timer? = null
    private var firstRotation = 0
    private var currentSize = 0
    private var stateMap : Map<String, Runnable>? = null

    fun setStateMap(map: Map<String, Runnable>){
        this.stateMap = map
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("UnsafeExperimentalUsageError", "RestrictedApi")
    fun startCameraWithAnalysis(point: Point) {
        orientationEventListener =
                object : OrientationEventListener(context, SensorManager.SENSOR_DELAY_NORMAL) {
                    override fun onOrientationChanged(arg0: Int) {
                        val rotation = (360 - (arg0 + 45) % 360) / 90 % 4 * 90
                        nextRotaion = arg0
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

            val singleExecutor = Executors.newSingleThreadExecutor()

            val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                    .also {
                        it.setAnalyzer(singleExecutor, { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val image = InputImage.fromMediaImage(mediaImage, currentRotation)
                                var result = qrScanner.detect(image)
                                result.addOnSuccessListener { barcodes ->

                                    currentSize = barcodes.size

                                    if (barcodes.size > 0) {
                                        CoroutineScope(Dispatchers.IO).launch {
                                            timerCheck(Client.getInstance()::write, "AUT", barcodes[0].displayValue.toString())

                                            if (Objects.nonNull(setQrData) && !isConveyed.get()) {
                                                setQrData!!.invoke(JSONObject(barcodes[0].displayValue.toString()))
                                                isConveyed.set(true)
                                            }
                                        }
                                    }
                                    imageProxy.close()
                                }.addOnFailureListener {
                                    imageProxy.close()
                                }
                            }
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

    private fun timerCheck(write: KFunction2<String, String, Unit>, qrIdentifier: String, data: String) {
        if (isFirst) {
            if (Objects.nonNull(stateMap)){
                stateMap!!["checking"]?.run()
            }
            firstRotation = nextRotaion
            isFirst = false
            timer = timer(period = 1000) {
//                if (currentSize <= 0) {
//                    isFirst = true
//                    times = 0
//                    stateMap!!["checkFailed"]
//                    this.cancel()
//                }
                if (firstRotation > nextRotaion + 3 || firstRotation < nextRotaion - 3) {
                    if (Objects.nonNull(stateMap)){
                        stateMap!!["checkFailed"]?.run()
                    }
                    isFirst = true
                    times = 0
                    this.cancel()
                }
                if (times == 5) {
                    if (Objects.nonNull(stateMap)){
                        stateMap!!["checkSuccess"]?.run()
                    }
                    write.invoke(qrIdentifier, data)
                    Client.getInstance().userCode = JSONObject(data).get("userCode").toString()
                    isFirst = true
                    times = 0
                    this.cancel()
                } else {
                    times++
                }
            }

        }
    }

    fun startFrontCamera(frontPreview: PreviewView) {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({

            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder()
                    .build()

            val singleExecutor = Executors.newSingleThreadExecutor()

            val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(Size(640, 480))
                    .build()
                    .also {
                        it.setAnalyzer(singleExecutor, FaceAnalyzer { fc ->
                            run {
                                fc.result?.forEach { face ->
                                    frontPreview.bitmap?.let { it1 -> EyeGazeFinder.instance.detect(face = face, photo = it1, degree = currentRotation) }
                                }
                            }
                        })
                    }

            val cameraSelector = CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_FRONT).build()

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                        lifecycleOwner, cameraSelector, preview, analysis)

                preview.setSurfaceProvider(frontPreview.createSurfaceProvider())
            } catch (exc: Exception) {
                Log.e("TAG", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(context))
    }

    fun setQrCallback(f: KFunction1<JSONObject, Unit>) {
        this.setQrData = f
    }

    fun destroy() {
        orientationEventListener.disable()
    }

    private class FaceAnalyzer(private val listener: faceListener) : ImageAnalysis.Analyzer {

        lateinit var options: FaceDetectorOptions
        lateinit var detector: FaceDetector

        var isInit: Boolean = false

        @SuppressLint("UnsafeExperimentalUsageError")
        override fun analyze(imageProxy: ImageProxy) {
            val mediaImage = imageProxy.image

            if (mediaImage != null) {
                val image =
                        InputImage.fromMediaImage(mediaImage, currentRotation)

                if (!isInit) {
                    options = FaceDetectorOptions.Builder()
                            .setClassificationMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
                            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_ALL)
                            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                            .setMinFaceSize(0.15f)
                            .enableTracking()
                            .build()
                    detector = FaceDetection.getClient(options)
                    isInit = true
                }
                val result = detector.process(image)

                result.addOnSuccessListener {
                    listener(result)
                    imageProxy.close()
                }
            }
        }
    }
}