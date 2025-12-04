package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import com.ngu.lcmtypes.laser_t
import java.lang.ref.WeakReference
import kotlin.math.cos
import kotlin.math.sin

/**
 * 上激光点云
 */
@SuppressLint("ViewConstructor")
class UpLaserScanView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView(context, parent) {
    private val paint: Paint = Paint()
    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    //上激光点云
    private val upPointsCloudList = mutableListOf<PointF>()

    init {
        paint.setColor(Color.RED)
        paint.strokeWidth = 4f
        paint.style = Paint.Style.FILL
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 只有在绘制启用状态下才绘制点云
        if (isDrawingEnabled) {
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
    }

    /**
     * 上激光点云
     */
    fun updateUpLaserScan(laser: laser_t) {
        upPointsCloudList.clear()
        val robotX = laser.ranges[0]
        val robotY = laser.ranges[1]
        val robotT = laser.ranges[2]
        if (laser.ranges.size > 3) {
            for (i in 1 until laser.ranges.size / 3) {
                val laserX = laser.ranges[3 * i]
                val laserY = laser.ranges[3 * i + 1]
                upPointsCloudList.add(
                    PointF(
                        laserX * cos(robotT) - laserY * sin(robotT) + robotX,
                        laserX * sin(robotT) + laserY * cos(robotT) + robotY
                    )
                )
            }
        }
        postInvalidate()
    }

    /**
     * 设置是否启用绘制
     */
    fun setDrawingEnabled(enabled: Boolean) {
        this.isDrawingEnabled = enabled
        postInvalidate()
    }
}
