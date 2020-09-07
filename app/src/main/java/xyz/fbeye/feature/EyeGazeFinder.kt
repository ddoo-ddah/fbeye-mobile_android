package xyz.fbeye.feature

import android.content.res.AssetFileDescriptor
import android.graphics.*
import android.util.Log
import com.google.mlkit.vision.face.Face
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgproc.Imgproc
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.gpu.GpuDelegate
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.*
import kotlin.reflect.KFunction1

class EyeGazeFinder private constructor() {

    //make destroy
    //얼굴인식 똑바로 되는지 체크

    private lateinit var interpreter : Interpreter
    private val gpuDelegate = GpuDelegate()
    private val option = Interpreter.Options().addDelegate(gpuDelegate)

    private var processedBitmap : Bitmap? = null

    var requestBitmap = false

    private lateinit var leftPositions :List<Pair<Float, Float>>
    private lateinit var rightPositions :List<Pair<Float, Float>>

    private val imageProcessor: ImageProcessor = ImageProcessor.Builder()
        .add(ResizeOp(108,180, ResizeOp.ResizeMethod.BILINEAR))
        .build()

    private val rotateMatrix = Matrix()

    private lateinit var executor : ExecutorService

    private lateinit var bitmapWriter : KFunction1<@ParameterName(name = "bitmap") Bitmap, Unit>
    private lateinit var eyeWriter : KFunction1<MutableList<Float>, Unit>

    fun init(fileDescriptor : AssetFileDescriptor){
        val channel = FileInputStream(fileDescriptor.fileDescriptor).channel
        interpreter = Interpreter(channel.map(FileChannel.MapMode.READ_ONLY,fileDescriptor.startOffset, fileDescriptor.declaredLength), option)
        OpenCVLoader.initDebug()
        executor = Executors.newCachedThreadPool()
        rotateMatrix.postRotate(180.0f)
    }

    fun getProcessedBitmap() : Bitmap?{
        return processedBitmap
    }

    fun detect(face: Face, photo: Bitmap, degree: Int){

        var photo1 : Bitmap = photo
        if(degree == 90){
            photo1 = Bitmap.createBitmap(photo,0,0,photo.width, photo.height, rotateMatrix, true)
        }

        val imageRatio = 640f / 480f
        val viewRatio =
            photo1.width.toFloat() / photo1.height.toFloat()

        val scaleFactor: Float
        var scaleWidthOffset = 0f
        var scaleHeightOffset = 0f

        if (imageRatio < viewRatio) {
            scaleFactor = photo1.width.toFloat() / 480f
            scaleHeightOffset =
                (photo1.width.toFloat() / imageRatio - photo1.height.toFloat()) / 2
        } else {
            scaleFactor = photo1.height.toFloat() / 640f
            scaleWidthOffset =
                (photo1.height.toFloat() / imageRatio - photo1.width.toFloat()) / 2
        }

        var leftEyeLeft = Float.MAX_VALUE
        var leftEyeTop = Float.MAX_VALUE
        var leftEyeRight = 0f
        var leftEyeBottom = 0f

        var rightEyeLeft = Float.MAX_VALUE
        var rightEyeTop = Float.MAX_VALUE
        var rightEyeRight = 0f
        var rightEyeBottom = 0f

        face.allContours.forEach {
            kotlin.run {
                //0,4,8,12
                //right eye
                if (it.faceContourType == 6) {
                    it.points.forEach {
                        val cx =
                            photo1.width - (it.x * scaleFactor - scaleWidthOffset)
                        val cy = it.y * scaleFactor - scaleHeightOffset

                        if (cx < rightEyeLeft) {
                            rightEyeLeft = cx
                        }
                        if (cx > rightEyeRight) {
                            rightEyeRight = cx
                        }
                        if (cy < rightEyeTop) {
                            rightEyeTop = cy
                        }
                        if (cy > rightEyeBottom) {
                            rightEyeBottom = cy
                        }
                    }
                }
                //left eye
                else if (it.faceContourType == 7) {
                    it.points.forEach {
                        val cx =
                            photo1.width - (it.x * scaleFactor - scaleWidthOffset)
                        val cy = it.y * scaleFactor - scaleHeightOffset

                        if (cx < leftEyeLeft) {
                            leftEyeLeft = cx
                        }
                        if (cx > leftEyeRight) {
                            leftEyeRight = cx
                        }
                        if (cy < leftEyeTop) {
                            leftEyeTop = cy
                        }
                        if (cy > leftEyeBottom) {
                            leftEyeBottom = cy
                        }
                    }
                }
            }
        }
        val leftEyeBallRadius = (leftEyeRight - leftEyeLeft) * 0.55f
        val rightEyeBallRadius = (rightEyeRight - rightEyeLeft) * 0.55f
        executor.execute{
            val tleftPositions = processEye(photo1, leftEyeLeft,leftEyeTop-20, (leftEyeRight - leftEyeLeft)+20,(leftEyeBottom - leftEyeTop)*1.3f+30)
            val trightPositions = processEye(photo1,rightEyeLeft-20,rightEyeTop-20, (rightEyeRight - rightEyeLeft)+40,(rightEyeBottom - rightEyeTop)*1.3f + 30)

            if (tleftPositions == null || trightPositions == null) {
                processedBitmap = photo1
                return@execute
            }

            leftPositions = tleftPositions
            rightPositions = trightPositions


            val leftEyeDegree = calculateEyeDegree(leftPositions, leftEyeBallRadius)
            val rightEyeDegree = calculateEyeDegree(rightPositions, rightEyeBallRadius)
//            Log.e("eyeL",leftEyeDegree.toString())
//            Log.e("eyeR",rightEyeDegree.toString())

            val leftSize = Rect()
            leftSize.set(leftEyeLeft.toInt(),
                (leftEyeTop - 20).toInt(), leftEyeRight.toInt(), leftEyeBottom.toInt()
            )

            val rightSize = Rect()
            rightSize.set((rightEyeLeft - 20).toInt(),
                (rightEyeTop-20).toInt(), rightEyeRight.toInt(), rightEyeBottom.toInt()
            )

            ArrayList<Float>()
                .also {
                    it.add(leftEyeDegree.first)
                    it.add(leftEyeDegree.second)
                    it.add(rightEyeDegree.first)
                    it.add(rightEyeDegree.second)
                    it.add(face.headEulerAngleX)
                    it.add(face.headEulerAngleY)
                    it.add(face.headEulerAngleZ)
                    if(!it.contains(Float.NaN)){
                        eyeWriter.invoke(it)
                    }
            }

            if(requestBitmap){
                processBitmap(photo, leftPositions, rightPositions, leftSize, rightSize)
                processedBitmap?.let { bitmapWriter.invoke(it) }
            }
        }
    }

    private fun processBitmap(bitmap: Bitmap, leftPositions: List<Pair<Float, Float>> , rightPositions: List<Pair<Float, Float>>, leftSize : Rect, rightSize : Rect ){

        val canvas = Canvas(bitmap)

        //width 180 height 108

        val leftWidthRatio = ((leftSize.right - leftSize.left)+20) / 180.0f
        val leftHeightRatio = ((leftSize.bottom - leftSize.top)*1.3f+30) / 108.0f

        val rightWidthRatio = ((rightSize.right - rightSize.left)+40) / 180.0f
        val rightHeightRatio = ((rightSize.bottom - rightSize.top)*1.3f+30) / 108.0f
        val paint = Paint()
        paint.color = Color.RED

        val paint2 = Paint()
        paint2.color = Color.GREEN

        for (i in 8 until 18){

            canvas.drawCircle(leftPositions[i].first * leftWidthRatio + leftSize.left,leftPositions[i].second * leftHeightRatio + leftSize.top,5.0f,paint)
            canvas.drawCircle(rightPositions[i].first * rightWidthRatio + rightSize.left, rightPositions[i].second * rightHeightRatio+rightSize.top,5.0f, paint)
        }

        val irisCenterX = leftPositions[16].first * leftWidthRatio + leftSize.left
        val irisCenterY = leftPositions[16].second * leftHeightRatio + leftSize.top

        val eyeBallCenterX = leftPositions[17].first * leftWidthRatio + leftSize.left
        val eyeBallCenterY = leftPositions[17].second * leftHeightRatio + leftSize.top

        val drawX = (irisCenterX - eyeBallCenterX)*1.2f
        val drawY = (irisCenterY - eyeBallCenterY)*1.2f

        canvas.drawLine(eyeBallCenterX, eyeBallCenterY, eyeBallCenterX + drawX, eyeBallCenterY + drawY, paint2)

        processedBitmap = bitmap
    }

    private fun calculateEyeDegree(positions : List<Pair<Float, Float>>, radius : Float ): Pair<Float, Float> {
        //Head z orientation apply.
        //calculate euler angle
        //8~15 iris
        //16 iris center
        //17 eyeball center
        //x 위+ 아래-
        //y 왼쪽+ 오른쪽-
        //test all iris contours, apply root square

        var ttheta = 0.0
        var tphi = 0.0
        var count = 0

        for(i in 8..16){
            val cx = positions[i].first - positions[17].first
            val cy = positions[i].second - positions[17].second

            val theta = asin(cx/radius).toDouble()

            val phi = asin(cy/(radius * -cos(theta)))
            if(!theta.isNaN() && !phi.isNaN()){
                ttheta += theta
                tphi += phi
                count += 1
            }
        }

        ttheta /= count
        tphi /= count
//        if(count == 8)
        Log.e("degree", "t : ${Math.toDegrees(ttheta).toFloat()}, p : ${Math.toDegrees(tphi).toFloat()}")

//        return Pair(Math.toDegrees(ttheta).toFloat() - headAngle.second/2, Math.toDegrees(tphi).toFloat() + headAngle.first/2)
        return Pair(Math.toDegrees(ttheta).toFloat(), Math.toDegrees(tphi).toFloat())

    }

    private fun processEye(photo:Bitmap, eyeLeft:Float, eyeTop : Float, width:Float, height:Float): ArrayList<Pair<Float, Float>>? {

        val eyeBitmap : Bitmap
        try {
            eyeBitmap = Bitmap.createBitmap(
                photo,
                eyeLeft.roundToInt(),
                eyeTop.roundToInt(),
                width.roundToInt(),
                height.roundToInt()
            )
        }catch(e :IllegalArgumentException){
            Log.e("EyeProcess", "")
            return null
        }

        val rMat = Mat(eyeBitmap.width, eyeBitmap.height, CvType.CV_8UC1)
        Utils.bitmapToMat(eyeBitmap, rMat)
        val gMat = Mat()
        Imgproc.cvtColor(rMat,gMat, Imgproc.COLOR_BGRA2GRAY)
        Imgproc.equalizeHist(gMat, gMat)
        val bEye = Bitmap.createBitmap(eyeBitmap)
        Utils.matToBitmap(gMat,bEye)

        val eyeImage = TensorImage(DataType.FLOAT32)
        eyeImage.load(bEye)
        imageProcessor.process(eyeImage)

        val output = TensorBuffer.createFixedSize(interpreter.getOutputTensor(0).shape(), DataType.FLOAT32)
        interpreter.run(
            convertBitmapToByteBuffer(eyeImage.bitmap),
            output.buffer
        )

        val heatMap = output.floatArray.toList().withIndex().groupBy { it.index%18 }

        val positions = ArrayList<Pair<Float,Float>>()

        for(i in 0 until 18){
            val contPos = getMaxResult(heatMap[i]);
            positions.add(Pair((contPos%60)*3.0f, contPos/20.0f))
        }
        return positions
    }

    private fun convertBitmapToByteBuffer(grayScaleImage: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(4*108*180)
        byteBuffer.order(ByteOrder.nativeOrder())

        val pixels = IntArray(180 * 108)
        grayScaleImage.getPixels(pixels, 0, grayScaleImage.width, 0, 0, grayScaleImage.width, grayScaleImage.height)
        var pixel = 0
        for (i in 0 until 180) {
            for (j in 0 until 108) {
                val pixelVal = pixels[pixel++]
                byteBuffer.putFloat(((pixelVal shr 16 and 0xFF) - 127.5f) / 127.5f)
            }
        }
        return byteBuffer
    }

    private fun getMaxResult(result: List<IndexedValue<Float>>?): Int {
        var probability = result?.get(0)?.value
        var index = 0
        for (i in result?.indices!!) {
            if (probability!! < result[i].value) {
                probability = result[i].value
                index = i
            }
        }
        return index
    }

    fun setBitmapWriter(writer: KFunction1<@ParameterName(name = "bitmap") Bitmap, Unit>){
        this.bitmapWriter = writer
    }

    fun setEyeDataWriter(writer: KFunction1<MutableList<Float>, Unit>) {
        this.eyeWriter = writer;
    }

    companion object{
        val instance : EyeGazeFinder = EyeGazeFinder()
    }

}