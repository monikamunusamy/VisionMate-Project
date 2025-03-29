package com.programminghut.realtime_object.network

data class BoundingBox(
    val x1: Int,
    val y1: Int,
    val x2: Int,
    val y2: Int,
    val score: Float,
    val classId: Int
)