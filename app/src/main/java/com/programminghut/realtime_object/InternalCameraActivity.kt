package com.programminghut.realtime_object

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import android.view.TextureView
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.programminghut.realtime_object.ml.SsdMobilenetV11Metadata1
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp

class InternalCameraActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView
    private lateinit var imageView: ImageView
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var handler: Handler
    private lateinit var model: SsdMobilenetV11Metadata1
    private lateinit var labels: List<String>
    private lateinit var imageProcessor: ImageProcessor
    private val paint = Paint()
    private val colors = listOf(
        Color.RED, Color.BLUE, Color.GREEN, Color.CYAN, Color.YELLOW, Color.MAGENTA
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_internal_camera)

        textureView = findViewById(R.id.textureView)
        imageView = findViewById(R.id.imageView)

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager

        // Load model & labels
        model = SsdMobilenetV11Metadata1.newInstance(this)
        labels = FileUtil.loadLabels(this, "labels.txt")
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()

        val handlerThread = HandlerThread("CameraThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: android.graphics.SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: android.graphics.SurfaceTexture): Boolean = false
            override fun onSurfaceTextureUpdated(surface: android.graphics.SurfaceTexture) {
                detectObjects(textureView.bitmap ?: return)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(cameraId: String = cameraManager.cameraIdList[0]) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            return
        }

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                val surface = Surface(textureView.surfaceTexture)
                val requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                requestBuilder.addTarget(surface)
                cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.setRepeatingRequest(requestBuilder.build(), null, null)
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Toast.makeText(applicationContext, "Camera configuration failed", Toast.LENGTH_SHORT).show()
                    }
                }, handler)
            }

            override fun onDisconnected(camera: CameraDevice) {}
            override fun onError(camera: CameraDevice, error: Int) {}
        }, handler)
    }

    private fun detectObjects(bitmap: Bitmap) {
        val tensorImage = imageProcessor.process(TensorImage.fromBitmap(bitmap))
        val outputs = model.process(tensorImage)

        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray

        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val height = bitmap.height
        val width = bitmap.width

        paint.textSize = height / 20f
        paint.strokeWidth = 4f

        scores.forEachIndexed { index, score ->
            if (score > 0.5f) {
                val x = index * 4
                val rect = RectF(
                    locations[x + 1] * width,
                    locations[x] * height,
                    locations[x + 3] * width,
                    locations[x + 2] * height
                )
                paint.color = colors[index % colors.size]
                paint.style = Paint.Style.STROKE
                canvas.drawRect(rect, paint)

                paint.style = Paint.Style.FILL
                canvas.drawText("${labels[classes[index].toInt()]} ${"%.2f".format(score)}", rect.left, rect.top, paint)
            }
        }

        imageView.setImageBitmap(mutableBitmap)
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }
}
