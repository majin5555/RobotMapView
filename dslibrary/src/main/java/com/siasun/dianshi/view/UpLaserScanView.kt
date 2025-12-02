package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import java.lang.ref.WeakReference

/**
 * 上激光点云
 */
@SuppressLint("ViewConstructor")
class UpLaserScanView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView(context, parent) {
    private val paint: Paint = Paint()
    private var upPointsCloudList: MutableList<PointF>? = null

    init {
        paint.setColor(Color.RED)
        paint.strokeWidth = 4f
        paint.style = Paint.Style.FILL
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val flatMap = upPointsCloudList?.flatMap { point ->
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
     * 上激光点云
     */
    fun updateUpLaserScan(list: MutableList<PointF>) {
        this.upPointsCloudList = list
        postInvalidate()
    }
}
