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
        textSize = 140F
    }

    private val counterRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.YELLOW
        alpha = 255
    }

    private val counterCaptionPaint = Paint().apply {
        color = Color.DKGRAY
        alpha = 255
        textSize = 30F
    }

    private val fpsTextPaint = Paint().apply {
        color = Color.DKGRAY
        alpha = 255
        textSize = 140F
    }

    private val fpsRectPaint = Paint().apply {
        style = Paint.Style.FILL
        color = Color.YELLOW
        alpha = 255
    }

    private val fpsCaptionPaint = Paint().apply {
        color = Color.DKGRAY
        alpha = 255
        textSize = 30F
    }

    private val targetRectPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.CYAN
        alpha = 255
    }

    private  val targetRectBuffer= 100f

    override fun draw(canvas: Canvas) {
        val textWidth = 500f
        val textMargin = 10f
        val fpsLeft = canvas.width-textWidth
        val fpsRight = canvas.width-textMargin
        val countLeft = textMargin
        val countRight = textWidth+textMargin

        val textTop = textMargin
        val textBottom = 160f

        val captionBottom = textTop+30f

        canvas.drawRect( countLeft, textTop, countRight, textBottom, counterRectPaint)
        canvas.drawRect( fpsLeft, textTop, fpsRight, textBottom, fpsRectPaint)

        canvas.drawRect(
            counterViewModel.targetX-targetRectBuffer,
            counterViewModel.targetY-targetRectBuffer,
            counterViewModel.targetX+targetRectBuffer,
            counterViewModel.targetY+targetRectBuffer,
            targetRectPaint)

        canvas.drawText(
            "Count",
            countLeft+15f, captionBottom,
            counterCaptionPaint
        )

        canvas.drawText(
            counterViewModel.rotationCountString,
            countLeft+5f, textBottom-8f,
            counterTextPaint
        )

        canvas.drawText(
            counterViewModel.currentFpsString,
            fpsLeft+5f, textBottom-8f,
            fpsTextPaint
        )

        canvas.drawText(
            "FPS",
            fpsLeft+15f, captionBottom,
            fpsCaptionPaint
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