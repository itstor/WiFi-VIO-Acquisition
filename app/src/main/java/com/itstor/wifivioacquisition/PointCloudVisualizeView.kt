package com.itstor.wifivioacquisition

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.view.View

internal class PointCloudVisualizeView(context: Context?) : View(context) {
    private var pointsList: List<Point> = ArrayList()
    private val markPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        strokeWidth = 1.5f
    }

    fun setPoints(points: List<Point>) {
        pointsList = points
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        for (point in pointsList) {
            val x = point.x.toFloat()
            val y = point.y.toFloat()

            canvas.drawLine(x - 10, y - 10, x + 10, y + 10, markPaint)
            canvas.drawLine(x + 10, y - 10, x - 10, y + 10, markPaint)
        }
    }
}

