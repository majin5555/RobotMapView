package com.siasun.dianshi.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
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

    //实时路径
    private var pathList: MutableList<PointF> =
        Collections.synchronizedList(mutableListOf<PointF>())

    // 机器人有任务 实时路径，限制最大长度防止内存溢出
    private val MAX_PATH_POINTS = 1000 // 可根据实际需求调整
    private val cartPosList = Collections.synchronizedList(mutableListOf<PointF>())

    // 用于复用的PointF对象，减少内存分配
    private var tempPoint = PointF()

    // 伴生对象存储画笔，避免重复创建
    companion object {
        private val mPaint: Paint = Paint().apply {
            strokeWidth = 5f
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = Color.parseColor("#FFC0CB")
        }
    }

    //绘制车实时路径
    private val mCarPath = Path()
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val mapView = parent.get() ?: return

        mCarPath.reset()
        // 绘制已设置的路径列表
        pathList.forEachIndexed { index, pointF ->
            val pnt1Windows: PointF = mapView.worldToScreen(pointF.x, pointF.y)
            if (index == 0) mCarPath.moveTo(pnt1Windows.x, pnt1Windows.y) else mCarPath.lineTo(
                pnt1Windows.x, pnt1Windows.y
            )
            //实时路径
            canvas.drawPath(mCarPath, mPaint)
        }


        // 绘制实时路径（cartPosList）
        synchronized(cartPosList) {
            cartPosList.forEach { pointF ->
                // 复用PointF对象
                tempPoint = mapView.worldToScreen(pointF.x, pointF.y)
                drawCircle(
                    canvas, tempPoint, radius * scale, mPaint
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
     * 外部接口:
     * 更新车行车轨迹
     */
    fun setPathList(point: PointF) {
        pathList.add(point)
        postInvalidate()
    }

    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        // 清空路径列表
        pathList.clear()

        // 清空机器人位置列表
        cartPosList.clear()
    }

}
