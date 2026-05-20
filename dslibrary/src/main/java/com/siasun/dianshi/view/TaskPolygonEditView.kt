package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.util.Log
import android.view.MotionEvent
import com.siasun.dianshi.bean.CleanAreaNew
import com.siasun.dianshi.bean.PointNew
import java.lang.ref.WeakReference

/**
 * 新建，编辑任务 的清扫区域（精简版：仅展示和点击选中）
 */
@SuppressLint("ViewConstructor")
class TaskPolygonEditView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView<MapView>(context, parent) {

    private val mapViewRef: WeakReference<MapView> = parent
    private val list: MutableList<CleanAreaNew> = mutableListOf()

    // 回调监听
    private var onTaskAreaSelectedListener: OnTaskAreaSelectedListener? = null

    // 触摸判断相关
    private var downX = 0f
    private var downY = 0f
    private var isMultiTouch = false
    private val touchSlop = android.view.ViewConfiguration.get(context!!).scaledTouchSlop

    // 绘制复用对象
    private val path = Path()
    private val textRect = Rect()

    // 矩阵批量转换优化复用对象
    private val worldToScreenMatrix = Matrix()
    private val srcPoints = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f)
    private val dstPoints = FloatArray(6)
    private var floatBuffer = FloatArray(100)

    // 文本缓存优化
    private val textBoundsCache = mutableMapOf<String, Rect>()
    private val rightmostIndexCache = mutableMapOf<Int, Int>()

    companion object {
        private val unselectedAreaPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.GRAY // 默认灰色
            strokeWidth = 2f
            isAntiAlias = true
        }
        private val unselectedFillPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.argb(50, 128, 128, 128) // 灰色半透明
            isAntiAlias = true
        }

        private val highlightAreaPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.GREEN // 已经区域绿色
            strokeWidth = 4f
            isAntiAlias = true
        }
        private val highlightFillPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.argb(50, 0, 255, 0) // 绿色半透明
            isAntiAlias = true
        }

        private val selectedAreaPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.BLUE // 当前已选蓝色
            strokeWidth = 4f
            isAntiAlias = true
        }
        private val selectedFillPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.argb(50, 0, 0, 255) // 蓝色半透明
            isAntiAlias = true
        }

        private val textPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }
    }

    /**
     * 设置回调监听
     */
    fun setOnTaskAreaSelectedListener(listener: OnTaskAreaSelectedListener?) {
        this.onTaskAreaSelectedListener = listener
    }

    /**
     * 设置要绘制的区域数据
     */
    fun setCleanAreaData(data: MutableList<CleanAreaNew>) {
        synchronized(list) {
            this.list.clear()
            this.list.addAll(data)
            
            // 预先计算最右侧点的索引以避免在onDraw中循环
            rightmostIndexCache.clear()
            for (area in data) {
                getRightmostPointIndex(area.m_VertexPnt)?.let {
                    rightmostIndexCache[area.regId] = it
                }
            }
        }
        invalidate()
    }

    /**
     * 获取清扫区域
     */
    fun getData(): List<CleanAreaNew> {
        return synchronized(list) {
            list.toList()
        }
    }

    /**
     * 清除清扫区域
     */
    fun cleanData() {
        synchronized(list) {
            list.clear()
            rightmostIndexCache.clear()
        }
        postInvalidate()
    }

    /**
     * 设置当前选中的区域（蓝色），可以选择是否触发回调
     */
    fun setSelectedCleanArea(area: CleanAreaNew?, notify: Boolean = false) {
        synchronized(list) {
            list.forEach { it.isSelected = (it.regId == area?.regId) }
        }
        if (notify) {
            onTaskAreaSelectedListener?.onSelectedAreaChanged(area)
        }
        invalidate()
    }

    /**
     * 批量设置要高亮（绿色）显示的区域，传null或空列表则清除高亮
     */
    fun setHighlightAreas(areas: List<CleanAreaNew>?) {
        if (areas == null) {
            synchronized(list) {
                list.forEach { it.isHighlighted = false }
            }
        } else {
            val highlightIds = areas.map { it.regId }
            synchronized(list) {
                list.forEach { it.isHighlighted = highlightIds.contains(it.regId) }
            }
        }
        invalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {

        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                downY = y
                isMultiTouch = false
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                // 标记为多指操作（缩放等），取消单指点击判定
                isMultiTouch = true
            }

            MotionEvent.ACTION_UP -> {
                if (!isMultiTouch) {
                    val dx = Math.abs(x - downX)
                    val dy = Math.abs(y - downY)
                    if (dx <= touchSlop && dy <= touchSlop) {
                        // 视为点击事件
                        handleMapClick(x, y)
                    }
                }
            }
        }

        // 主动将手势事件传递给MapView处理，以支持原生的底图缩放（Pinch）与平移（Drag）操作
        mapViewRef.get()?.processMapGestures(event)

        // 必须返回true消费此事件，否则系统不会分发后续的 ACTION_MOVE 和 ACTION_UP 事件，导致点击判定失效
        return true
    }

    private fun handleMapClick(screenX: Float, screenY: Float) {
        val mapView = mapViewRef.get() ?: return
        val worldPoint = mapView.screenToWorld(screenX, screenY)

        var clickedArea: CleanAreaNew? = null
        synchronized(list) {
            // 倒序遍历，优先响应最上层的区域
            for (i in list.indices.reversed()) {
                val area = list[i]
                if (isPointInPolygon(worldPoint.x, worldPoint.y, area.m_VertexPnt)) {
                    clickedArea = area
                    break
                }
            }
        }

        if (clickedArea != null) {
            setSelectedCleanArea(clickedArea, true)
        }
    }

    /**
     * 判断世界坐标点是否在多边形内（射线法）
     */
    private fun isPointInPolygon(worldX: Float, worldY: Float, points: List<PointNew>): Boolean {
        var isInside = false
        var j = points.size - 1
        for (i in points.indices) {
            if ((points[i].Y > worldY) != (points[j].Y > worldY) && (worldX < (points[j].X - points[i].X) * (worldY - points[i].Y) / (points[j].Y - points[i].Y) + points[i].X)) {
                isInside = !isInside
            }
            j = i
        }
        return isInside
    }

    private fun updateWorldToScreenMatrix() {
        val mapView = mapViewRef.get() ?: return
        val p1 = mapView.worldToScreen(0f, 0f)
        dstPoints[0] = p1.x
        dstPoints[1] = p1.y

        val p2 = mapView.worldToScreen(1f, 0f)
        dstPoints[2] = p2.x
        dstPoints[3] = p2.y

        val p3 = mapView.worldToScreen(0f, 1f)
        dstPoints[4] = p3.x
        dstPoints[5] = p3.y

        worldToScreenMatrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 3)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        updateWorldToScreenMatrix()

        canvas.save()
        synchronized(list) {
            for (i in list.indices) {
                val area = list[i]
                drawPolygon(canvas, area, area.isSelected, area.isHighlighted)
            }
        }
        canvas.restore()
    }

    private fun drawPolygon(canvas: Canvas, area: CleanAreaNew, isSelected: Boolean, isHighlighted: Boolean) {
        val points = area.m_VertexPnt
        if (points.size < 3) return

        val size = points.size * 2
        if (floatBuffer.size < size) {
            floatBuffer = FloatArray(size * 2)
        }

        for (i in points.indices) {
            floatBuffer[i * 2] = points[i].X
            floatBuffer[i * 2 + 1] = points[i].Y
        }

        worldToScreenMatrix.mapPoints(floatBuffer, 0, floatBuffer, 0, points.size)

        path.reset()
        path.moveTo(floatBuffer[0], floatBuffer[1])
        for (i in 1 until points.size) {
            path.lineTo(floatBuffer[i * 2], floatBuffer[i * 2 + 1])
        }
        path.close()

        val fillPaintToUse = when {
            isSelected -> selectedFillPaint
            isHighlighted -> highlightFillPaint
            else -> unselectedFillPaint
        }
        canvas.drawPath(path, fillPaintToUse)
        
        val paintToUse = when {
            isSelected -> selectedAreaPaint
            isHighlighted -> highlightAreaPaint
            else -> unselectedAreaPaint
        }
        canvas.drawPath(path, paintToUse)

        // 绘制区域名称在最右边点的下边
        var rightIndex = rightmostIndexCache[area.regId]
        if (rightIndex == null) {
            rightIndex = getRightmostPointIndex(points)
            if (rightIndex != null) {
                rightmostIndexCache[area.regId] = rightIndex
            }
        }
        
        if (rightIndex != null && rightIndex < points.size) {
            val screenX = floatBuffer[rightIndex * 2]
            val screenY = floatBuffer[rightIndex * 2 + 1]

            // 缓存文本边界
            var bounds = textBoundsCache[area.sub_name]
            if (bounds == null) {
                bounds = Rect()
                textPaint.getTextBounds(area.sub_name, 0, area.sub_name.length, bounds)
                textBoundsCache[area.sub_name] = bounds
            }
            
            val textX = screenX - bounds.width() / 2f
            val textY = screenY + bounds.height() + 10f // 10像素间距
            canvas.drawText(area.sub_name, textX, textY, textPaint)
        }
    }

    private fun getRightmostPointIndex(points: List<PointNew>): Int? {
        if (points.isEmpty()) return null
        var rightmostIndex = 0
        var maxX = points[0].X
        for (i in 1 until points.size) {
            if (points[i].X > maxX) {
                maxX = points[i].X
                rightmostIndex = i
            }
        }
        return rightmostIndex
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        synchronized(list) {
            list.clear()
            rightmostIndexCache.clear()
            textBoundsCache.clear()
        }
        onTaskAreaSelectedListener = null
    }

    interface OnTaskAreaSelectedListener {
        fun onSelectedAreaChanged(area: CleanAreaNew?)
    }
}
