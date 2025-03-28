package com.programminghut.realtime_object.network


data class SelectedObject(
    val `class` : Int,
    val score: Float,
    val box: List<Float>
)