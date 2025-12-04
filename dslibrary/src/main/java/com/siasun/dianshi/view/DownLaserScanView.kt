package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import com.ngu.lcmtypes.laser_t
import java.lang.ref.WeakReference
import java.util.Collections
import kotlin.math.cos
import kotlin.math.sin

/**
 * 下激光点云
 */
@SuppressLint("ViewConstructor")
class DownLaserScanView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView(context, parent) {
    private val paint: Paint = Paint()
    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true
    //下激光点云
    private val downPointsCloudList = mutableListOf<PointF>()

    init {
        paint.setColor(Color.YELLOW)
        paint.strokeWidth = 4f
        paint.style = Paint.Style.FILL
    }


    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 只有在绘制启用状态下才绘制点云
        if (isDrawingEnabled) {
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
    }

    /**
     * 下激光点云
     */
    fun updateDownLaserScan(laser: laser_t) {
        downPointsCloudList.clear()
        val robotX: Float = laser.ranges[0]
        val robotY: Float = laser.ranges[1]
        val theta: Float = laser.ranges[2]
        if (laser.ranges.isNotEmpty()) {
            for (i in 1 until laser.ranges.size / 3) {
                val laserX: Float = laser.ranges[3 * i]
                val laserY: Float = laser.ranges[3 * i + 1]
                downPointsCloudList.add(
                    PointF(
                        (laserX * cos(theta) - laserY * sin(theta) + robotX),
                        (laserX * sin(theta) + laserY * cos(theta) + robotY)
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
