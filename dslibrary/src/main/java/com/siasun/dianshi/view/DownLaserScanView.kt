package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import java.lang.ref.WeakReference

/**
 * 下激光点云
 */
@SuppressLint("ViewConstructor")
class DownLaserScanView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView(context, parent) {
    private val paint: Paint = Paint()
    private var downPointsCloudList: MutableList<PointF>? = null

    init {
        paint.setColor(Color.YELLOW)
        paint.strokeWidth = 4f
        paint.style = Paint.Style.FILL
    }


    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val flatMap = downPointsCloudList?.flatMap { point ->
            val pointCloud = parent.get()!!.worldToScreen(point.x, point.y)
            listOf(pointCloud.x, pointCloud.y)
        }
        flatMap?.let {
            canvas.drawPoints(
                it.toFloatArray(), paint
            )
        }
    }

    /**
     * 下激光点云
     */
    fun updateDownLaserScan(list: MutableList<PointF>) {
        this.downPointsCloudList = list
        postInvalidate()
    }
}
