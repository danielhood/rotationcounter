package com.danielhood.rotationcounter

import android.graphics.Color
import android.util.Log

class ViewTarget (private var rotationDegrees: Int, private var imageWidth: Int, private var imageHeight: Int, private var viewWidth: Int, private var viewHeight: Int) {
    var x: Int = 0
        private set

    var y: Int = 0
        private set

    var xScaled: Int = 0
        private set

    var yScaled: Int = 0
        private set

    private val targetBuffer = 100

    var bufferX: Int = targetBuffer
        private set

    var bufferY: Int = targetBuffer
        private set

    var targetColor : Color = Color.valueOf(0f, 0f, 0f)
        private set

    private var scaleFactorX: Float = 1f
    private var scaleFactorY: Float = 1f



    init {
        updateScaleFactor()
        updateScaledCoordinates()
    }

    companion object {
        private const val TAG = "ViewTarget"
    }

    fun setTargetColor(color: Color) {
        targetColor = color

        Log.d(TAG, "setTargetColor(${targetColor.red()},${targetColor.green()},${targetColor.blue()}) at (${xScaled},${yScaled})")
    }

    fun updateCoordinates(x: Int, y: Int)
    {
        this.x = x.coerceIn(0, viewWidth)
        this.y = y.coerceIn(0, viewHeight)

        updateScaledCoordinates()
    }

    fun updateImageAndViewSize(rotationDegrees: Int, imageWidth: Int, imageHeight: Int, viewWidth: Int, viewHeight: Int) {
        this.rotationDegrees = rotationDegrees
        this.imageWidth = imageWidth
        this.imageHeight = imageHeight
        this.viewWidth = viewWidth
        this.viewHeight = viewHeight

        Log.d(
            TAG,
            "rotation:${rotationDegrees}, image(${imageWidth},${imageHeight}), view(${viewWidth},${viewHeight})"
        )

        updateScaleFactor()
        updateScaledCoordinates()
    }

    private fun updateScaleFactor() {
        when (rotationDegrees) {
            90 -> {
                this.scaleFactorX = imageHeight.toFloat() / viewWidth
                this.scaleFactorY = imageWidth.toFloat() / viewHeight
            }
            else -> {
                this.scaleFactorX = imageWidth.toFloat() / viewWidth
                this.scaleFactorY = imageHeight.toFloat() / viewHeight
            }
        }

        Log.d(
            TAG,
            "scaleFactor(${scaleFactorX},${scaleFactorY})"
        )
    }

    private fun updateScaledCoordinates() {
        when (rotationDegrees) {
            180 -> {
                xScaled = (scaleFactorX * (viewWidth - x)).toInt()
                yScaled = (scaleFactorY * (viewHeight - y)).toInt()

                bufferX = (scaleFactorX * targetBuffer).toInt()
                bufferY = (scaleFactorY * targetBuffer).toInt()
            }
            90 -> {
                xScaled = (scaleFactorY * y).toInt()
                yScaled = (scaleFactorX * (viewWidth - x)).toInt()

                bufferX = (scaleFactorY * targetBuffer).toInt()
                bufferY = (scaleFactorX * targetBuffer).toInt()
            }
            else -> {
                xScaled = (scaleFactorX * x).toInt()
                yScaled = (scaleFactorY * y).toInt()

                bufferX = (scaleFactorX * targetBuffer).toInt()
                bufferY = (scaleFactorY * targetBuffer).toInt()
            }
        }

        Log.d(
            TAG,
            "target(${x},${y}), scaled(${xScaled},${yScaled})"
        )
    }

}