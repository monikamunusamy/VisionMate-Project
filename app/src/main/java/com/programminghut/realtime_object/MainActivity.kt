package com.programminghut.realtime_object

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var textToSpeech: TextToSpeech
    private lateinit var editTextLocation: EditText
    private val VOICE_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        // Initialize buttons
        val btnInternalCamera: View = findViewById(R.id.button_open_internal_camera)
        val btnExternalCamera: View = findViewById(R.id.button_open_external_camera)
        val btnOpenList: View = findViewById(R.id.button_open_list)
        val btnOpenMap: View = findViewById(R.id.button_open_map)
        val btnBarcode: View = findViewById(R.id.button_barcode)
        val btnSpeak: View = findViewById(R.id.button_voice_input)

        // Set up TextToSpeech
        textToSpeech = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                textToSpeech.language = Locale.US
            }
        }

        // Button click listeners
        btnInternalCamera.setOnClickListener {
            speakOut("Opening internal camera")
            startActivity(Intent(this, InternalCameraActivity::class.java))
        }

        btnExternalCamera.setOnClickListener {
            speakOut("Opening external camera")
            startActivity(Intent(this, ExternalCameraActivity::class.java))
        }

        btnOpenList.setOnClickListener {
            speakOut("Opening your list")
            startActivity(Intent(this, ListActivity::class.java))
        }

        btnOpenMap.setOnClickListener {
            val location = editTextLocation.text.toString()
            if (location.isNotEmpty()) {
                speakOut("Opening map for $location")
                navigateToLocation(location)
            } else {
                Toast.makeText(this, "Enter a location", Toast.LENGTH_SHORT).show()
            }
        }

        btnBarcode.setOnClickListener {
            speakOut("Opening barcode scanner")
            startActivity(Intent(this, BarcodeActivity::class.java))
        }

        btnSpeak.setOnClickListener {
            startVoiceInput()
        }
    }

    private fun speakOut(message: String) {
        textToSpeech.speak(message, TextToSpeech.QUEUE_FLUSH, null, "")
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        try {
            startActivityForResult(intent, VOICE_REQUEST_CODE)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == VOICE_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val command = result?.get(0)?.lowercase(Locale.getDefault()) ?: ""

            // Debug: show what was heard
            Log.d("VoiceCommand", "User said: $command")
            Toast.makeText(this, "You said: $command", Toast.LENGTH_SHORT).show()

            when {
                "internal camera" in command -> {
                    speakOut("Opening internal camera")
                    startActivity(Intent(this, InternalCameraActivity::class.java))
                }

                "external camera" in command -> {
                    speakOut("Opening external camera")
                    startActivity(Intent(this, ExternalCameraActivity::class.java))
                }

                "open list" in command || "show list" in command -> {
                    speakOut("Opening your list")
                    startActivity(Intent(this, ListActivity::class.java))
                }

                "open barcode" in command || "scan barcode" in command || "barcode" in command -> {
                    speakOut("Opening barcode scanner")
                    startActivity(Intent(this, BarcodeActivity::class.java))
                }

                "navigate to" in command -> {
                    val location = command.replace("navigate to", "").trim()
                    if (location.isNotEmpty()) {
                        speakOut("Navigating to $location")
                        navigateToLocation(location)
                    } else {
                        speakOut("Please provide a destination.")
                    }
                }

                "map" in command || "navigate" in command -> {
                    val location = editTextLocation.text.toString()
                    if (location.isNotEmpty()) {
                        speakOut("Opening map for $location")
                        navigateToLocation(location)
                    } else {
                        speakOut("Please type a location or say 'navigate to [place]'")
                    }
                }

                else -> speakOut("Sorry, I didn't understand the command.")
            }
        }
    }

    private fun navigateToLocation(location: String) {
        val gmmIntentUri = Uri.parse("google.navigation:q=${Uri.encode(location)}")
        val mapIntent = Intent(Intent.ACTION_VIEW, gmmIntentUri)
        mapIntent.setPackage("com.google.android.apps.maps")
        if (mapIntent.resolveActivity(packageManager) != null) {
            startActivity(mapIntent)
        } else {
            Toast.makeText(this, "Google Maps is not installed.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        super.onDestroy()
    }
}
