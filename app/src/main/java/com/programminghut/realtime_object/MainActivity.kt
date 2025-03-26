package com.programminghut.realtime_object

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.programminghut.realtime_object.ml.SsdMobilenetV11Metadata1
import com.programminghut.realtime_object.network.ApiService
import com.programminghut.realtime_object.network.NavigationRequest
import com.programminghut.realtime_object.network.NavigationResponse
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {

    private lateinit var apiService: ApiService
    private lateinit var cameraManager: CameraManager
    private lateinit var cameraDevice: CameraDevice
    private lateinit var handler: Handler
    private lateinit var textureView: TextureView
    private lateinit var imageView: ImageView
    private lateinit var model: SsdMobilenetV11Metadata1
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var labels: List<String>
    private lateinit var webView: WebView
    private lateinit var editTextLocation: EditText
    private val paint = Paint()
    private lateinit var locationManager: LocationManager

    private val colors = listOf(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY,
        Color.BLACK, Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Retrofit setup
        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        // Setup
        getPermissions()
        labels = FileUtil.loadLabels(this, "labels.txt")
        model = SsdMobilenetV11Metadata1.newInstance(this)
        imageProcessor = ImageProcessor.Builder().add(
            ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)
        ).build()

        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        // Views
        textureView = findViewById(R.id.textureView)
        imageView = findViewById(R.id.imageView)
        webView = findViewById(R.id.webview)
        editTextLocation = findViewById(R.id.editText_location)
        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.visibility = WebView.GONE

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera()
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                textureView.bitmap?.let { detectObjects(it) }
            }
        }

        findViewById<Button>(R.id.button_open_internal_camera).setOnClickListener {
            webView.visibility = WebView.GONE
            textureView.visibility = TextureView.VISIBLE
            imageView.visibility = ImageView.VISIBLE
            openCamera()
        }

        findViewById<Button>(R.id.button_open_external_camera).setOnClickListener {
            textureView.visibility = TextureView.GONE
            imageView.visibility = ImageView.GONE
            webView.visibility = WebView.VISIBLE
            webView.loadUrl("http://172.22.6.149:5000")
        }

        findViewById<Button>(R.id.button_get_coffee).setOnClickListener {
            val request = NavigationRequest("hand_position_value", "target_position_value")
            apiService.navigate(request).enqueue(object : Callback<NavigationResponse> {
                override fun onResponse(call: Call<NavigationResponse>, response: Response<NavigationResponse>) {
                    Toast.makeText(this@MainActivity, if (response.isSuccessful) "Coffee navigation success" else "Coffee failed (server error)", Toast.LENGTH_SHORT).show()
                }
                override fun onFailure(call: Call<NavigationResponse>, t: Throwable) {
                    Toast.makeText(this@MainActivity, "Coffee navigation failed", Toast.LENGTH_SHORT).show()
                }
            })
        }

        findViewById<Button>(R.id.button_open_map).setOnClickListener {
            val location = editTextLocation.text.toString()
            if (location.isNotEmpty()) {
                val gmmIntentUri = Uri.parse("google.navigation:q=$location")
                val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
                mapIntent.setPackage("com.google.android.apps.maps")
                if (mapIntent.resolveActivity(packageManager) != null) {
                    startActivity(mapIntent)
                } else {
                    Toast.makeText(this, "Google Maps not installed", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Enter a location first", Toast.LENGTH_SHORT).show()
            }
        }

        editTextLocation.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                findViewById<Button>(R.id.button_open_map).performClick()
                true
            } else false
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera(cameraId: String = cameraManager.cameraIdList[0]) {
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                val surfaceTexture = textureView.surfaceTexture ?: return
                val surface = Surface(surfaceTexture)
                val requestBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                requestBuilder.addTarget(surface)
                cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.setRepeatingRequest(requestBuilder.build(), null, null)
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        Toast.makeText(applicationContext, "Camera config failed", Toast.LENGTH_SHORT).show()
                    }
                }, handler)
            }
            override fun onDisconnected(camera: CameraDevice) {}
            override fun onError(camera: CameraDevice, error: Int) {}
        }, handler)
    }

    private fun detectObjects(bitmap: Bitmap) {
        var image = TensorImage.fromBitmap(bitmap)
        image = imageProcessor.process(image)
        val outputs = model.process(image)
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray

        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val h = mutableBitmap.height
        val w = mutableBitmap.width

        scores.forEachIndexed { index, score ->
            if (score > 0.5) {
                val x = index * 4
                paint.style = Paint.Style.STROKE
                paint.color = colors[index % colors.size]
                paint.strokeWidth = 4f
                canvas.drawRect(
                    RectF(locations[x + 1] * w, locations[x] * h, locations[x + 3] * w, locations[x + 2] * h), paint
                )
                paint.style = Paint.Style.FILL
                paint.textSize = h / 15f
                canvas.drawText("${labels[classes[index].toInt()]} $score", locations[x + 1] * w, locations[x] * h, paint)
            }
        }

        imageView.setImageBitmap(mutableBitmap)
    }

    private fun getPermissions() {
        val permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
    }
}
