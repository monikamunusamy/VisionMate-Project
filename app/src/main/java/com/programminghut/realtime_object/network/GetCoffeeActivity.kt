package com.programminghut.realtime_object

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

data class BoundingBox(
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int,
    val score: Float,
    val classId: Int
)

class GetCoffeeActivity : AppCompatActivity() {

    private var selectedBoundingBox: BoundingBox? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_coffee)

        val getCoffeeButton: Button = findViewById(R.id.getcoffee_button)
        getCoffeeButton.setOnClickListener {
            selectedBoundingBox = getSelectedBoundingBox()
            if (selectedBoundingBox != null) {
                sendGetCoffeeRequest(selectedBoundingBox!!)
            } else {
                Toast.makeText(applicationContext, "Please select an object!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getSelectedBoundingBox(): BoundingBox? {
        // Placeholder implementation, should be replaced with actual logic to get the selected bounding box.
        return selectedBoundingBox
    }

    private fun sendGetCoffeeRequest(boundingBox: BoundingBox) {
        val url = "http://192.168.188.225:8000/getcoffee"

        val jsonBody = JSONObject()
        try {
            val selectedObject = JSONObject()
            selectedObject.put("class", boundingBox.classId)
            selectedObject.put("score", boundingBox.score)
            selectedObject.put("box", JSONArray(listOf(boundingBox.x1, boundingBox.y1, boundingBox.x2, boundingBox.y2)))

            jsonBody.put("selected_object", selectedObject)
            println("JSON Body: $jsonBody") // Debug log
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            Response.Listener { response ->
                Toast.makeText(applicationContext, "Hand navigation started", Toast.LENGTH_SHORT).show()
            },
            Response.ErrorListener { error ->
                Toast.makeText(applicationContext, "Error: " + error.message, Toast.LENGTH_SHORT).show()
            }
        )

        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        requestQueue.add(jsonObjectRequest)
    }
}