package com.danielhood.rotationcounter

import android.graphics.Rect

class CounterViewModel (rotationCount: Int, currentFps: Float) {
    var rotationCountString: String = rotationCount.toString()
    var currentFpsString: String = "%.02f".format(currentFps)
}