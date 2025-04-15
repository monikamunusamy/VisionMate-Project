package com.programminghut.realtime_object

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.*

class ListActivity : AppCompatActivity() {

    private val itemList = mutableListOf<String>()
    private lateinit var tts: TextToSpeech
    private val voiceInputCode = 200
    private var currentAction: String? = null

    private lateinit var listView: ListView
    private lateinit var adapter: ArrayAdapter<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_list)

        listView = findViewById(R.id.list_view)
        loadItemList()

        adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, itemList)
        listView.adapter = adapter

        tts = TextToSpeech(this) {
            if (it == TextToSpeech.SUCCESS) {
                tts.language = Locale.US
                askMainAction()
            }
        }
    }

    private fun askMainAction() {
        currentAction = null
        speak("What would you like to do? Add, remove, list, or save your items.") {
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
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
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

            Log.d("VoiceCommand", "User said: $userSpeech")
            Toast.makeText(this, "Heard: $userSpeech", Toast.LENGTH_SHORT).show()

            if (currentAction == null) {
                when {
                    "add" in userSpeech -> {
                        currentAction = "add"
                        speak("Please say the item or items to add.") {
                            waitAndStartVoiceInput()
                        }
                    }

                    "remove" in userSpeech || "delete" in userSpeech -> {
                        currentAction = "remove"
                        speak("Please say the item or items to remove.") {
                            waitAndStartVoiceInput()
                        }
                    }

                    "list" in userSpeech || "show" in userSpeech -> {
                        if (itemList.isEmpty()) {
                            speak("Your list is currently empty.") {
                                askMainAction()
                            }
                        } else {
                            speak("Here are your items.") {
                                speakItems()
                            }
                        }
                    }

                    "save" in userSpeech -> {
                        saveItemList()
                        speak("Your items have been saved.") {
                            askMainAction()
                        }
                    }

                    "exit" in userSpeech || "close" in userSpeech || "quit" in userSpeech -> {
                        speak("Closing the list and returning to the main menu.") {
                            val intent = Intent(this, MainActivity::class.java)
                            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(intent)
                            finish()
                        }
                    }

                    else -> {
                        speak("Sorry, I didn't understand. Please say add, remove, list, or save.") {
                            askMainAction()
                        }
                    }
                }
            } else {
                when (currentAction) {
                    "add" -> {
                        val itemsToAdd = userSpeech.split(",", " and ")
                            .map { it.trim() }
                            .filter { it.isNotEmpty() }

                        if (itemsToAdd.isNotEmpty()) {
                            itemList.addAll(itemsToAdd)
                            adapter.notifyDataSetChanged()
                            saveItemList()
                            speak("Added: ${itemsToAdd.joinToString(", ")}") {
                                askMainAction()
                            }
                        } else {
                            speak("I didn't catch any items. Please try again.") {
                                askMainAction()
                            }
                        }
                    }

                    "remove" -> {
                        val itemsToRemove = userSpeech.split(",", " and ")
                            .map { it.trim().lowercase() }
                            .filter { it.isNotEmpty() }

                        val removedItems = mutableListOf<String>()
                        val iterator = itemList.iterator()

                        while (iterator.hasNext()) {
                            val currentItem = iterator.next()
                            for (removeTarget in itemsToRemove) {
                                if (currentItem.trim().lowercase() == removeTarget) {
                                    iterator.remove()
                                    removedItems.add(currentItem)
                                    break
                                }
                            }
                        }

                        adapter.notifyDataSetChanged()
                        saveItemList()

                        if (removedItems.isNotEmpty()) {
                            speak("Removed: ${removedItems.joinToString(", ")}") {
                                askMainAction()
                            }
                        } else {
                            speak("None of the items were found in your list.") {
                                askMainAction()
                            }
                        }
                    }
                }
                currentAction = null
            }
        }
    }

    private fun speakItems(index: Int = 0) {
        if (index >= itemList.size) {
            speak("Would you like to do anything else? Add, remove, list, or save?") {
                askMainAction()
            }
            return
        }
        speak(itemList[index]) {
            speakItems(index + 1)
        }
    }

    private fun saveItemList() {
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val editor = prefs.edit()
        editor.putStringSet("itemList", itemList.toSet())
        editor.apply()
    }

    private fun loadItemList() {
        val prefs = getSharedPreferences("MyPrefs", MODE_PRIVATE)
        val savedSet = prefs.getStringSet("itemList", emptySet())
        itemList.clear()
        itemList.addAll(savedSet ?: emptySet())
    }

    override fun onDestroy() {
        tts.stop()
        tts.shutdown()
        super.onDestroy()
    }
}
