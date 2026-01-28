package com.siasun.dianshi.createMap2D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.Log
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.view.SlamWareBaseView
import java.lang.ref.WeakReference
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.graphics.withRotation

/**
 * 建图上激光点云
 */
@SuppressLint("ViewConstructor")
class UpLaserScanView2D(context: Context?, val parent: WeakReference<CreateMapView2D>) :
    SlamWareBaseView<CreateMapView2D>(context, parent) {
    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    //激光点云
    private val cloudList = ArrayList<PointF>()

    companion object {
        private val paint: Paint = Paint().apply {
            color = Color.RED
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
     * 上激光点云
     */
    fun updateUpLaserScan(laserData: laser_t) {
        cloudList.clear()
        if (laserData.ranges.size <= 3) return // 最少包含机器人位置数据
        val mapView = parent.get() ?: return

        // 动态计算采样间隔（根据数据量和缩放比例）
        val totalPoints = (laserData.ranges.size - 3) / 3 // 总激光点数（排除机器人位置）
        val baseSampleInterval = when {
            totalPoints > 600 -> 10  // 数据量极大时，间隔10
            totalPoints > 400 -> 5  // 数据量较大时，间隔5
            else -> 2  // 数据量较小时，间隔2
        }
        val dynamicSampleInterval =
            maxOf(baseSampleInterval, (1f / mapView.mSrf.scale).toInt()) // 缩放越小，间隔越大


        // 遍历激光点，按采样间隔降采样
        for (i in 0 until totalPoints step dynamicSampleInterval) {
            val index = 3 + i * 3 // 跳过机器人位置数据（前3个元素）
            if (index + 2 >= laserData.ranges.size) break // 越界保护

            val laserX = laserData.ranges[index]
            val laserY = laserData.ranges[index + 1]

            // 坐标变换（仅计算有效点）
            val cosT = cos(laserData.ranges[2])
            val sinT = sin(laserData.ranges[2])
            val laserXNew = laserX * cosT - laserY * sinT + laserData.ranges[0]
            val laserYNew = laserX * sinT + laserY * cosT + laserData.ranges[1]
            cloudList.add(PointF(laserXNew, laserYNew))
        }
        Log.i("SLAMMapView2D", "有效点数 ${cloudList.size}")

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
