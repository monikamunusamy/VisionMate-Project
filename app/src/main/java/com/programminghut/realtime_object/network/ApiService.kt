package com.programminghut.realtime_object.network


import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("/navigate")
    fun navigate(@Body request: NavigationRequest): Call<NavigationResponse>

    @POST("/start_camera")
    fun startCamera(): Call<Void>
}