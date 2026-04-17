package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import java.lang.ref.WeakReference

/**
 *  工作路径
 *  有任务状态下的路径
 */
@SuppressLint("ViewConstructor")
class WorkIngPathView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView<MapView>(context, parent) {

    // 机器人有任务 实时路径，限制最大长度防止内存溢出
    private val MAX_PATH_POINTS = 50000 // 可根据实际需求调整

    // 使用一维FloatArray存储世界坐标，防止内存抖动
    private var worldPosArray = FloatArray(MAX_PATH_POINTS * 2)
    // 预分配屏幕坐标数组用于批量转换
    private var screenPosArray = FloatArray(MAX_PATH_POINTS * 2)
    private var pointCount = 0

    // 锁对象，保护坐标数组
    private val lock = Any()

    // 矩阵映射相关预分配对象，防止内存抖动
    private val mTransformMatrix = android.graphics.Matrix()
    private val srcPoints = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f)
    private val dstPoints = FloatArray(6)

    // 绘制画笔 - 移至伴生对象，避免重复创建
    companion object {
        private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 3f
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = Color.GREEN
            strokeJoin = Paint.Join.ROUND // 设置线段连接处为圆角，使过弯更加平滑
            strokeCap = Paint.Cap.ROUND // 设置线帽为圆形，让线段端点平滑
        }
    }

    //绘制车实时路径
    private val mCarPath = Path()
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val mapView = parent.get() ?: return

        mCarPath.reset()
        
        var currentCount = 0
        synchronized(lock) {
            currentCount = pointCount
            if (currentCount == 0) return
            
            // 拷贝出当前需要绘制的世界坐标点
            System.arraycopy(worldPosArray, 0, screenPosArray, 0, currentCount * 2)
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
        mTransformMatrix.mapPoints(screenPosArray, 0, screenPosArray, 0, currentCount)
        
        // 构建完整平滑路径，防止连线出现直角（使用二阶贝塞尔曲线）
        mCarPath.moveTo(screenPosArray[0], screenPosArray[1])
        if (currentCount > 2) {
            for (i in 0 until currentCount - 1) {
                val curX = screenPosArray[i * 2]
                val curY = screenPosArray[i * 2 + 1]
                val nextX = screenPosArray[(i + 1) * 2]
                val nextY = screenPosArray[(i + 1) * 2 + 1]
                
                val midX = (curX + nextX) / 2f
                val midY = (curY + nextY) / 2f
                
                if (i == 0) {
                    // 第一条线段，先画直线到中点
                    mCarPath.lineTo(midX, midY)
                } else {
                    // 后续线段，使用二次贝塞尔曲线平滑过弯，以当前点为控制点连接到下一个中点
                    mCarPath.quadTo(curX, curY, midX, midY)
                }
            }
            // 最后一条线段，从中点画直线到终点
            val lastIdx = currentCount - 1
            mCarPath.lineTo(screenPosArray[lastIdx * 2], screenPosArray[lastIdx * 2 + 1])
        } else if (currentCount == 2) {
            // 只有两个点，直接连线
            mCarPath.lineTo(screenPosArray[2], screenPosArray[3])
        }
        
        // 一次性绘制完整路径
        canvas.drawPath(mCarPath, mPaint)
    }

    /**
     * 机器人有任务实时路径
     */
    fun setData(mCarPoint: PointF) {
        synchronized(lock) {
            // 验证坐标的有效性
            if (mCarPoint.x.isFinite() && mCarPoint.y.isFinite()) {
                if (pointCount >= MAX_PATH_POINTS) {
                    // 数组已满，移除最旧的一个点（前移所有数据）
                    System.arraycopy(worldPosArray, 2, worldPosArray, 0, (MAX_PATH_POINTS - 1) * 2)
                    pointCount--
                }
                worldPosArray[pointCount * 2] = mCarPoint.x
                worldPosArray[pointCount * 2 + 1] = mCarPoint.y
                pointCount++
            }
        }
        postInvalidate()
    }


    fun clearCarPath() {
        synchronized(lock) {
            pointCount = 0
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
        }
    }

}
