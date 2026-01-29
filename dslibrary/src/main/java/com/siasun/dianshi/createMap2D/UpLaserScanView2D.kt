package com.siasun.dianshi.createMap2D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.utils.RadianUtil
import com.siasun.dianshi.view.SlamWareBaseView
import java.lang.ref.WeakReference
import kotlin.math.cos
import kotlin.math.sin

/**
 * 建图上激光点云
 */
@SuppressLint("ViewConstructor")
class UpLaserScanView2D(context: Context?, val parent: WeakReference<CreateMapView2D>) :
    SlamWareBaseView<CreateMapView2D>(context, parent) {

    //激光点云
    private val cloudList: MutableList<PointF> = mutableListOf()
    private var currentWorkMode = CreateMapView2D.WorkMode.MODE_SHOW_MAP
    var matrixPoint = Matrix()

    companion object {
        private val paint: Paint = Paint().apply {
            color = Color.RED
            strokeWidth = 2f
            style = Paint.Style.FILL
        }
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: CreateMapView2D.WorkMode) {
        if (currentWorkMode == mode) return // 避免重复设置

        currentWorkMode = mode

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
            val cosT = cos(mapView.robotPose[2])
            val sinT = sin(mapView.robotPose[2])
            val laserXNew = laserX * cosT - laserY * sinT + mapView.robotPose[0]
            val laserYNew = laserX * sinT + laserY * cosT + mapView.robotPose[1]
            cloudList.add(PointF(laserXNew, laserYNew))
        }
//        Log.i("SLAMMapView2D", "有效点数 ${cloudList.size}")

        postInvalidate()
    }


    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (cloudList.isNotEmpty()) {
            val mapView = parent.get() ?: return

            // 预分配数组大小
            val screenPoints = FloatArray(cloudList.size * 2)
            var index = 0

            for (point in cloudList) {
                // 先转换世界坐标到地图像素坐标
                val mapPixelPoint = mapView.mSrf.worldToScreen(point.x, point.y)
                // 再使用矩阵转换地图像素坐标到屏幕坐标
                val points = floatArrayOf(mapPixelPoint.x, mapPixelPoint.y)
                matrixPoint.mapPoints(points)
                screenPoints[index++] = points[0]
                screenPoints[index++] = points[1]
            }

            canvas.drawPoints(screenPoints, paint)
        }
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

    override fun setMatrixWithScale(matrix: Matrix, scale: Float) {
        super.setMatrixWithScale(matrix, scale)
        val tempMatrix = Matrix()
        tempMatrix.set(matrix)
        // 应用相同的缩放比例
        tempMatrix.postScale(scale, scale)
        matrixPoint = tempMatrix
    }

    override fun setMatrixWithRotation(matrix: Matrix, rotation: Float) {
        super.setMatrixWithRotation(matrix, rotation)
        val tempMatrix = Matrix()
        tempMatrix.set(matrix)
        // 应用相同的旋转角度
        tempMatrix.postRotate(RadianUtil.toAngel(rotation))
        matrixPoint = tempMatrix
    }
}
