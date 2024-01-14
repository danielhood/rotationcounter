package com.danielhood.rotationcounter

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

class CounterDrawable(private val counterViewModel: CounterViewModel) : Drawable() {
    private val counterTextPaint = Paint().apply {
        color = Color.DKGRAY
        alpha = 255
        textSize = 36F
    }

    private val counterRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.YELLOW
        alpha = 255
    }
    private val fpsTextPaint = Paint().apply {
        color = Color.DKGRAY
        alpha = 255
        textSize = 36F
    }

    private val fpsRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.YELLOW
        alpha = 255
    }

    private val targetRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.CYAN
        alpha = 255
    }

    private  val targetRectBuffer= 100f

    override fun draw(canvas: Canvas) {
        canvas.drawRect( 10f, 10f, 200f, 50f, counterRectPaint)
        canvas.drawRect( 10f, 70f, 200f, 110f, fpsRectPaint)

        canvas.drawRect(
            counterViewModel.targetX-targetRectBuffer,
            counterViewModel.targetY-targetRectBuffer,
            counterViewModel.targetX+targetRectBuffer,
            counterViewModel.targetY+targetRectBuffer,
            targetRectPaint)

        canvas.drawText(
            counterViewModel.rotationCountString,
            15f, 42f,
            counterTextPaint
        )

        canvas.drawText(
            counterViewModel.currentFpsString,
            15f, 102f,
            fpsTextPaint
        )
    }

    override fun setAlpha(alpha: Int) {
        counterRectPaint.alpha = alpha
        counterTextPaint.alpha = alpha

        fpsRectPaint.alpha = alpha
        fpsTextPaint.alpha = alpha

        targetRectPaint.alpha = alpha
    }

    override fun setColorFilter(colorFiter: ColorFilter?) {
        counterRectPaint.colorFilter = colorFilter
        counterTextPaint.colorFilter = colorFilter

        fpsRectPaint.colorFilter = colorFilter
        fpsTextPaint.colorFilter = colorFilter

        targetRectPaint.colorFilter = colorFilter
    }

    @Deprecated("Deprecated in Java",
        ReplaceWith("PixelFormat.TRANSLUCENT", "android.graphics.PixelFormat")
    )
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}