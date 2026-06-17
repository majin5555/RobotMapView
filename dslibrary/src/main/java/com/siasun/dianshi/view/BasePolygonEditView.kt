package com.siasun.dianshi.view

import android.content.Context
import android.graphics.*
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.ViewConfiguration
import com.siasun.dianshi.bean.PointNew
import java.lang.ref.WeakReference
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 区域编辑基类
 * 抽取了多边形区域绘制、事件处理、手势检测、添加删除顶点等公共逻辑
 */
abstract class BasePolygonEditView<T>(context: Context?, val parentMapView: WeakReference<MapView>) :
    SlamWareBaseView<MapView>(context, parentMapView), GestureDetector.OnGestureListener,
    GestureDetector.OnDoubleTapListener {

    // 区域列表 - 使用同步访问确保线程安全
    protected val list: MutableList<T> = mutableListOf()

    // 编辑模式相关变量
    protected var currentWorkMode = WorkMode.MODE_SHOW_MAP
    var selectedArea: T? = null
    protected var selectedPointIndex: Int = -1
    protected var isDragging = false
    protected var isAreaDragging = false
    protected var lastTouchX = 0f
    protected var lastTouchY = 0f
    protected val vertexRadius = 10f // 顶点半径

    // 控制是否绘制
    @JvmField
    protected var isDrawingEnabled: Boolean = true

    // 绘制相关的画笔 - 使用伴生对象创建静态实例，避免重复创建
    companion object {
        val areaPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.BLACK
            strokeWidth = 2f
            isAntiAlias = true
        }

        val selectedAreaPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.GREEN
            strokeWidth = 4f
            isAntiAlias = true
        }

        val vertexPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.GREEN
            isAntiAlias = true
        }

        val selectedVertexPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.YELLOW
            isAntiAlias = true
        }

        val edgePointPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.CYAN
            isAntiAlias = true
        }

        val edgePointTextPaint = Paint().apply {
            color = Color.BLACK
            textSize = 20f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
        }

        val textPaint = Paint().apply {
            color = Color.BLACK
            isAntiAlias = true
        }

        // 填充区域的画笔 - 透明蓝色
        val fillPaint = Paint().apply {
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

    // 复用的Path对象，避免频繁创建
    private val path = Path()

    // 复用的PointF对象，减少内存分配
    private val tempPoint = PointF()
    private val screenP1 = PointF()
    private val screenP2 = PointF()
    private val worldPoint = PointF()

    // 触摸滑动判断相关
    private var touchSlop: Int = 0
    private var downX = 0f
    private var downY = 0f
    private var isDraggingStartDelayed = false
    private var isAreaDraggingStartDelayed = false

    init {
        gestureDetector.setOnDoubleTapListener(this)
        val configuration = ViewConfiguration.get(context!!)
        touchSlop = configuration.scaledTouchSlop
    }

    // ----------------- 抽象方法 -----------------

    /**
     * 获取指定区域的顶点集合
     */
    abstract fun getAreaVertices(area: T): MutableList<PointNew>

    /**
     * 获取指定区域的名称，用于在界面上绘制
     */
    abstract fun getAreaName(area: T): String

    /**
     * 判断当前模式是否是编辑/添加模式
     */
    abstract fun isEditMode(mode: WorkMode): Boolean

    /**
     * 获取当前View特定的未选中画笔（如特殊区域的颜色不同）
     */
    open fun getSpecificAreaPaint(area: T): Paint = areaPaint

    // ----------------- 供子类实现的回调 -----------------
    open fun onSelectedAreaChangedCallback(area: T?) {}
    open fun onVertexDragStartCallback(area: T, vertexIndex: Int) {}
    open fun onVertexDraggingCallback(area: T, vertexIndex: Int, newX: Float, newY: Float) {}
    open fun onVertexDragEndCallback(area: T, vertexIndex: Int, isInsideMap: Boolean) {}
    open fun onVertexAddedCallback(area: T, vertexIndex: Int, x: Float, y: Float) {}
    open fun onEdgeRemovedCallback(area: T, edgeIndex: Int) {}
    open fun onVertexRemovedCallback(area: T, vertexIndex: Int) {}
    open fun onAreaCreatedCallback(area: T) {}
    open fun onAreaDragStartCallback(area: T) {}
    open fun onAreaDraggingCallback(area: T) {}
    open fun onAreaDragEndCallback(area: T, isInsideMap: Boolean) {}
    open fun onAreaClickCallback(area: T) {}
    open fun onValidateAndFixStartPoint(area: T) {}

    // ----------------- 公共逻辑 -----------------

    fun setWorkMode(mode: WorkMode) {
        this.currentWorkMode = mode
        if (!isEditMode(mode)) {
            selectedArea = null
            selectedPointIndex = -1
            isDragging = false
        }
        invalidate()
    }

    fun getData(): List<T> {
        return synchronized(list) {
            list.toList()
        }
    }

    fun cleanData() {
        synchronized(list) {
            list.clear()
        }
        postInvalidate()
    }

    fun setDrawingEnabled(enabled: Boolean) {
        this.isDrawingEnabled = enabled
        postInvalidate()
    }

    fun setSelectedAreaBase(area: T?) {
        this.selectedArea = area
        selectedPointIndex = -1
        isDragging = false
        onSelectedAreaChangedCallback(area)
        invalidate()
    }

    protected fun getRightmostPoint(points: List<PointNew>): PointNew? {
        if (points.isEmpty()) return null
        var rightmost = points[0]
        for (point in points) {
            if (point.X > rightmost.X) {
                rightmost = point
            }
        }
        return rightmost
    }

    protected fun isPointInPolygon(area: T?, screenX: Float, screenY: Float): Boolean {
        area ?: return false
        val mapView = parentMapView.get() ?: return false
        val worldPt = mapView.screenToWorld(screenX, screenY)
        val x = worldPt.x
        val y = worldPt.y

        var isInside = false
        val points = getAreaVertices(area)
        var j = points.size - 1
        for (i in points.indices) {
            if ((points[i].Y > y) != (points[j].Y > y) &&
                (x < (points[j].X - points[i].X) * (y - points[i].Y) / (points[j].Y - points[i].Y) + points[i].X)
            ) {
                isInside = !isInside
            }
            j = i
        }
        return isInside
    }

    protected fun ccw(A: PointNew, B: PointNew, C: PointNew): Boolean {
        return (C.Y - A.Y) * (B.X - A.X) > (B.Y - A.Y) * (C.X - A.X)
    }

    protected fun segmentsIntersect(A: PointNew, B: PointNew, C: PointNew, D: PointNew): Boolean {
        return ccw(A, C, D) != ccw(B, C, D) && ccw(A, B, C) != ccw(A, B, D)
    }

    protected fun isPolygonSelfIntersecting(points: List<PointNew>): Boolean {
        val n = points.size
        if (n < 4) return false

        for (i in 0 until n) {
            val p1 = points[i]
            val p2 = points[(i + 1) % n]
            for (j in i + 2 until n) {
                if (i == 0 && j == n - 1) continue
                val p3 = points[j]
                val p4 = points[(j + 1) % n]
                if (segmentsIntersect(p1, p2, p3, p4)) {
                    return true
                }
            }
        }
        return false
    }

    protected fun isPointInVertex(screenX: Float, screenY: Float, vertex: PointNew): Boolean {
        val mapView = parentMapView.get() ?: return false
        tempPoint.set(
            mapView.worldToScreen(vertex.X, vertex.Y).x, mapView.worldToScreen(vertex.X, vertex.Y).y
        )
        val distance = Math.sqrt(
            Math.pow((screenX - tempPoint.x).toDouble(), 2.0) + Math.pow((screenY - tempPoint.y).toDouble(), 2.0)
        )
        return distance <= vertexRadius * 2
    }

    protected fun getTopLeftVertex(points: List<PointNew>): PointNew? {
        if (points.isEmpty()) return null
        return points.minByOrNull { it.X + it.Y }
    }

    protected fun findNearbyVertexIndex(area: T?, screenX: Float, screenY: Float): Int {
        area ?: return -1
        val points = getAreaVertices(area)
        for (i in points.indices) {
            if (isPointInVertex(screenX, screenY, points[i])) {
                return i
            }
        }
        return -1
    }

    protected fun calculateMidPoint(p1: PointNew, p2: PointNew): PointNew {
        return PointNew((p1.X + p2.X) / 2f, (p1.Y + p2.Y) / 2f)
    }

    protected fun isPointInEdgePoint(screenX: Float, screenY: Float, midPoint: PointNew): Boolean {
        val mapView = parentMapView.get() ?: return false
        tempPoint.set(
            mapView.worldToScreen(midPoint.X, midPoint.Y).x,
            mapView.worldToScreen(midPoint.X, midPoint.Y).y
        )
        val distance = sqrt(
            (screenX - tempPoint.x).toDouble().pow(2.0) + Math.pow((screenY - tempPoint.y).toDouble(), 2.0)
        )
        return distance <= edgePointRadius * 2
    }

    protected fun findNearbyEdgeIndex(area: T?, screenX: Float, screenY: Float): Int {
        area ?: return -1
        val points = getAreaVertices(area)
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

    protected fun addVertexOnEdge(area: T?, edgeIndex: Int) {
        area ?: return
        val points = getAreaVertices(area)
        if (points.size < 2) return

        val p1 = points[edgeIndex]
        val p2 = points[(edgeIndex + 1) % points.size]
        val midPoint = calculateMidPoint(p1, p2)

        val newIndex = edgeIndex + 1
        points.add(newIndex, midPoint)

        if (isPolygonSelfIntersecting(points)) {
            points.removeAt(newIndex)
            return
        }

        onVertexAddedCallback(area, newIndex, midPoint.X, midPoint.Y)
        invalidate()
    }

    protected fun isPointOnLine(screenX: Float, screenY: Float, p1: PointNew, p2: PointNew): Boolean {
        val mapView = parentMapView.get() ?: return false
        val p1Screen = mapView.worldToScreen(p1.X, p1.Y)
        screenP1.set(p1Screen.x, p1Screen.y)

        val p2Screen = mapView.worldToScreen(p2.X, p2.Y)
        screenP2.set(p2Screen.x, p2Screen.y)

        val distance = calculateDistanceFromPointToLine(
            screenX, screenY, screenP1.x, screenP1.y, screenP2.x, screenP2.y
        )
        return distance <= lineClickTolerance && isPointBetween(
            screenX, screenY, screenP1.x, screenP1.y, screenP2.x, screenP2.y
        )
    }

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

    private fun isPointBetween(
        px: Float, py: Float, x1: Float, y1: Float, x2: Float, y2: Float
    ): Boolean {
        val minX = Math.min(x1, x2)
        val maxX = Math.max(x1, x2)
        val minY = Math.min(y1, y2)
        val maxY = Math.max(y1, y2)
        return px >= minX - lineClickTolerance && px <= maxX + lineClickTolerance && py >= minY - lineClickTolerance && py <= maxY + lineClickTolerance
    }

    protected fun removeEdge(area: T?, edgeIndex: Int) {
        area ?: return
        val points = getAreaVertices(area)
        if (points.size <= 3) return

        val removedIndex = edgeIndex % points.size
        val removedPoint = points.removeAt(removedIndex)

        if (isPolygonSelfIntersecting(points)) {
            points.add(removedIndex, removedPoint)
            return
        }
        onEdgeRemovedCallback(area, edgeIndex)
        invalidate()
    }

    protected fun requestRemoveVertex(area: T?, vertexIndex: Int) {
        area ?: return
        val points = getAreaVertices(area)
        if (points.size <= 3) return
        onVertexRemovedCallback(area, vertexIndex)
    }

    fun performDeleteVertex(area: T, vertexIndex: Int) {
        val points = getAreaVertices(area)
        if (points.size <= 3) return

        if (vertexIndex >= 0 && vertexIndex < points.size) {
            val removedPoint = points.removeAt(vertexIndex)
            if (isPolygonSelfIntersecting(points)) {
                points.add(vertexIndex, removedPoint)
                return
            }
            invalidate()
        }
    }

    protected fun findTouchedArea(screenX: Float, screenY: Float): T? {
        val areasCopy = synchronized(list) { list.toList() }
        selectedArea?.let { area ->
            if (areasCopy.contains(area) && isPointInPolygon(area, screenX, screenY)) {
                return area
            }
        }
        for (i in areasCopy.indices.reversed()) {
            val area = areasCopy[i]
            if (area == selectedArea) continue
            if (isPointInPolygon(area, screenX, screenY)) {
                return area
            }
        }
        return null
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (gestureDetector.onTouchEvent(event)) {
            return true
        }

        if (!isEditMode(currentWorkMode)) {
            return false
        }

        val x = event.x
        val y = event.y
        var handled = false

        if (selectedArea == null) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (findTouchedArea(x, y) != null) {
                    handled = true
                }
            }
            return handled
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                downX = x
                downY = y
                selectedPointIndex = findNearbyVertexIndex(selectedArea, x, y)
                if (selectedPointIndex != -1) {
                    isDraggingStartDelayed = true
                    handled = true
                } else {
                    val edgeIndex = findNearbyEdgeIndex(selectedArea, x, y)
                    if (edgeIndex != -1) {
                        handled = true
                    } else if (isPointInPolygon(selectedArea, x, y)) {
                        isAreaDraggingStartDelayed = true
                        handled = true
                    } else if (findTouchedArea(x, y) != null) {
                        handled = true
                    }
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDraggingStartDelayed) {
                    val dx = abs(x - downX)
                    val dy = abs(y - downY)
                    if (dx > touchSlop || dy > touchSlop) {
                        isDraggingStartDelayed = false
                        isDragging = true
                        onVertexDragStartCallback(selectedArea!!, selectedPointIndex)
                    } else {
                        handled = true
                    }
                }

                if (isAreaDraggingStartDelayed) {
                    val dx = abs(x - downX)
                    val dy = abs(y - downY)
                    if (dx > touchSlop || dy > touchSlop) {
                        isAreaDraggingStartDelayed = false
                        isAreaDragging = true
                        lastTouchX = downX
                        lastTouchY = downY
                        onAreaDragStartCallback(selectedArea!!)
                    } else {
                        handled = true
                    }
                }

                if (isDragging && selectedPointIndex != -1) {
                    val mapView = parentMapView.get() ?: return false
                    val screenToWorldPt = mapView.screenToWorld(x, y)
                    worldPoint.set(screenToWorldPt.x, screenToWorldPt.y)

                    val points = getAreaVertices(selectedArea!!)
                    val selectedPoint = points[selectedPointIndex]
                    val oldX = selectedPoint.X
                    val oldY = selectedPoint.Y

                    selectedPoint.X = worldPoint.x
                    selectedPoint.Y = worldPoint.y

                    if (isPolygonSelfIntersecting(points)) {
                        selectedPoint.X = oldX
                        selectedPoint.Y = oldY
                    } else {
                        onVertexDraggingCallback(selectedArea!!, selectedPointIndex, selectedPoint.X, selectedPoint.Y)
                    }
                    invalidate()
                    handled = true
                } else if (isAreaDragging) {
                    val mapView = parentMapView.get() ?: return false
                    val lastWorld = mapView.screenToWorld(lastTouchX, lastTouchY)
                    val currWorld = mapView.screenToWorld(x, y)
                    val dx = currWorld.x - lastWorld.x
                    val dy = currWorld.y - lastWorld.y

                    getAreaVertices(selectedArea!!).forEach { point ->
                        point.X += dx
                        point.Y += dy
                    }
                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                    onAreaDraggingCallback(selectedArea!!)
                    handled = true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDraggingStartDelayed) {
                    isDraggingStartDelayed = false
                    handled = true
                } else if (isDragging && selectedPointIndex != -1) {
                    val mapView = parentMapView.get() ?: return false
                    val isInsideMap = mapView.isInsideMap(x, y)
                    onVertexDragEndCallback(selectedArea!!, selectedPointIndex, isInsideMap)
                    onValidateAndFixStartPoint(selectedArea!!)
                    handled = true
                }

                if (isAreaDraggingStartDelayed) {
                    isAreaDraggingStartDelayed = false
                    handled = true
                } else if (isAreaDragging) {
                    val mapView = parentMapView.get() ?: return false
                    var isAllInside = true
                    for (point in getAreaVertices(selectedArea!!)) {
                        val screenPoint = mapView.worldToScreen(point.X, point.Y)
                        if (!mapView.isInsideMap(screenPoint.x, screenPoint.y)) {
                            isAllInside = false
                            break
                        }
                    }
                    onValidateAndFixStartPoint(selectedArea!!)
                    onAreaDragEndCallback(selectedArea!!, isAllInside)
                    handled = true
                }
                isDragging = false
                isAreaDragging = false
                selectedPointIndex = -1
            }
        }
        return handled
    }

    override fun onDoubleTap(e: MotionEvent): Boolean {
        if (!isEditMode(currentWorkMode)) {
            return false
        }
        val x = e.x
        val y = e.y

        if (selectedArea != null) {
            val points = getAreaVertices(selectedArea!!)
            val clickedVertexIndex = findNearbyVertexIndex(selectedArea, x, y)
            if (clickedVertexIndex != -1) {
                requestRemoveVertex(selectedArea, clickedVertexIndex)
                return true
            }

            val edgeIndex = findNearbyEdgeIndex(selectedArea, x, y)
            if (edgeIndex != -1) {
                addVertexOnEdge(selectedArea, edgeIndex)
                return true
            }

            for (i in points.indices) {
                val p1 = points[i]
                val p2 = points[(i + 1) % points.size]
                if (isPointOnLine(x, y, p1, p2)) {
                    removeEdge(selectedArea, i)
                    return true
                }
            }
        }

        val touchedArea = findTouchedArea(x, y)
        if (touchedArea != null) {
            // 这里我们给子类重写修改模式的机会
            handleDoubleTapSelectArea()

            if (selectedArea != touchedArea) {
                selectedArea = touchedArea
                selectedPointIndex = -1
                isDragging = false
                onSelectedAreaChangedCallback(selectedArea)
            }
            onAreaClickCallback(touchedArea)
            invalidate()
            return true
        }

        return false
    }

    open fun handleDoubleTapSelectArea() {}

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isDrawingEnabled) {
            canvas.save()
            val areasCopy = synchronized(list) { list.toList() }

            areasCopy.forEach { area ->
                if (area != selectedArea) {
                    drawPolygon(canvas, area, false)
                }
            }

            selectedArea?.let { area ->
                if (areasCopy.contains(area)) {
                    drawPolygon(canvas, area, true)
                }
            }
            canvas.restore()
        }
    }

    protected open fun drawPolygon(canvas: Canvas, area: T, isSelected: Boolean) {
        val points = getAreaVertices(area)
        if (points.isEmpty()) return

        val mapView = parentMapView.get() ?: return
        path.reset()

        val firstPoint = mapView.worldToScreen(points[0].X, points[0].Y)
        path.moveTo(firstPoint.x, firstPoint.y)

        for (i in 1 until points.size) {
            val screenPoint = mapView.worldToScreen(points[i].X, points[i].Y)
            path.lineTo(screenPoint.x, screenPoint.y)
        }
        path.close()

        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, if (isSelected) selectedAreaPaint else getSpecificAreaPaint(area))

        if (isEditMode(currentWorkMode) && isSelected) {
            for (i in points.indices) {
                val screenPoint = mapView.worldToScreen(points[i].X, points[i].Y)
                canvas.drawCircle(
                    screenPoint.x,
                    screenPoint.y,
                    vertexRadius,
                    if (i == selectedPointIndex) selectedVertexPaint else vertexPaint
                )
            }

            if (points.size >= 2) {
                for (i in points.indices) {
                    val p1 = points[i]
                    val p2 = points[(i + 1) % points.size]
                    val midPoint = calculateMidPoint(p1, p2)
                    val screenMidPoint = mapView.worldToScreen(midPoint.X, midPoint.Y)

                    canvas.drawCircle(screenMidPoint.x, screenMidPoint.y, edgePointRadius, edgePointPaint)
                    canvas.drawText("+", screenMidPoint.x, screenMidPoint.y + 8, edgePointTextPaint)
                }
            }
        }

        val name = getAreaName(area)
        if (name.isNotEmpty()) {
            getRightmostPoint(points)?.let { rightmost ->
                val rightmostScreen = mapView.worldToScreen(rightmost.X, rightmost.Y)
                val textRect = Rect()
                textPaint.getTextBounds(name, 0, name.length, textRect)
                val textX = rightmostScreen.x - textRect.width() / 2
                val textY = rightmostScreen.y + textRect.height() + 10
                canvas.drawText(name, textX, textY, textPaint)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        synchronized(list) {
            list.clear()
        }
        selectedArea = null
    }

    override fun onDown(e: MotionEvent): Boolean = false
    override fun onShowPress(e: MotionEvent) {}
    override fun onSingleTapUp(e: MotionEvent): Boolean = false
    override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean = false
    override fun onLongPress(e: MotionEvent) {}
    override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean = false
    override fun onSingleTapConfirmed(e: MotionEvent): Boolean = false
    override fun onDoubleTapEvent(e: MotionEvent): Boolean = false
}
