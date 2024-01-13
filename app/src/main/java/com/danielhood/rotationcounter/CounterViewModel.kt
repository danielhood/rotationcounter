package com.danielhood.rotationcounter

import android.graphics.Rect
import android.view.MotionEvent
import android.view.View

class CounterViewModel () {
    var rotationCount: Int = 0
    var currentFps: Float = 0f
    var targetX: Float = 0f
    var targetY: Float = 0f

    val rotationCountString: String
        get()  = rotationCount.toString()

    val currentFpsString: String
        get() = "%.02f".format(currentFps)
}