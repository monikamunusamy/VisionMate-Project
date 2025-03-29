package com.programminghut.realtime_object

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class ListActivity : AppCompatActivity() {

    private val itemList = mutableListOf<String>()
    private lateinit var tts: TextToSpeech
    private val voiceInputCode = 200
    private var isWaitingForItem = true

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        listView = findViewById(R.id.list_view)
        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, itemList)
        listView.adapter = adapter

        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                askForItem()
            }
        }
    }

    private fun askForItem() {
        isWaitingForItem = true
        speak("Please say an item to add.") {
            waitAndStartVoiceInput()
        }
    }

    private fun askNextAction() {
        isWaitingForItem = false
        speak("Item added. Would you like to add more items or should I list your items?") {
            waitAndStartVoiceInput()
        }
    }

    private fun speak(text: String, onDone: (() -> Unit)? = null) {
        val utteranceId = UUID.randomUUID().toString()
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {}
            override fun onDone(utteranceId: String?) {
                onDone?.let {
                    runOnUiThread { it() }
                }
            }

            override fun onError(utteranceId: String?) {}
        })
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun waitAndStartVoiceInput(delay: Long = 500) {
        Handler(Looper.getMainLooper()).postDelayed({
            startVoiceInput()
        }, delay)
    }

    private fun startVoiceInput() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        intent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now")
        try {
            startActivityForResult(intent, voiceInputCode)
        } catch (e: Exception) {
            Toast.makeText(this, "Voice input failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == voiceInputCode && resultCode == Activity.RESULT_OK && data != null) {
            val result = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val userSpeech = result?.get(0)?.lowercase(Locale.getDefault()) ?: ""

            if (isWaitingForItem) {
                if (userSpeech.isNotBlank()) {
                    itemList.add(userSpeech)
                    adapter.notifyDataSetChanged()
                    askNextAction()
                } else {
                    speak("I didn’t hear an item. Please try again.") {
                        askForItem()
                    }
                }

            } else {
                when {
                    "add" in userSpeech || "more" in userSpeech -> {
                        askForItem()
                    }

                    "list" in userSpeech || "my items" in userSpeech -> {
                        if (itemList.isEmpty()) {
                            speak("Your list is currently empty.") {
                                askNextAction()
                            }
                        } else {
                            speak("Here are your items.") {
                                speakItems()
                            }
                        }
                    }

                    "close" in userSpeech || "exit" in userSpeech -> {
                        speak("Closing the list and returning to main menu.") {
                            val intent = Intent(this, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        }
                    }

                    else -> {
                        speak("I didn't understand. Say add more, list my items, or close the list.") {
                            waitAndStartVoiceInput()
                        }
                    }
                }
            }
        }
    }

    private fun speakItems(index: Int = 0) {
        if (index >= itemList.size) {
            speak("Would you like to add more items or close the list?") {
                waitAndStartVoiceInput()
            }
            return
        }
        speak(itemList[index]) {
            speakItems(index + 1)
        }
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
