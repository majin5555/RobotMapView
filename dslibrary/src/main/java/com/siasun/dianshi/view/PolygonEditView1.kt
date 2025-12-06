package com.siasun.dianshi.view

import android.content.Context
import android.graphics.*
import com.siasun.dianshi.bean.CleanAreaNew
import com.siasun.dianshi.bean.PointNew
import java.lang.ref.WeakReference

class PolygonEditView1(context: Context?, parent: WeakReference<MapView>) :
    SlamWareBaseView(context, parent) {

    // 保存parent引用以便安全访问
    private var mapViewRef: WeakReference<MapView>? = parent
    var list: MutableList<CleanAreaNew> = mutableListOf()

    // 绘制相关的画笔
    private val areaPaint = Paint().apply {
        style = Paint.Style.STROKE
        color = Color.BLUE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 30f
        isAntiAlias = true
    }

    init {
    }

    /**
     * 设置要绘制的区域数据
     */
    fun setCleanAreaData(data: MutableList<CleanAreaNew>) {
        this.list.clear()
        this.list.addAll(data)
        invalidate() // 触发重绘
    }

    /**
     * 获取区域的最右边点
     */
    private fun getRightmostPoint(points: List<PointNew>): PointNew? {
        if (points.isEmpty()) return null

        var rightmost = points[0]
        for (point in points) {
            if (point.X > rightmost.X) {
                rightmost = point
            }
        }
        return rightmost
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()

        // 绘制所有区域
        list.forEach { area ->
            drawPolygon(canvas, area)
        }

        canvas.restore()
    }

    /**
     * 绘制单个不规则图形区域
     */
    private fun drawPolygon(canvas: Canvas, area: CleanAreaNew) {
        val points = area.m_VertexPnt
        if (points.isEmpty()) return

        // 创建路径
        val path = Path()

        // 将第一个点转换为屏幕坐标并移动到该点
        val firstPoint =mapViewRef?.get()?. worldToScreen(points[0].X,points[0].Y)
        path.moveTo(firstPoint!!.x, firstPoint!!.y)

        // 添加所有其他点到路径
        for (i in 1 until points.size) {
            val screenPoint =mapViewRef?.get()?. worldToScreen(points[i].X, points[i].Y)
            path.lineTo(screenPoint!!.x, screenPoint!!.y)
        }

        // 闭合路径
        path.close()

        // 绘制多边形轮廓
        canvas.drawPath(path, areaPaint)

        // 绘制区域名称在最右边点的下边
        getRightmostPoint(points)?.let { rightmost ->
            val rightmostScreen = mapViewRef?.get()?. worldToScreen(rightmost.X, rightmost.Y)

            // 计算文本位置：在最右边点的下方，居中对齐
            val textRect = Rect()
            textPaint.getTextBounds(area.sub_name, 0, area.sub_name.length, textRect)

            val textX = rightmostScreen!!.x - textRect.width() / 2
            val textY = rightmostScreen!!.y + textRect.height() + 10 // 10像素的间距

            // 绘制文本
            canvas.drawText(area.sub_name, textX, textY, textPaint)
        }
    }
}
