package com.programminghut.realtime_object

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.JsonObjectRequest
import com.android.volley.toolbox.Volley
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

class GetCoffeeActivity : AppCompatActivity() {

    @SuppressLint("WrongViewCast")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_get_coffee)

        val getCoffeeButton: Button = findViewById(R.id.getcoffee_button)
        getCoffeeButton.setOnClickListener {
            val selectedBoundingBox = getSelectedBoundingBox()
            if (selectedBoundingBox != null) {
                sendGetCoffeeRequest(selectedBoundingBox)
            }
        }
    }

    private fun getSelectedBoundingBox(): BoundingBox? {
        // Implement this method to return the selected bounding box
        // For demonstration purposes, we will return a dummy bounding box
        return BoundingBox(50, 50, 200, 200, 0.9f, 1)
    }

    private fun sendGetCoffeeRequest(boundingBox: BoundingBox) {
        val url = "http://192.168.188.225:8000/getcoffee"

        val jsonBody = JSONObject()
        try {
            jsonBody.put("selected_object", JSONObject()
                .put("class", boundingBox.classId)
                .put("score", boundingBox.score)
                .put("box", JSONArray(listOf(boundingBox.x1, boundingBox.y1, boundingBox.x2, boundingBox.y2)))
            )
        } catch (e: JSONException) {
            e.printStackTrace()
        }

        val jsonObjectRequest = JsonObjectRequest(Request.Method.POST, url, jsonBody,
            Response.Listener { response ->
                // Handle response
                Toast.makeText(applicationContext, "Hand navigation started", Toast.LENGTH_SHORT).show()
            },
            Response.ErrorListener { error ->
                // Handle error
                Toast.makeText(applicationContext, "Error: " + error.message, Toast.LENGTH_SHORT).show()
            }
        )

        // Add the request to the RequestQueue
        val requestQueue: RequestQueue = Volley.newRequestQueue(this)
        requestQueue.add(jsonObjectRequest)
    }
}

data class BoundingBox(
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int,
    val score: Float,
    val classId: Int
)