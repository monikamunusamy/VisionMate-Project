package com.programminghut.realtime_object


import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.view.View
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.programminghut.realtime_object.R
import java.net.HttpURLConnection
import java.net.URL
import java.util.*

class ExternalCameraActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var webView: WebView
    private lateinit var textToSpeech: TextToSpeech
    private val speechQueue: Queue<String> = LinkedList()
    private var isVideoFeedLoaded = false // Flag to check if video feed is loaded

    private val speechRecognizerLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val data = result.data
                val matches = data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                val recognizedText = matches?.get(0)
                recognizedText?.let {
                    sendObjectNameToFlask(it)
                }
            }
        }

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_external_camera)

        // Initialize TextToSpeech
        textToSpeech = TextToSpeech(this, this)

        // Immediately speak the opening message
        speakWithDelay("Opening the camera, please wait.", 0)

        // Initialize WebView
        webView = findViewById(R.id.webView)
        webView.webViewClient = object : WebViewClient() {
            override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                super.onReceivedError(view, request, error)
                val errorMessage = "Unable to load external camera feed."
                Toast.makeText(applicationContext, errorMessage, Toast.LENGTH_LONG).show()
                speak(errorMessage)
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                super.onPageFinished(view, url)
                if (url == "http://192.168.188.225:8000/video_feed") {
                    isVideoFeedLoaded = true
                    speakWithDelay("You are now viewing the external camera feed.", 0)
                    promptForObjectWithDelay(0) // Prompt for object selection immediately
                }
            }
        }

        // Enable JavaScript
        webView.settings.javaScriptEnabled = true
        webView.loadUrl("http://192.168.188.225:8000/video_feed") // Ensure this URL is correct

        // Enable immersive mode for full-screen experience
        enableImmersiveMode()
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech.setLanguage(Locale.US) // Change to Locale.forLanguageTag("tr-TR") for Turkish
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("TTS", "Language not supported")
            }
        } else {
            Log.e("TTS", "Initialization failed")
        }
    }

    private fun speak(message: String) {
        speechQueue.add(message)
        if (textToSpeech.isSpeaking) return
        processSpeechQueue()
    }

    private fun speakWithDelay(message: String, delay: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            Log.d("TTS", "Attempting to speak: $message")
            speak(message)
        }, delay)
    }

    private fun processSpeechQueue() {
        val message = speechQueue.poll()
        message?.let {
            val params = Bundle().apply {
                putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, message)
            }
            textToSpeech.speak(it, TextToSpeech.QUEUE_FLUSH, params, null)
            textToSpeech.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {}

                override fun onDone(utteranceId: String?) {
                    processSpeechQueue() // Process the next message in the queue
                }

                override fun onError(utteranceId: String?) {}
            })
        }
    }

    private fun enableImmersiveMode() {
        val decorView = window.decorView
        decorView.systemUiVisibility =
            (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    private fun promptForObject() {
        if (!isVideoFeedLoaded) return // Ensure video feed is loaded before prompting

        Log.d("ExternalCameraActivity", "Prompting for object...")
        speak("Please say the label of the object you want to select.")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault()) // Change to Turkish if needed
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Please say the label of the object you want to select.")
        }
        try {
            speechRecognizerLauncher.launch(intent)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Speech recognition is not supported on this device.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun promptForObjectWithDelay(delay: Long) {
        Handler(Looper.getMainLooper()).postDelayed({
            promptForObject()
        }, delay)
    }

    // Function to send the recognized object name to the Flask application
    private fun sendObjectNameToFlask(objectName: String) {
        speak("You selected the object: $objectName. Sending to server.")
        val url = "http://192.168.188.225:8000/activate_bracelet"
        val requestBody = """{"object_name": "$objectName"}"""

        Thread {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true
                connection.outputStream.write(requestBody.toByteArray())

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    speak("Object name sent successfully. Guiding your hand.")
                } else {
                    speak("Failed to send object name.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                speak("Error occurred while sending object name.")
            }
        }.start()
    }
}