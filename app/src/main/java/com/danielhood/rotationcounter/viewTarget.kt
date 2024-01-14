package com.danielhood.rotationcounter

import android.graphics.Color

class ViewTarget (var x: Int, var y: Int, private var scaleFactorX: Float, private var scaleFactorY: Float) {
    val xScaled : Int
        get() = (scaleFactorX * x).toInt()

    val yScaled : Int
        get() = (scaleFactorY * y).toInt()

    var targetColor : Color = Color.valueOf(0f, 0f, 0f)
        private set
    fun updateScaleFactor(scaleFactorX: Float, scaleFactorY: Float){
        this.scaleFactorX = scaleFactorX
        this.scaleFactorY = scaleFactorY
    }

    fun setTargetColor(color: Color) {
        targetColor = color
    }
}