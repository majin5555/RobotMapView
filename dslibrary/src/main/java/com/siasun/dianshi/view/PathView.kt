package com.siasun.dianshi.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import com.siasun.dianshi.bean.TeachPoint
import java.lang.ref.WeakReference

/**
 * 路线
 */
class PathView(context: Context?, parent: WeakReference<MapView>) :
    SlamWareBaseView(context, parent) {
    private var mTeachPaint: Paint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 1f
        isAntiAlias = true
        style = Paint.Style.FILL
    }

    //试教中的绿色的点的集合
    private val teachPointList = mutableListOf<TeachPoint>()

    init {

    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //绘制试教中的点
        teachPointList.forEach {
            drawTeachPointIng(canvas, it)
        }
    }

    /**
     * 绘制示教点
     *
     */
    private fun drawTeachPointIng(canvas: Canvas, point: TeachPoint) {
        val mapView = mParent.get() ?: return
        val pnt: PointF = mapView.worldToScreen(point.x.toFloat(), point.y.toFloat())
        drawCircle(canvas, pnt, 10f, mTeachPaint)
    }

    /**
     * 外部接口: 设置试教点 试教中
     */
    fun setTeachPoint(point: TeachPoint) {
        teachPointList.add(point)
    }

    /**
     * 外部接口: 设置试教点 清除
     */
    fun clearTeachPoint() {
        teachPointList.clear()
    }

    fun setStations() {
        postInvalidate()
    }
}
