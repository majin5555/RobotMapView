package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import com.ngu.lcmtypes.laser_t
import java.lang.ref.WeakReference
import kotlin.math.cos
import kotlin.math.sin

/**
 * 下激光点云
 */
@SuppressLint("ViewConstructor")
class DownLaserScanView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView<MapView>(context, parent) {
    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    // 优化：将 PointF 列表改为世界坐标 FloatArray 存储，避免频繁分配对象
    private var worldPoints = FloatArray(0)
    private var screenPoints = FloatArray(0)
    private var pointCount = 0

    // 优化：复用矩阵和坐标数组，避免 onDraw 内存抖动
    private val transformMatrix = Matrix()
    private val srcPoints = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f)
    private val dstPoints = FloatArray(6)

    // 锁对象，保证多线程访问安全
    private val dataLock = Any()

    companion object {
        private val paint: Paint = Paint().apply {
            color = Color.YELLOW
            strokeWidth = 2f
            style = Paint.Style.FILL
        }
    }

    init {
        // 【优化 2】核心修复：对该高负载 View 强制关闭硬件加速，转为 CPU 绘制，彻底解决 libGLES_mali.so 崩溃
        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }
    
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isDrawingEnabled) return
        val mapView = parent.get() ?: return

        synchronized(dataLock) {
            if (pointCount > 0) {
                // 确保屏幕坐标数组容量足够
                if (screenPoints.size < pointCount * 2) {
                    screenPoints = FloatArray(pointCount * 2)
                }

                // 1. 通过 3 个基准点计算世界坐标到屏幕坐标的仿射变换矩阵
                val p0 = mapView.worldToScreen(0f, 0f)
                val px = mapView.worldToScreen(1f, 0f)
                val py = mapView.worldToScreen(0f, 1f)

                dstPoints[0] = p0.x
                dstPoints[1] = p0.y
                dstPoints[2] = px.x
                dstPoints[3] = px.y
                dstPoints[4] = py.x
                dstPoints[5] = py.y

                transformMatrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 3)

                // 2. 批量进行矩阵坐标转换（极大地避免了遍历世界点和创建 PointF 对象）
                transformMatrix.mapPoints(screenPoints, 0, worldPoints, 0, pointCount)

                // 3. 一次性绘制点云
                canvas.drawPoints(screenPoints, 0, pointCount * 2, paint)
            }
        }
    }

    /**
     * 下激光点云
     */
    fun updateDownLaserScan(laser: laser_t) {
        val robotX = laser.ranges[0]
        val robotY = laser.ranges[1]
        val robotT = laser.ranges[2]

        if (laser.ranges.size > 3) {
            val expectedPointCount = (laser.ranges.size / 3) - 1
            val requiredCapacity = expectedPointCount * 2

            // 计算三角函数值，避免循环内重复计算
            val cosT = cos(robotT)
            val sinT = sin(robotT)

            synchronized(dataLock) {
                // 如果数组容量不够，则扩容
                if (worldPoints.size < requiredCapacity) {
                    worldPoints = FloatArray(requiredCapacity)
                }

                var index = 0
                for (i in 1 until laser.ranges.size / 3) {
                    val laserX = laser.ranges[3 * i]
                    val laserY = laser.ranges[3 * i + 1]

                    worldPoints[index++] = laserX * cosT - laserY * sinT + robotX
                    worldPoints[index++] = laserX * sinT + laserY * cosT + robotY
                }
                pointCount = expectedPointCount
            }
        } else {
            synchronized(dataLock) {
                pointCount = 0
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
        synchronized(dataLock) {
            pointCount = 0
            worldPoints = FloatArray(0)
            screenPoints = FloatArray(0)
        }
        // 清理父引用
        parent.clear()
    }
}
