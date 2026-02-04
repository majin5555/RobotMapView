package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.GestureDetector
import android.view.MotionEvent
import com.siasun.dianshi.bean.CleanAreaNew
import com.siasun.dianshi.bean.PointNew
import java.lang.ref.WeakReference
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 清扫区域
 */
@SuppressLint("ViewConstructor")
class PolygonEditView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView<MapView>(context, parent), GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener {

    // 保存parent引用以便安全访问 - 使用非空WeakReference
    private val mapViewRef: WeakReference<MapView> = parent

    // 清扫区域列表 - 使用private和同步访问确保线程安全
    private val list: MutableList<CleanAreaNew> = mutableListOf()

    // 编辑模式相关变量
    private var currentWorkMode = WorkMode.MODE_SHOW_MAP
    private var selectedArea: CleanAreaNew? = null
    private var selectedPointIndex: Int = -1
    private var isDragging = false
    private val vertexRadius = 10f // 顶点半径

    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    // 绘制相关的画笔 - 使用伴生对象创建静态实例，避免重复创建
    companion object {
        private val areaPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = 2f
            isAntiAlias = true
        }

        private val selectedAreaPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.GREEN
            strokeWidth = 4f
            isAntiAlias = true
        }

        private val vertexPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.GREEN
            isAntiAlias = true
        }

        private val selectedVertexPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.YELLOW
            isAntiAlias = true
        }

        private val edgePointPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.CYAN
            isAntiAlias = true
        }

        private val edgePointTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        private val textPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }
        
        // 填充区域的画笔 - 透明蓝色
        private val fillPaint = Paint().apply {
            style = Paint.Style.FILL
            // 设置透明蓝色 (ARGB: 128表示半透明，0, 0, 255表示蓝色)
            color = Color.argb(50, 0, 0, 255)
            isAntiAlias = true
        }
    }

    // 边中点的半径
    private val edgePointRadius = 15f

    // 线段识别的点击精度
    private val lineClickTolerance = 10f

    // 手势检测器，用于处理双击事件
    private val gestureDetector = GestureDetector(context, this)

    // 编辑监听器 - 使用强引用确保回调能被触发
    private var onCleanAreaEditListener: OnCleanAreaEditListener? = null

    // 复用的Path对象，避免频繁创建
    private val path = Path()

    // 复用的PointF对象，减少内存分配
    private val tempPoint = PointF()
    private val screenP1 = PointF()
    private val screenP2 = PointF()
    private val worldPoint = PointF()

    init {
        gestureDetector.setOnDoubleTapListener(this)
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        this.currentWorkMode = mode
        // 如果退出编辑模式，清空选中状态
        if (mode != WorkMode.MODE_CLEAN_AREA_EDIT && mode != WorkMode.MODE_CLEAN_AREA_ADD) {
            selectedArea = null
            selectedPointIndex = -1
            isDragging = false
        }
        invalidate()
    }

    /**
     * 在地图中心创建一个矩形清扫区域
     */
    fun createRectangularAreaAtCenter(newArea: CleanAreaNew) {
//        // 获取MapView实例
//        val mapView = mapViewRef.get() ?: return
//
//        // 计算地图中心位置
//        val centerX = mapView.viewWidth / 2f
//        val centerY = mapView.viewHeight / 2f
//
//        // 将屏幕中心坐标转换为世界坐标
//        val worldCenter = mapView.screenToWorld(centerX, centerY)
//
//        // 创建矩形的四个顶点（100x100的矩形，中心在地图中心）
//        val rectSize = 20f
//        val halfSize = rectSize / 2f
//
//        val topLeft = PointNew(worldCenter.x - halfSize, worldCenter.y - halfSize)
//        val topRight = PointNew(worldCenter.x + halfSize, worldCenter.y - halfSize)
//        val bottomRight = PointNew(worldCenter.x + halfSize, worldCenter.y + halfSize)
//        val bottomLeft = PointNew(worldCenter.x - halfSize, worldCenter.y + halfSize)
//
//        // 添加顶点到区域
//        newArea.m_VertexPnt.add(topLeft)
//        newArea.m_VertexPnt.add(topRight)
//        newArea.m_VertexPnt.add(bottomRight)
//        newArea.m_VertexPnt.add(bottomLeft)
//
//        // 将新区域添加到列表
//        list.add(newArea)
//
//        // 选中新创建的区域
//        selectedArea = newArea
//
//        // 通知监听器选中区域变化
//        onCleanAreaEditListener?.onSelectedAreaChanged(selectedArea)
//
//        // 通知监听器创建了新区域
//        onCleanAreaEditListener?.onAreaCreated(newArea)
//
//        invalidate()

        val mapView = mapViewRef.get() ?: return
        val centerX = mapView.viewWidth / 2f
        val centerY = mapView.viewHeight / 2f

        val sizePx = 100f
        val halfSize = sizePx / 2f


        val topLeft =  mapView.screenToWorld(centerX - halfSize,centerY - halfSize)
        val topRight = mapView.screenToWorld(centerX + halfSize, centerY - halfSize)
        val bottomRight = mapView.screenToWorld(centerX + halfSize, centerY + halfSize)
        val bottomLeft = mapView.screenToWorld(centerX - halfSize, centerY + halfSize)


        newArea.m_VertexPnt.apply {
            clear()
            add(PointNew(topLeft.x,topLeft.y))
            add(PointNew(topRight.x,topRight.y))
            add(PointNew(bottomRight.x,bottomRight.y))
            add(PointNew(bottomLeft.x,bottomLeft.y))

        }

        list.add(newArea)
        selectedArea = newArea

        onCleanAreaEditListener?.onSelectedAreaChanged(selectedArea)
        onCleanAreaEditListener?.onAreaCreated(newArea)

        invalidate()
    }

    /**
     * 设置编辑监听器
     */
    fun setOnCleanAreaEditListener(listener: OnCleanAreaEditListener?) {
        this.onCleanAreaEditListener = listener
    }

    /**
     * 设置要编辑的区域
     */
    fun setSelectedArea(area: CleanAreaNew?) {
        this.selectedArea = area
        selectedPointIndex = -1
        isDragging = false
        // 通知监听器选中区域变化
        onCleanAreaEditListener?.onSelectedAreaChanged(area)
        invalidate()
    }

    /**
     * 设置要绘制的区域数据
     */
    fun setCleanAreaData(data: MutableList<CleanAreaNew>) {
        synchronized(list) {
            this.list.clear()
            this.list.addAll(data)
        }
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

    /**
     * 检查点是否在顶点的可点击范围内
     */
    private fun isPointInVertex(screenX: Float, screenY: Float, vertex: PointNew): Boolean {
        val mapView = mapViewRef.get() ?: return false
        tempPoint.set(
            mapView.worldToScreen(vertex.X, vertex.Y).x,
            mapView.worldToScreen(vertex.X, vertex.Y).y
        )
        val distance = Math.sqrt(
            Math.pow(
                (screenX - tempPoint.x).toDouble(),
                2.0
            ) + Math.pow((screenY - tempPoint.y).toDouble(), 2.0)
        )
        return distance <= vertexRadius * 2
    }

    /**
     * 查找并返回点击位置附近的顶点索引
     */
    private fun findNearbyVertexIndex(area: CleanAreaNew, screenX: Float, screenY: Float): Int {
        for (i in area.m_VertexPnt.indices) {
            if (isPointInVertex(screenX, screenY, area.m_VertexPnt[i])) {
                return i
            }
        }
        return -1
    }

    /**
     * 计算两点之间的中点
     */
    private fun calculateMidPoint(p1: PointNew, p2: PointNew): PointNew {
        return PointNew((p1.X + p2.X) / 2f, (p1.Y + p2.Y) / 2f)
    }

    /**
     * 检查点是否在边中点的可点击范围内
     */
    private fun isPointInEdgePoint(screenX: Float, screenY: Float, midPoint: PointNew): Boolean {
        val mapView = mapViewRef.get() ?: return false
        tempPoint.set(
            mapView.worldToScreen(midPoint.X, midPoint.Y).x,
            mapView.worldToScreen(midPoint.X, midPoint.Y).y
        )
        val distance = sqrt(
            (screenX - tempPoint.x).toDouble()
                .pow(2.0) + Math.pow((screenY - tempPoint.y).toDouble(), 2.0)
        )
        return distance <= edgePointRadius * 2
    }

    /**
     * 查找并返回点击位置附近的边索引
     */
    private fun findNearbyEdgeIndex(area: CleanAreaNew, screenX: Float, screenY: Float): Int {
        val points = area.m_VertexPnt
        if (points.size < 2) return -1

        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % points.size]
            val midPoint = calculateMidPoint(p1, p2)
            if (isPointInEdgePoint(screenX, screenY, midPoint)) {
                return i
            }
        }
        return -1
    }

    /**
     * 在边上添加新顶点
     */
    private fun addVertexOnEdge(area: CleanAreaNew, edgeIndex: Int) {
        val points = area.m_VertexPnt
        if (points.size < 2) return

        val p1 = points[edgeIndex]
        val p2 = points[(edgeIndex + 1) % points.size]
        val midPoint = calculateMidPoint(p1, p2)

        // 在edgeIndex + 1位置插入新顶点
        val newIndex = edgeIndex + 1
        points.add(newIndex, midPoint)
        // 通知监听器添加了新顶点
        onCleanAreaEditListener?.onVertexAdded(area, newIndex, midPoint.X, midPoint.Y)
        invalidate()
    }

    /**
     * 检查点是否在线段上
     */
    private fun isPointOnLine(screenX: Float, screenY: Float, p1: PointNew, p2: PointNew): Boolean {
        val mapView = mapViewRef.get() ?: return false

        val p1Screen = mapView.worldToScreen(p1.X, p1.Y)
        screenP1.set(p1Screen.x, p1Screen.y)

        val p2Screen = mapView.worldToScreen(p2.X, p2.Y)
        screenP2.set(p2Screen.x, p2Screen.y)

        // 计算点到线段的距离
        val distance = calculateDistanceFromPointToLine(
            screenX, screenY, screenP1.x, screenP1.y, screenP2.x, screenP2.y
        )

        // 检查距离是否在容忍范围内，并且点在线段的延长线上
        return distance <= lineClickTolerance && isPointBetween(
            screenX,
            screenY,
            screenP1.x,
            screenP1.y,
            screenP2.x,
            screenP2.y
        )
    }

    /**
     * 计算点到线段的距离
     */
    private fun calculateDistanceFromPointToLine(
        px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float
    ): Float {
        val A = px - x1
        val B = py - y1
        val C = x2 - x1
        val D = y2 - y1

        val dot = A * C + B * D
        val lenSq = C * C + D * D
        var param = -1f
        if (lenSq != 0f) {
            param = dot / lenSq
        }

        val xx: Float
        val yy: Float

        if (param < 0f) {
            xx = x1
            yy = y1
        } else if (param > 1f) {
            xx = x2
            yy = y2
        } else {
            xx = x1 + param * C
            yy = y1 + param * D
        }

        val dx = px - xx
        val dy = py - yy
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    /**
     * 检查点是否在线段的两个端点之间
     */
    private fun isPointBetween(
        px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float
    ): Boolean {
        // 检查点是否在线段的包围盒内
        val minX = Math.min(x1, x2)
        val maxX = Math.max(x1, x2)
        val minY = Math.min(y1, y2)
        val maxY = Math.max(y1, y2)

        return px >= minX - lineClickTolerance && px <= maxX + lineClickTolerance && py >= minY - lineClickTolerance && py <= maxY + lineClickTolerance
    }

    /**
     * 删除指定的边
     */
    private fun removeEdge(area: CleanAreaNew, edgeIndex: Int) {
        val points = area.m_VertexPnt
        // 确保删除后还有至少3个顶点，保持多边形有效
        if (points.size <= 3) return

        // 删除边上的第二个点（即edgeIndex+1位置的点），这样就删除了edgeIndex对应的边
        points.removeAt((edgeIndex + 1) % points.size)
        // 通知监听器删除了边
        onCleanAreaEditListener?.onEdgeRemoved(area, edgeIndex)
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 先处理手势事件
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }

        if ((currentWorkMode != WorkMode.MODE_CLEAN_AREA_EDIT && currentWorkMode != WorkMode.MODE_CLEAN_AREA_ADD) || selectedArea == null) {
            return false
        }

        val x = event.x
        val y = event.y
        var handled = false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 查找点击位置附近的顶点
                selectedPointIndex = findNearbyVertexIndex(selectedArea!!, x, y)

                if (selectedPointIndex != -1) {
                    isDragging = true
                    handled = true
                    // 通知监听器顶点开始拖动
                    onCleanAreaEditListener?.onVertexDragStart(selectedArea!!, selectedPointIndex)
                } else {
                    // 如果没有点击到顶点，检查是否点击到边中点
                    val edgeIndex = findNearbyEdgeIndex(selectedArea!!, x, y)
                    if (edgeIndex != -1) {
                        // 在边上添加新顶点
                        addVertexOnEdge(selectedArea!!, edgeIndex)
                        handled = true
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging && selectedPointIndex != -1) {
                    // 将屏幕坐标转换为世界坐标
                    val mapView = mapViewRef.get() ?: return false
                    val screenToWorldPoint = mapView.screenToWorld(x, y)
                    worldPoint.set(screenToWorldPoint.x, screenToWorldPoint.y)
                    // 更新选中顶点的坐标
                    selectedArea?.m_VertexPnt?.get(selectedPointIndex)?.apply {
                        X = worldPoint.x
                        Y = worldPoint.y
                        // 通知监听器顶点拖动中
                        onCleanAreaEditListener?.onVertexDragging(
                            selectedArea!!, selectedPointIndex, worldPoint.x, worldPoint.y
                        )
                    }
                    invalidate() // 触发重绘
                    handled = true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging && selectedPointIndex != -1) {
                    // 通知监听器顶点拖动结束
                    onCleanAreaEditListener?.onVertexDragEnd(selectedArea!!, selectedPointIndex)
                    handled = true
                }
                isDragging = false
                selectedPointIndex = -1
            }
        }
        // 如果没有处理任何事件，返回false让事件传递给父视图（MapView），支持地图拖拽缩放
        return handled
    }

    // GestureDetector.OnGestureListener 接口方法
    override fun onDown(e: MotionEvent): Boolean {
        return false
    }

    override fun onShowPress(e: MotionEvent) {
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return false
    }

    override fun onScroll(
        e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float
    ): Boolean {
        return false
    }

    override fun onLongPress(e: MotionEvent) {
    }

    override fun onFling(
        e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float
    ): Boolean {
        return false
    }

    // GestureDetector.OnDoubleTapListener 接口方法
    override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
        return false
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        if ((currentWorkMode != WorkMode.MODE_CLEAN_AREA_EDIT && currentWorkMode != WorkMode.MODE_CLEAN_AREA_ADD) || selectedArea == null) {
            return false
        }

        val x = e.x
        val y = e.y
        val points = selectedArea!!.m_VertexPnt

        // 先检查是否双击在顶点上
        val clickedVertexIndex = findNearbyVertexIndex(selectedArea!!, x, y)
        if (clickedVertexIndex != -1) {
            // 删除与该顶点相关的边（删除下一条边，即顶点clickedVertexIndex和(clickedVertexIndex+1)%points.size之间的边）
            removeEdge(selectedArea!!, clickedVertexIndex)
            return true
        }

        // 查找双击位置所在的线段
        for (i in points.indices) {
            val p1 = points[i]
            val p2 = points[(i + 1) % points.size]

            if (isPointOnLine(x, y, p1, p2)) {
                // 删除该线段
                removeEdge(selectedArea!!, i)
                return true
            }
        }

        return false
    }

    override fun onDoubleTapEvent(e: MotionEvent): Boolean {
        return false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isDrawingEnabled) {
            canvas.save()
            // 绘制所有区域 - 使用副本避免并发修改
            val areasCopy = synchronized(list) {
                list.toList()
            }
            areasCopy.forEach { area ->
                drawPolygon(canvas, area, area == selectedArea)
            }

            canvas.restore()
        }
    }

    /**
     * 绘制单个不规则图形区域
     */
    private fun drawPolygon(canvas: Canvas, area: CleanAreaNew, isSelected: Boolean) {
        val points = area.m_VertexPnt
        if (points.isEmpty()) return

        val mapView = mapViewRef.get() ?: return

        // 复用Path对象
        path.reset()

        // 将第一个点转换为屏幕坐标并移动到该点
        val firstPoint = mapView.worldToScreen(points[0].X, points[0].Y)
        path.moveTo(firstPoint.x, firstPoint.y)

        // 添加所有其他点到路径
        for (i in 1 until points.size) {
            val screenPoint = mapView.worldToScreen(points[i].X, points[i].Y)
            path.lineTo(screenPoint.x, screenPoint.y)
        }

        // 闭合路径
        path.close()

        // 绘制填充区域（透明蓝色）
        canvas.drawPath(path, fillPaint)

        // 绘制多边形轮廓，选中的区域使用不同的画笔
        canvas.drawPath(path, if (isSelected) selectedAreaPaint else areaPaint)

        // 如果是编辑、添加或删除模式且区域被选中，绘制所有顶点和边中点
        if ((currentWorkMode == WorkMode.MODE_CLEAN_AREA_EDIT || currentWorkMode == WorkMode.MODE_CLEAN_AREA_ADD) && isSelected) {
            // 绘制所有顶点
            for (i in points.indices) {
                val screenPoint = mapView.worldToScreen(points[i].X, points[i].Y)
                // 绘制顶点，选中的顶点使用不同的颜色
                canvas.drawCircle(
                    screenPoint.x,
                    screenPoint.y,
                    vertexRadius,
                    if (i == selectedPointIndex) selectedVertexPaint else vertexPaint
                )
            }

            // 绘制所有边中点的加号按钮
            if (points.size >= 2) {
                for (i in points.indices) {
                    val p1 = points[i]
                    val p2 = points[(i + 1) % points.size]
                    val midPoint = calculateMidPoint(p1, p2)
                    val screenMidPoint = mapView.worldToScreen(midPoint.X, midPoint.Y)

                    // 绘制边中点的背景圆
                    canvas.drawCircle(
                        screenMidPoint.x, screenMidPoint.y, edgePointRadius, edgePointPaint
                    )

                    // 绘制加号
                    canvas.drawText("+", screenMidPoint.x, screenMidPoint.y + 8, edgePointTextPaint)
                }
            }
        }

        // 绘制区域名称在最右边点的下边
        getRightmostPoint(points)?.let { rightmost ->
            val rightmostScreen = mapView.worldToScreen(rightmost.X, rightmost.Y)

            // 计算文本位置：在最右边点的下方，居中对齐
            val textRect = Rect()
            textPaint.getTextBounds(area.sub_name, 0, area.sub_name.length, textRect)

            val textX = rightmostScreen.x - textRect.width() / 2
            val textY = rightmostScreen.y + textRect.height() + 10 // 10像素的间距

            // 绘制文本
            canvas.drawText(area.sub_name, textX, textY, textPaint)
        }
    }

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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        // 清理资源，防止内存泄漏
        synchronized(list) {
            list.clear()
        }

        selectedArea = null
        onCleanAreaEditListener = null
    }

    // 清扫区域编辑回调接口
    interface OnCleanAreaEditListener {
        // 选中区域变化
        fun onSelectedAreaChanged(area: CleanAreaNew?) {}

        // 顶点开始拖动
        fun onVertexDragStart(area: CleanAreaNew, vertexIndex: Int) {}

        // 顶点拖动中
        fun onVertexDragging(area: CleanAreaNew, vertexIndex: Int, newX: Float, newY: Float) {}

        // 顶点拖动结束
        fun onVertexDragEnd(area: CleanAreaNew, vertexIndex: Int)

        // 添加了新顶点
        fun onVertexAdded(area: CleanAreaNew, vertexIndex: Int, x: Float, y: Float)

        // 删除了边
        fun onEdgeRemoved(area: CleanAreaNew, edgeIndex: Int)

        // 创建了新区域
        fun onAreaCreated(area: CleanAreaNew) {}
    }
}
