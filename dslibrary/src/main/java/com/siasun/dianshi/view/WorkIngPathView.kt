package com.siasun.dianshi.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import java.lang.ref.WeakReference
import java.util.Collections

/**
 *  工作路径
 *  有任务状态下的路径
 */
class WorkIngPathView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView(context, parent) {
    private var radius = 8f

    private var pathList: MutableList<PointF>? = null

    // 机器人有任务 实时路径，限制最大长度防止内存溢出
    private val MAX_PATH_POINTS = 1000 // 可根据实际需求调整
    private val cartPosList = Collections.synchronizedList(mutableListOf<PointF>())

    // 用于复用的PointF对象，减少内存分配
    private var tempPoint = PointF()

    // 伴生对象存储画笔，避免重复创建
    companion object {
        private val mPaint: Paint = Paint().apply {
            strokeWidth = 1f
            isAntiAlias = true
            style = Paint.Style.FILL
            alpha = 100
            color = Color.parseColor("#d2f0f4")
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val mapView = parent.get() ?: return
        // 绘制已设置的路径列表
        pathList?.forEach { pointF ->
            // 复用PointF对象
            tempPoint = mapView.worldToScreen(pointF.x, pointF.y)
            drawCircle(
                canvas,
                tempPoint, radius * scale, mPaint
            )
        }

        // 绘制实时路径（cartPosList）
        synchronized(cartPosList) {
            cartPosList.forEach { pointF ->
                // 复用PointF对象
                tempPoint = mapView.worldToScreen(pointF.x, pointF.y)
                drawCircle(
                    canvas,
                    tempPoint, radius * scale, mPaint
                )
            }
        }
    }

    /**
     * 机器人有任务实时路径
     */
    fun setData(mCarPoint: PointF) {
        synchronized(cartPosList) {
            cartPosList.add(mCarPoint)
            // 限制路径点数量，防止内存溢出
            if (cartPosList.size > MAX_PATH_POINTS) {
                cartPosList.removeAt(0)
            }
        }
        postInvalidate()
    }

    /**
     * 设置路径列表
     */
    fun setPathList(list: MutableList<PointF>?) {
        this.pathList = list
        postInvalidate()
    }

    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        // 清空路径列表
        pathList?.clear()
        pathList = null

        // 清空机器人位置列表
        cartPosList.clear()
    }

}
