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
 * 下激光点云
 */
@SuppressLint("ViewConstructor")
class DownLaserScanView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView(context, parent) {
    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    //激光点云
    private val cloudList = ArrayList<PointF>()

    companion object {
        private val paint: Paint = Paint().apply {
            color = Color.YELLOW
            strokeWidth = 2f
            style = Paint.Style.FILL
        }
    }


    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 只有在绘制启用状态下才绘制点云
        if (isDrawingEnabled && cloudList.isNotEmpty()) {
            val mapView = parent.get() ?: return

            // 预分配数组大小
            val pointsArray = FloatArray(cloudList.size * 2)
            var index = 0


            for (point in cloudList) {
                val screenPoint = mapView.worldToScreen(point.x, point.y)
                pointsArray[index++] = screenPoint.x
                pointsArray[index++] = screenPoint.y
            }

            canvas.drawPoints(pointsArray, paint)
        }
    }

    /**
     * 下激光点云
     */
    fun updateDownLaserScan(laser: laser_t) {
        cloudList.clear()
        val robotX = laser.ranges[0]
        val robotY = laser.ranges[1]
        val robotT = laser.ranges[2]
        if (laser.ranges.size > 3) {
            // 预分配容量
            val expectedSize = (laser.ranges.size / 3) - 1
            cloudList.ensureCapacity(expectedSize)

            for (i in 1 until laser.ranges.size / 3) {
                val laserX = laser.ranges[3 * i]
                val laserY = laser.ranges[3 * i + 1]
                cloudList.add(
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

    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理点云数据
        cloudList.clear()
        // 清理父引用
        parent.clear()
    }
}
