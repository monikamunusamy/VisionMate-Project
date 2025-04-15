package com.programminghut.realtime_object

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.*
import java.util.concurrent.Executors
import android.speech.tts.TextToSpeech

class BarcodeActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private val executor = Executors.newSingleThreadExecutor()
    private lateinit var tts: TextToSpeech
    private var lastScannedCode: String? = null  // prevent repeat reads

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_barcode)

        previewView = findViewById(R.id.previewView)

        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
            }
        }

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val barcodeScanner = BarcodeScanning.getClient()
            val analysis = ImageAnalysis.Builder().build().also {
                it.setAnalyzer(executor) { imageProxy ->
                    processImageProxy(barcodeScanner, imageProxy)
                }
            }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(this, cameraSelector, preview, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.camera.core.ExperimentalGetImage
    private fun processImageProxy(scanner: com.google.mlkit.vision.barcode.BarcodeScanner, imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(image)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val raw = barcode.rawValue ?: continue

                        // Avoid repeating the same barcode
                        if (raw != lastScannedCode) {
                            lastScannedCode = raw
                            Log.d("BARCODE", "Detected: $raw")
                            fetchProductInfo(raw)
                        }
                    }
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun fetchProductInfo(barcode: String) {
        val client = OkHttpClient()
        val request = Request.Builder()
            .url("https://world.openfoodfacts.org/api/v0/product/$barcode.json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    speak("Failed to fetch product information.")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                try {
                    val json = JSONObject(body ?: "{}")
                    val product = json.optJSONObject("product")
                    val name = product?.optString("product_name", "Unknown product")
                    val brand = product?.optString("brands", "Unknown brand")
                    val result = "Product: $name, Brand: $brand"
                    runOnUiThread {
                        speak(result)
                        Toast.makeText(this@BarcodeActivity, result, Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        speak("Could not process product data.")
                    }
                }
            }
        })
    }

    private fun speak(text: String) {
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, UUID.randomUUID().toString())
    }

    override fun onDestroy() {
        tts.shutdown()
        super.onDestroy()
    }
}
