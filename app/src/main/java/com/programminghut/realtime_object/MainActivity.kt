package com.programminghut.realtime_object

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.hardware.camera2.*
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.Surface
import android.view.TextureView
import android.view.inputmethod.EditorInfo
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
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
import java.util.*

class MainActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var textToSpeech: TextToSpeech
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
    private lateinit var locationManager: LocationManager
    private val paint = Paint()

    private val colors = listOf(
        Color.BLUE, Color.GREEN, Color.RED, Color.CYAN, Color.GRAY,
        Color.BLACK, Color.DKGRAY, Color.MAGENTA, Color.YELLOW, Color.RED
    )

    private val voiceInputLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK && result.data != null) {
            val spokenText = result.data!!.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)
            spokenText?.let { handleVoiceCommand(it) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val retrofit = Retrofit.Builder()
            .baseUrl("http://172.22.3.138:8000")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        apiService = retrofit.create(ApiService::class.java)

        textToSpeech = TextToSpeech(this, this)

        getPermissions()
        labels = FileUtil.loadLabels(this, "labels.txt")
        model = SsdMobilenetV11Metadata1.newInstance(this)
        imageProcessor = ImageProcessor.Builder()
            .add(ResizeOp(300, 300, ResizeOp.ResizeMethod.BILINEAR))
            .build()

        val handlerThread = HandlerThread("videoThread")
        handlerThread.start()
        handler = Handler(handlerThread.looper)

        textureView = findViewById(R.id.textureView)
        imageView = findViewById(R.id.imageView)
        webView = findViewById(R.id.webView)
        editTextLocation = findViewById(R.id.editText_location)
        val btnVoiceInput: Button = findViewById(R.id.button_voice_input)

        webView.webViewClient = WebViewClient()
        webView.settings.javaScriptEnabled = true
        webView.visibility = WebView.GONE

        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                openInternalCamera()
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
            openInternalCamera()
        }

        findViewById<Button>(R.id.button_open_external_camera).setOnClickListener {
            textureView.visibility = TextureView.GONE
            imageView.visibility = ImageView.GONE
            webView.visibility = WebView.VISIBLE
            webView.loadUrl("http://${getDeviceIpAddress()}:8000/video_feed")
        }

        findViewById<Button>(R.id.button_get_coffee).setOnClickListener {
            onGetCoffeeButtonClick()
        }

        findViewById<Button>(R.id.button_open_map).setOnClickListener {
            openMap()
        }

        editTextLocation.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                openMap()
                true
            } else false
        }

        btnVoiceInput.setOnClickListener {
            startVoiceInput()
        }
    }

    private fun openMap() {
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
            textToSpeech.speak("Please say the location you want to navigate to.", TextToSpeech.QUEUE_FLUSH, null, null)
            startVoiceInput()
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale.US
            textToSpeech.speak("Welcome. You are on the main screen. Use buttons or voice to proceed.", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your command or destination")
        }
        voiceInputLauncher.launch(intent)
    }

    private fun handleVoiceCommand(command: String) {
        when {
            command.contains("navigate to", true) -> {
                val location = command.substringAfter("navigate to").trim()
                if (location.isNotEmpty()) {
                    editTextLocation.setText(location)
                    openMap()
                } else {
                    textToSpeech.speak("Please say a valid location.", TextToSpeech.QUEUE_FLUSH, null, null)
                }
            }
            command.contains("open camera", true) || command.contains("internal camera", true) -> {
                webView.visibility = WebView.GONE
                textureView.visibility = TextureView.VISIBLE
                imageView.visibility = ImageView.VISIBLE
                openInternalCamera()
            }
            command.contains("external camera", true) -> findViewById<Button>(R.id.button_open_external_camera).performClick()
            command.contains("get coffee", true) -> findViewById<Button>(R.id.button_get_coffee).performClick()
            command.contains("open map", true) || command.contains("navigation", true) -> openMap()
            else -> textToSpeech.speak("Sorry, I didn't get that.", TextToSpeech.QUEUE_FLUSH, null, null)
        }
    }

    @SuppressLint("MissingPermission")
    private fun openInternalCamera(cameraId: String = cameraManager.cameraIdList[0]) {
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
                        Toast.makeText(applicationContext, "Camera configuration failed", Toast.LENGTH_SHORT).show()
                    }
                }, handler)
            }
            override fun onDisconnected(camera: CameraDevice) {}
            override fun onError(camera: CameraDevice, error: Int) {}
        }, handler)
    }

    private fun detectObjects(bitmap: Bitmap) {
        val image = TensorImage.fromBitmap(bitmap)
        val processedImage = imageProcessor.process(image)
        val outputs = model.process(processedImage)
        val locations = outputs.locationsAsTensorBuffer.floatArray
        val classes = outputs.classesAsTensorBuffer.floatArray
        val scores = outputs.scoresAsTensorBuffer.floatArray

        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val height = mutableBitmap.height
        val width = mutableBitmap.width

        scores.forEachIndexed { index, score ->
            if (score > 0.5) {
                val x = index * 4
                paint.style = Paint.Style.STROKE
                paint.color = colors[index % colors.size]
                paint.strokeWidth = 4f
                canvas.drawRect(
                    RectF(locations[x + 1] * width, locations[x] * height, locations[x + 3] * width, locations[x + 2] * height), paint
                )
                paint.style = Paint.Style.FILL
                paint.textSize = height / 15f
                canvas.drawText("${labels[classes[index].toInt()]} $score", locations[x + 1] * width, locations[x] * height, paint)
            }
        }

        imageView.setImageBitmap(mutableBitmap)
    }

    private fun onGetCoffeeButtonClick() {
        val request = NavigationRequest("hand_position_value", "target_position_value")
        apiService.navigate(request).enqueue(object : Callback<NavigationResponse> {
            override fun onResponse(call: Call<NavigationResponse>, response: Response<NavigationResponse>) {
                if (response.isSuccessful) {
                    Toast.makeText(this@MainActivity, "Coffee navigation success", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, "Coffee navigation failed (server error)", Toast.LENGTH_SHORT).show()
                }
            }
            override fun onFailure(call: Call<NavigationResponse>, t: Throwable) {
                Log.e("COFFEE_API", "Error: ${t.localizedMessage}")
                Toast.makeText(this@MainActivity, "Coffee navigation failed", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun getPermissions() {
        val permissions = listOf(
            Manifest.permission.CAMERA,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.RECORD_AUDIO
        )
        val missing = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missing.toTypedArray(), 101)
        }
    }

    private fun getDeviceIpAddress(): String {
        return "172.22.3.138"
    }

    override fun onDestroy() {
        super.onDestroy()
        model.close()
        textToSpeech.shutdown()
    }
}