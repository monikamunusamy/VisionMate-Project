package com.programminghut.realtime_object

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.View
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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class MainActivity : AppCompatActivity() {
    private lateinit var locationManager: LocationManager
    private val locationPermissionCode = 2
    private lateinit var apiService: ApiService
    private lateinit var labels: List<String>
    private val colors = listOf(Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY, Color.BLACK, Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED)
    private val paint = Paint()
    private lateinit var imageProcessor: ImageProcessor
    private lateinit var bitmap: Bitmap
    private lateinit var imageView: ImageView
    private lateinit var cameraDevice: CameraDevice
    private lateinit var handler: Handler
    private lateinit var cameraManager: CameraManager
    private lateinit var textureView: TextureView
    private lateinit var model: SsdMobilenetV11Metadata1
    private lateinit var webView: WebView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://10.0.2.2:5000/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        getPermission()

        labels = FileUtil.loadLabels(this, "labels.txt")
        imageProcessor = ImageProcessor.Builder().add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR)).build()
        model = SsdMobilenetV11Metadata1.newInstance(this)

        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        imageView = findViewById(R.id.imageView)
        textureView = findViewById(R.id.textureView)
        webView = findViewById(R.id.webview)
        val editTextLocation = findViewById<EditText>(R.id.editText_location)

        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.visibility = View.GONE

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openCamera()
            }
            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = false
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                bitmap = textureView.bitmap!!
                detectObjects(bitmap)
            }
        }

        cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager

        findViewById<Button>(R.id.button_open_internal_camera).setOnClickListener {
            webView.visibility = View.GONE
            textureView.visibility = View.VISIBLE
            imageView.visibility = View.VISIBLE
            openCamera()
        }

        findViewById<Button>(R.id.button_open_external_camera).setOnClickListener {
            textureView.visibility = View.GONE
            imageView.visibility = View.GONE
            webView.visibility = View.VISIBLE
            webView.loadUrl("http://172.22.6.149:5000")
        }

        findViewById<Button>(R.id.button_get_coffee).setOnClickListener {
            val request = NavigationRequest("hand_position_value", "target_position_value")
            apiService.navigate(request).enqueue(object : Callback<NavigationResponse> {
                override fun onResponse(call: Call<NavigationResponse>, response: Response<NavigationResponse>) {
                    Toast.makeText(this@MainActivity, if (response.isSuccessful) "Coffee navigation success" else "Coffee navigation failed (server error)", Toast.LENGTH_SHORT).show()
                }
                override fun onFailure(call: Call<NavigationResponse>, t: Throwable) {
                    Log.e("COFFEE_API", "Error: ${t.localizedMessage}")
                    Toast.makeText(this@MainActivity, "Coffee navigation failed", Toast.LENGTH_SHORT).show()
                }
            })
        }

        findViewById<Button>(R.id.button_open_map).setOnClickListener {
            val location = editTextLocation.text.toString()
            if (location.isNotEmpty()) {
                openMapActivity(location)
            } else {
                getCurrentLocation()
            }
        }

        editTextLocation.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                val location = editTextLocation.text.toString()
                if (location.isNotEmpty()) {
                    openMapActivity(location)
                } else {
                    Toast.makeText(this, "Please enter a location", Toast.LENGTH_SHORT).show()
                }
                true
            } else {
                false
            }
        }
    }

    private fun detectObjects(bitmap: Bitmap) {
        var image = TensorImage.fromBitmap(bitmap)
        image = imageProcessor.process(image)
        val outputs = model.process(image)
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray
        val mutable = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutable)
        val h = mutable.height
        val w = mutable.width

        scores.forEachIndexed { index, score ->
            if (score > 0.5) {
                val x = index * 4
                paint.color = colors[index % colors.size]
                paint.style = Paint.Style.STROKE
                paint.strokeWidth = 4f
                canvas.drawRect(
                    RectF(locations[x + 1] * w, locations[x] * h, locations[x + 3] * w, locations[x + 2] * h), paint
                )
                paint.style = Paint.Style.FILL
                paint.textSize = h / 15f
                canvas.drawText(
                    labels[classes[index].toInt()] + " " + score.toString(),
                    locations[x + 1] * w,
                    locations[x] * h,
                    paint
                )
            }
        }
        imageView.setImageBitmap(mutable)
    }

    private fun openMapActivity(location: String) {
        val intent = Intent(this, MapsActivity::class.java)
        intent.putExtra("location", location)
        startActivity(intent)
    }

    private fun getCurrentLocation() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Location permissions are not granted", Toast.LENGTH_SHORT).show()
            return
        }
        val location: Location? = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
        if (location != null) {
            val currentLocation = "${location.latitude}, ${location.longitude}"
            openMapActivity(currentLocation)
        } else {
            Toast.makeText(this, "Unable to fetch current location", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getPermission() {
        val permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(permission), locationPermissionCode)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun openCamera() {
        cameraManager.openCamera(cameraManager.cameraIdList[0], object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                val surfaceTexture = textureView.surfaceTexture ?: return
                val surface = Surface(surfaceTexture)
                val captureRequest = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                captureRequest.addTarget(surface)
                cameraDevice.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        session.setRepeatingRequest(captureRequest.build(), null, null)
                    }
                    override fun onConfigureFailed(session: CameraCaptureSession) {}
                }, handler)
            }
            override fun onDisconnected(camera: CameraDevice) {}
            override fun onError(camera: CameraDevice, error: Int) {}
        }, handler)
    }

    override fun onDestroy() {
        model.close()
        super.onDestroy()
    }
}
