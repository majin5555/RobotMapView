package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import java.lang.ref.WeakReference

/**
 *  工作路径
 *  有任务状态下的路径
 */
@SuppressLint("ViewConstructor")
class WorkIngPathView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView<MapView>(context, parent) {

    // 机器人有任务 实时路径，最大点数限制
    private val MAX_PATH_POINTS = 8000

    // 使用一维FloatArray存储世界坐标，防止内存抖动
    private var worldPosArray = FloatArray(MAX_PATH_POINTS * 2)
    // 预分配屏幕坐标数组用于批量转换
    private var screenPosArray = FloatArray(MAX_PATH_POINTS * 2)
    
    // 环形缓冲区指针与计数
    private var headIndex = 0
    private var pointCount = 0
    
    // 记录上一个点，用于过滤过近的冗余点
    private var lastX = Float.NaN
    private var lastY = Float.NaN

    // 锁对象，保护坐标数组
    private val lock = Any()

    // 矩阵映射相关预分配对象，防止内存抖动
    private val mTransformMatrix = android.graphics.Matrix()
    private val srcPoints = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f)
    private val dstPoints = FloatArray(6)

    // 绘制画笔 - 移至伴生对象，避免重复创建
    companion object {
        private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 6f
            isAntiAlias = true
            style = Paint.Style.FILL
            color = Color.GREEN
            strokeCap = Paint.Cap.ROUND // 设置线帽为圆形，让线段端点平滑
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val mapView = parent.get() ?: return

        var currentCount = 0
        var currentHead = 0
        var localScreenPosArray: FloatArray
        synchronized(lock) {
            currentCount = pointCount
            currentHead = headIndex
            if (currentCount == 0) return
            
            localScreenPosArray = screenPosArray
            
            // 从环形缓冲区中提取数据，按顺序放入连续的 localScreenPosArray
            val firstPartCount = minOf(currentCount, MAX_PATH_POINTS - currentHead)
            System.arraycopy(worldPosArray, currentHead * 2, localScreenPosArray, 0, firstPartCount * 2)
            
            val secondPartCount = currentCount - firstPartCount
            if (secondPartCount > 0) {
                System.arraycopy(worldPosArray, 0, localScreenPosArray, firstPartCount * 2, secondPartCount * 2)
            }
        }
        
        // 计算仿射变换矩阵，映射三个参考点
        val p0 = mapView.worldToScreen(0f, 0f)
        val p1 = mapView.worldToScreen(1f, 0f)
        val p2 = mapView.worldToScreen(0f, 1f)
        
        dstPoints[0] = p0.x; dstPoints[1] = p0.y
        dstPoints[2] = p1.x; dstPoints[3] = p1.y
        dstPoints[4] = p2.x; dstPoints[5] = p2.y
        
        mTransformMatrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 3)
        
        // 批量转换所有世界坐标到屏幕坐标，避免创建海量 PointF 对象
        mTransformMatrix.mapPoints(localScreenPosArray, 0, localScreenPosArray, 0, currentCount)
        
        // 一次性绘制所有点
        canvas.drawPoints(localScreenPosArray, 0, currentCount * 2, mPaint)
    }

    /**
     * 机器人有任务实时路径
     */
    fun setData(mCarPoint: PointF) {
        synchronized(lock) {
            // 验证坐标的有效性
            if (mCarPoint.x.isFinite() && mCarPoint.y.isFinite()) {
                // 优化：过滤掉距离过近的点（如小于0.02米），减少冗余点
                if (!lastX.isNaN() && !lastY.isNaN()) {
                    val dx = mCarPoint.x - lastX
                    val dy = mCarPoint.y - lastY
                    if (dx * dx + dy * dy < 0.0004f) {
                        return
                    }
                }
                lastX = mCarPoint.x
                lastY = mCarPoint.y

                // 计算环形缓冲区的插入位置
                val insertIndex = (headIndex + pointCount) % MAX_PATH_POINTS
                worldPosArray[insertIndex * 2] = mCarPoint.x
                worldPosArray[insertIndex * 2 + 1] = mCarPoint.y
                
                if (pointCount < MAX_PATH_POINTS) {
                    pointCount++
                } else {
                    // 数组已满，覆盖最老的数据，头部指针后移
                    headIndex = (headIndex + 1) % MAX_PATH_POINTS
                }
            }
        }
        postInvalidate()
    }


    fun clearCarPath() {
        synchronized(lock) {
            pointCount = 0
            headIndex = 0
            lastX = Float.NaN
            lastY = Float.NaN
        }
        postInvalidate()
    }

    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清空机器人位置列表
        synchronized(lock) {
            pointCount = 0
            headIndex = 0
            lastX = Float.NaN
            lastY = Float.NaN
        }
    }

}
