package com.batz02.tradevisionai.ui

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

class ConfidenceGaugeView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 50f
        strokeCap = Paint.Cap.BUTT
    }

    private val needlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = Color.parseColor("#8B949E")
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.parseColor("#8B949E")
    }

    private val rectF = RectF()
    private var score = 50f

    private val segmentColors = listOf(
        Color.parseColor("#D32F2F"),
        Color.parseColor("#EF5350"),
        Color.parseColor("#E0E0E0"),
        Color.parseColor("#81C784"),
        Color.parseColor("#388E3C")
    )

    fun setPrediction(isBuy: Boolean, confidence: Float) {
        score = if (isBuy) {
            50f + (confidence / 2f)
        } else {
            50f - (confidence / 2f)
        }
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()

        val cx = width / 2f
        val cy = height - 20f
        val radius = (width / 2f) - paint.strokeWidth

        rectF.set(cx - radius, cy - radius, cx + radius, cy + radius)

        val sweepAngle = 180f / segmentColors.size
        var startAngle = 180f

        for (color in segmentColors) {
            paint.color = color
            canvas.drawArc(rectF, startAngle, sweepAngle - 3f, false, paint)
            startAngle += sweepAngle
        }

        val targetAngle = 180f + (score / 100f * 180f)
        val targetAngleRad = Math.toRadians(targetAngle.toDouble())

        val needleLength = radius - 15f
        val stopX = (cx + needleLength * cos(targetAngleRad)).toFloat()
        val stopY = (cy + needleLength * sin(targetAngleRad)).toFloat()

        canvas.drawLine(cx, cy, stopX, stopY, needlePaint)
        canvas.drawCircle(cx, cy, 12f, centerPaint)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val w = measuredWidth
        setMeasuredDimension(w, (w / 2) + 40)
    }
}