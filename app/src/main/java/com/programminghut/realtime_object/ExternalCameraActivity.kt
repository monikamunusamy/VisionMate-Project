package com.programminghut.realtime_object

import android.app.Activity
import android.content.Intent
import android.os.*
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import kotlin.concurrent.thread

class ExternalCameraActivity : AppCompatActivity(), TextToSpeech.OnInitListener {

    private lateinit var webView: WebView
    private lateinit var tts: TextToSpeech
    private var ttsReady = false
    private val voiceInputCode = 101
    private var detectedObjects = listOf<String>()
    private var isWaitingForSelection = false
    private var isAskingForNextAction = false
    private var isAskingForLiveObjects = false
    private val handler = Handler(Looper.getMainLooper())
    private var lastAnnouncedObjects: Set<String> = emptySet()
    private var isDetectionActive = true
    private var isVoiceSessionActive = false
    private var isBraceletGuiding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_external_camera)

        tts = TextToSpeech(this, this)
        setupWebView()

        findViewById<ImageButton>(R.id.btnVoiceAssistant).setOnClickListener {
            if (ttsReady && !isVoiceSessionActive && !isBraceletGuiding) {
                isVoiceSessionActive = true
                isAskingForLiveObjects = true
                speak("Voice assistant activated. Please speak now.") {
                    waitAndStartVoiceInput()
                }
            } else if (!ttsReady) {
                Toast.makeText(this, "Voice assistant not ready yet", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts.language = Locale.US
            ttsReady = true
            speak("Welcome. Initializing object detection system.") {
                startObjectDetection()
            }
        } else {
            Toast.makeText(this, "TTS initialization failed", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setupWebView() {
        webView = findViewById(R.id.webView)
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                if (url?.contains("video_feed") == true && ttsReady) {
                    handler.postDelayed({
                        speak("Camera feed is active. Starting object detection.") {
                            startObjectDetection()
                        }
                    }, 2000)
                }
            }
        }
        webView.loadUrl("http://192.168.188.225:8000/video_feed")
    }

    private fun startObjectDetection() {
        isDetectionActive = true
        fetchDetectedObjects()
    }

    private fun stopObjectDetection() {
        isDetectionActive = false
        handler.removeCallbacksAndMessages(null)
    }

    private fun fetchDetectedObjects() {
        if (!isDetectionActive) return

        thread {
            try {
                val url = URL("http://192.168.188.225:8000/detected_objects")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"

                val response = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonArray = JSONArray(response)

                val objects = mutableListOf<String>()
                for (i in 0 until jsonArray.length()) {
                    objects.add(jsonArray.getString(i).trim().lowercase(Locale.US))
                }

                runOnUiThread {
                    if (objects.isNotEmpty()) {
                        detectedObjects = objects
                        lastAnnouncedObjects = objects.toSet()

                        speak("Detected objects: ${objects.joinToString(", ")}. Which one would you like to grasp?") {
                            isWaitingForSelection = true
                            waitAndStartVoiceInput()
                        }
                    } else {
                        speak("No objects found. Trying again...") {
                            handler.postDelayed({ fetchDetectedObjects() }, 3000)
                        }
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    speak("Error detecting objects. Retrying...") {
                        handler.postDelayed({ fetchDetectedObjects() }, 3000)
                    }
                }
            }
        }
    }

    private fun waitAndStartVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        }
        try {
            startActivityForResult(intent, voiceInputCode)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != voiceInputCode || resultCode != Activity.RESULT_OK || data == null) return

        val speech = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.get(0)?.lowercase(Locale.getDefault()) ?: ""

        when {
            isWaitingForSelection -> {
                isWaitingForSelection = false
                handleObjectSelection(speech)
            }
            isAskingForNextAction -> handleNextAction(speech)
            isAskingForLiveObjects -> {
                isAskingForLiveObjects = false
                handleLiveObjectQuery(speech)
            }
        }
        isVoiceSessionActive = false
    }

    private fun handleObjectSelection(speech: String) {
        val cleanedSpeech = speech.trim().lowercase(Locale.US)
        val words = cleanedSpeech.split(" ")

        val matched = detectedObjects.firstOrNull { obj ->
            words.contains(obj) || cleanedSpeech.contains(obj)
        }

        if (matched != null) {
            isBraceletGuiding = true
            speak("Bracelet activated.") {
                speak("Now trying to navigate to the object $matched.") {
                    Toast.makeText(this, "Navigation started. You may now move your hand.", Toast.LENGTH_LONG).show()
                    sendObjectNameToFlask(matched)
                }
            }
        } else {
            speak("I couldn’t find that object in the current view. Try again.") {
                isWaitingForSelection = true
                waitAndStartVoiceInput()
            }
        }
    }

    private fun sendObjectNameToFlask(name: String) {
        thread {
            try {
                val url = URL("http://192.168.188.225:8000/activate_bracelet")
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "POST"
                connection.setRequestProperty("Content-Type", "application/json")
                connection.doOutput = true

                val json = """{"object_name": "$name"}"""
                connection.outputStream.use { it.write(json.toByteArray()) }

                val responseText = connection.inputStream.bufferedReader().use { it.readText() }
                val graspSuccess = responseText.contains("Grasp successful", true)

                runOnUiThread {
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        speak("Guidance is ongoing. You may now move your hand toward the object.") {
                            if (graspSuccess) {
                                speak("Grasp successful. Do you want to detect another object or exit?") {
                                    isAskingForNextAction = true
                                    waitAndStartVoiceInput()
                                }
                            }
                        }
                    } else {
                        speak("Failed to activate bracelet. Please try again.")
                        isBraceletGuiding = false
                    }
                }
            } catch (e: Exception) {
                runOnUiThread {
                    speak("Error connecting to server.") {
                        isBraceletGuiding = false
                    }
                }
            }
        }
    }

    private fun handleNextAction(speech: String) {
        isAskingForNextAction = false
        when {
            speech.contains("again") || speech.contains("yes") -> resetDetection()
            speech.contains("exit") || speech.contains("no") -> speak("Closing object detection.") { finish() }
            else -> {
                speak("Say 'again' to detect more or 'exit' to quit.") {
                    isAskingForNextAction = true
                    waitAndStartVoiceInput()
                }
            }
        }
    }

    private fun resetDetection() {
        detectedObjects = emptyList()
        lastAnnouncedObjects = emptySet()
        isBraceletGuiding = false
        speak("Resetting detection.") {
            fetchDetectedObjects()
        }
    }

    private fun handleLiveObjectQuery(speech: String) {
        if (speech.contains("what") || speech.contains("see") || speech.contains("list")) {
            if (lastAnnouncedObjects.isNotEmpty()) {
                val list = lastAnnouncedObjects.joinToString(", ")
                speak("Currently I see: $list. Which one would you like to grasp?") {
                    isWaitingForSelection = true
                    waitAndStartVoiceInput()
                }
            } else {
                speak("Let me check again.") {
                    fetchDetectedObjects()
                }
            }
        } else {
            speak("I didn’t understand. You can say something like: what do you see.") {
                isAskingForLiveObjects = true
                waitAndStartVoiceInput()
            }
        }
    }

    private fun speak(text: String, onDone: (() -> Unit)? = null) {
        if (!ttsReady) return
        val id = UUID.randomUUID().toString()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) { runOnUiThread { onDone?.invoke() } }
            override fun onError(utteranceId: String?) { runOnUiThread { onDone?.invoke() } }
        })
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, id)
    }

    override fun onDestroy() {
        stopObjectDetection()
        tts.shutdown()
        super.onDestroy()
    }
}
