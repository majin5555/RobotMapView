package com.siasun.dianshi.view

import VirWallLayerNew
import VirtualWallLineNew
import VirtualWallNew
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import com.siasun.dianshi.bean.PointNew
import java.lang.ref.WeakReference
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 虚拟墙的View
 */
@SuppressLint("ViewConstructor")
class VirtualWallView(
    context: Context?, val parent: WeakReference<MapView>
) : SlamWareBaseView<MapView>(context, parent) {
    private val LINE_WIDTH = 2f
    private var radius = 5f

    private val PROPORTION = 1000//虚拟墙文件上的是毫米 在本地显示要除1000
    private val MIN_WALL_LENGTH = 100f // 最小虚拟墙长度（毫米），防止添加过短的虚拟墙

    //虚拟墙
    private var virtualWall: VirtualWallNew = VirtualWallNew(1, mutableListOf<VirWallLayerNew>())

    fun getVirtualWall(): VirtualWallNew {
        return virtualWall
    }

    // 当前工作模式
    private var currentWorkMode: WorkMode = WorkMode.MODE_SHOW_MAP

    // 创建虚拟墙相关变量
    private var isCreating = false
    private var startPoint: PointF? = null
    private var currentPoint: PointF? = null
    private var selectedConfig = 3 // 默认创建普通虚拟墙

    // 编辑虚拟墙相关变量
    private var selectedLineIndex = -1
    private var isEditing = false
    private var touchedPointIndex = -1 // 0-起点，1-终点

    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    // 边缘平移相关变量
    private var panRunnable: Runnable? = null
    private var isPanning = false
    private var panDx = 0
    private var panDy = 0
    private var lastEventX = 0f
    private var lastEventY = 0f
    private var lastWorldX = 0f
    private var lastWorldY = 0f

    // 坐标转换复用对象，防止内存抖动
    private var worldCoords = FloatArray(0)
    private var screenCoords = FloatArray(0)
    private val transformMatrix = android.graphics.Matrix()
    private val srcPoints = floatArrayOf(0f, 0f, 1f, 0f, 0f, 1f)
    private val dstPoints = FloatArray(6)

    // 拖动阈值，防止误触
    private var touchSlop = 0
    private var isDragging = false
    private var initialTouchX = 0f
    private var initialTouchY = 0f

    init {
        context?.let {
            touchSlop = android.view.ViewConfiguration.get(it).scaledTouchSlop
        }
    }

    // 用于绘制虚线的路径效果
    private val dashPathEffect: DashPathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)

    // 虚拟墙点击回调接口
    interface OnVirtualWallClickListener {
        fun onVirtualWallClick(lineIndex: Int, config: Int)
    }

    // 虚拟墙点击监听器
    private var virtualWallClickListener: OnVirtualWallClickListener? = null

    // 设置虚拟墙点击监听器
    fun setOnVirtualWallClickListener(listener: OnVirtualWallClickListener) {
        this.virtualWallClickListener = listener
    }

    // 伴生对象存储画笔，避免重复创建
    companion object {
        private val mPaint: Paint = Paint().apply {
            isAntiAlias = true
            color = Color.BLUE
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        private val mSelectedPaint: Paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = Color.GREEN
            strokeWidth = 4f
        }
    }


    /***
     * 1重点虚拟墙
     * 2虚拟门
     * 3普通虚拟墙
     */
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mapView = parent.get() ?: return
        if (isDrawingEnabled) {
            if (virtualWall.LAYER.size > 0) virtualWall.LAYER[0].let { layer ->
                mPaint.strokeWidth = LINE_WIDTH
                mSelectedPaint.strokeWidth = 4f
                val scaledRadius = radius

                val lines = layer.LINE
                val numLines = lines.size
                if (numLines > 0) {
                    val requiredSize = numLines * 4
                    if (worldCoords.size < requiredSize) {
                        worldCoords = FloatArray(requiredSize * 2)
                        screenCoords = FloatArray(requiredSize * 2)
                    }

                    // Fill world coordinates
                    for (i in 0 until numLines) {
                        val line = lines[i]
                        worldCoords[i * 4] = line.BEGIN.X / PROPORTION
                        worldCoords[i * 4 + 1] = line.BEGIN.Y / PROPORTION
                        worldCoords[i * 4 + 2] = line.END.X / PROPORTION
                        worldCoords[i * 4 + 3] = line.END.Y / PROPORTION
                    }

                    // Calculate transformation matrix
                    val p0 = mapView.worldToScreen(0f, 0f)
                    val p1 = mapView.worldToScreen(1f, 0f)
                    val p2 = mapView.worldToScreen(0f, 1f)
                    dstPoints[0] = p0.x; dstPoints[1] = p0.y
                    dstPoints[2] = p1.x; dstPoints[3] = p1.y
                    dstPoints[4] = p2.x; dstPoints[5] = p2.y

                    transformMatrix.setPolyToPoly(srcPoints, 0, dstPoints, 0, 3)

                    // Map all points
                    transformMatrix.mapPoints(screenCoords, 0, worldCoords, 0, numLines * 2)

                    for (i in 0 until numLines) {
                        val line = lines[i]
                        val startX = screenCoords[i * 4]
                        val startY = screenCoords[i * 4 + 1]
                        val endX = screenCoords[i * 4 + 2]
                        val endY = screenCoords[i * 4 + 3]

                        // 根据 CONFIG 值设置不同的画笔样式和颜色
                        when (line.CONFIG) {
                            1 -> {
                                // 红色实线 重点虚拟墙
                                mPaint.color = Color.RED
                                mPaint.pathEffect = null
                            }

                            2 -> {
                                // 红色虚线 虚拟门
                                mPaint.color = Color.RED
                                mPaint.pathEffect = dashPathEffect
                            }

                            3 -> {
                                // 蓝色实线 普通虚拟墙
                                mPaint.color = Color.BLUE
                                mPaint.pathEffect = null
                            }
                        }

                        // 如果是选中的线，使用选中画笔
                        if (currentWorkMode == WorkMode.MODE_VIRTUAL_WALL_EDIT && i == selectedLineIndex) {
                            canvas.drawLine(startX, startY, endX, endY, mSelectedPaint)

                            // 绘制绿色实心端点圆
                            val originalSelectedStyle = mSelectedPaint.style
                            mSelectedPaint.style = Paint.Style.FILL
                            canvas.drawCircle(startX, startY, scaledRadius, mSelectedPaint)
                            canvas.drawCircle(endX, endY, scaledRadius, mSelectedPaint)
                            mSelectedPaint.style = originalSelectedStyle
                        } else {
                            canvas.drawLine(startX, startY, endX, endY, mPaint)

                            // 绘制实心端点圆
                            val originalStyle = mPaint.style
                            mPaint.style = Paint.Style.FILL
                            canvas.drawCircle(startX, startY, scaledRadius, mPaint)
                            canvas.drawCircle(endX, endY, scaledRadius, mPaint)
                            mPaint.style = originalStyle
                        }
                    }
                }
            }

            // 绘制正在创建的虚拟墙
            if (isCreating && startPoint != null && currentPoint != null) {
                mPaint.strokeWidth = LINE_WIDTH
                when (selectedConfig) {
                    1 -> {
                        mPaint.color = Color.RED
                        mPaint.pathEffect = null
                    }

                    2 -> {
                        mPaint.color = Color.RED
                        mPaint.pathEffect = dashPathEffect
                    }

                    3 -> {
                        mPaint.color = Color.BLUE
                        mPaint.pathEffect = null
                    }
                }
                canvas.drawLine(
                    startPoint!!.x, startPoint!!.y, currentPoint!!.x, currentPoint!!.y, mPaint
                )
                val scaledRadius = radius
                // 绘制实心端点圆
                val originalStyle = mPaint.style
                mPaint.style = Paint.Style.FILL
                canvas.drawCircle(startPoint!!.x, startPoint!!.y, scaledRadius, mPaint)
                canvas.drawCircle(currentPoint!!.x, currentPoint!!.y, scaledRadius, mPaint)
                mPaint.style = originalStyle
            }
        }
    }

    /**
     * 设置虚拟墙
     */
    fun setVirtualWall(virtualWall: VirtualWallNew) {
        this.virtualWall = virtualWall
        // 确保至少有一个图层
        if (this.virtualWall.LAYER.isEmpty()) {
            this.virtualWall.LAYER.add(VirWallLayerNew(ArrayList(), 0, 0))
        }
        postInvalidate()
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        currentWorkMode = mode
        // 重置状态
        if (mode != WorkMode.MODE_VIRTUAL_WALL_ADD) {
            isCreating = false
            startPoint = null
            currentPoint = null
        }
        if (mode != WorkMode.MODE_VIRTUAL_WALL_EDIT) {
            selectedLineIndex = -1
            isEditing = false
            touchedPointIndex = -1
        }
        if (mode != WorkMode.MODE_VIRTUAL_WALL_TYPE_EDIT) {
            selectedLineIndex = -1
        }
        stopMapPanning()
        postInvalidate()
    }

    /**
     * 添加虚拟墙
     */
    fun addVirtualWall(config: Int) {
        selectedConfig = config
    }

    /**
     * 确认编辑虚拟墙
     */
    fun confirmEditVirtualWall() {
        // 可以在这里添加保存编辑的逻辑
        selectedLineIndex = -1
        isEditing = false
        touchedPointIndex = -1
    }

    /**
     * 检查点是否在圆内
     */
    private fun isPointInCircle(point: PointF, center: PointF, radius: Float): Boolean {
        val dx = point.x - center.x
        val dy = point.y - center.y
        return dx * dx + dy * dy <= radius * radius
    }

    /**
     * 查找距离点最近的虚拟墙
     */
    private fun findNearestLine(point: PointF): Int {
        val mapView = parent.get() ?: return -1
        
        // 动态计算点击阈值：考虑地图缩放级别，缩放越大（放大）阈值适当增加，但设置上下限保证体验
        val endpointThreshold = (30f * scale).coerceIn(30f, 80f)
        val lineThreshold = (40f * scale).coerceIn(40f, 100f)

        if (virtualWall.LAYER.isEmpty() || virtualWall.LAYER[0].LINE.isEmpty()) {
            return -1
        }

        var nearestEndpointIndex = -1
        var minEndpointDist = Float.MAX_VALUE

        var nearestLineIndex = -1
        var minLineDist = Float.MAX_VALUE

        // 遍历所有虚拟墙，寻找绝对距离最近的端点或线段
        for ((index, line) in virtualWall.LAYER[0].LINE.withIndex()) {
            val start = mapView.worldToScreen(line.BEGIN.X / PROPORTION, line.BEGIN.Y / PROPORTION)
            val end = mapView.worldToScreen(line.END.X / PROPORTION, line.END.Y / PROPORTION)

            // 1. 计算到端点的距离
            val dxStart = point.x - start.x
            val dyStart = point.y - start.y
            val distStart = sqrt((dxStart * dxStart + dyStart * dyStart).toDouble()).toFloat()

            val dxEnd = point.x - end.x
            val dyEnd = point.y - end.y
            val distEnd = sqrt((dxEnd * dxEnd + dyEnd * dyEnd).toDouble()).toFloat()

            val minDistToEndpoint = minOf(distStart, distEnd)
            if (minDistToEndpoint <= endpointThreshold) {
                if (minDistToEndpoint < minEndpointDist) {
                    minEndpointDist = minDistToEndpoint
                    nearestEndpointIndex = index
                }
            }

            // 2. 计算到线段的距离
            if (isPointOnLineSegment(point, start, end, lineThreshold)) {
                val distLine = pointToLineDistance(point, start, end)
                if (distLine < minLineDist) {
                    minLineDist = distLine
                    nearestLineIndex = index
                }
            }
        }

        // 决定最终命中的是哪个
        var finalSelectedIndex = -1

        if (nearestEndpointIndex != -1) {
            finalSelectedIndex = nearestEndpointIndex
            // 防抖：如果已选中线段的端点也在有效范围内，且与最近端点的距离差小于 15px，保持选中，防止误触切换
            if (selectedLineIndex != -1 && selectedLineIndex != nearestEndpointIndex) {
                if (selectedLineIndex >= 0 && selectedLineIndex < virtualWall.LAYER[0].LINE.size) {
                    val selectedLine = virtualWall.LAYER[0].LINE[selectedLineIndex]
                    val sStart = mapView.worldToScreen(selectedLine.BEGIN.X / PROPORTION, selectedLine.BEGIN.Y / PROPORTION)
                    val sEnd = mapView.worldToScreen(selectedLine.END.X / PROPORTION, selectedLine.END.Y / PROPORTION)
                    
                    val dx1 = point.x - sStart.x
                    val dy1 = point.y - sStart.y
                    val d1 = sqrt((dx1 * dx1 + dy1 * dy1).toDouble()).toFloat()
                    
                    val dx2 = point.x - sEnd.x
                    val dy2 = point.y - sEnd.y
                    val d2 = sqrt((dx2 * dx2 + dy2 * dy2).toDouble()).toFloat()
                    
                    val minSelectedDist = minOf(d1, d2)
                    if (minSelectedDist <= endpointThreshold && Math.abs(minSelectedDist - minEndpointDist) < 15f) {
                        finalSelectedIndex = selectedLineIndex
                    }
                }
            }
        } else if (nearestLineIndex != -1) {
            finalSelectedIndex = nearestLineIndex
            // 防抖：如果已选中线段也在有效范围内，且与最近线段的距离差小于 15px，保持选中
            if (selectedLineIndex != -1 && selectedLineIndex != nearestLineIndex) {
                if (selectedLineIndex >= 0 && selectedLineIndex < virtualWall.LAYER[0].LINE.size) {
                    val selectedLine = virtualWall.LAYER[0].LINE[selectedLineIndex]
                    val sStart = mapView.worldToScreen(selectedLine.BEGIN.X / PROPORTION, selectedLine.BEGIN.Y / PROPORTION)
                    val sEnd = mapView.worldToScreen(selectedLine.END.X / PROPORTION, selectedLine.END.Y / PROPORTION)
                    if (isPointOnLineSegment(point, sStart, sEnd, lineThreshold)) {
                        val selectedDist = pointToLineDistance(point, sStart, sEnd)
                        if (Math.abs(selectedDist - minLineDist) < 15f) {
                            finalSelectedIndex = selectedLineIndex
                        }
                    }
                }
            }
        }

        return finalSelectedIndex
    }

    /**
     * 检测点是否在线段附近
     * @param point 点击点
     * @param start 线段起点
     * @param end 线段终点
     * @param threshold 点击阈值
     * @return 是否点击在线段上
     */
    private fun isPointOnLineSegment(
        point: PointF, start: PointF, end: PointF, threshold: Float
    ): Boolean {
        // 计算点到线段的距离
        val distance = pointToLineDistance(point, start, end)

        // 检查距离是否在阈值内
        if (distance > threshold) {
            return false
        }

        // 检查点是否在线段的延长线上
        val dotProduct =
            (point.x - start.x) * (end.x - start.x) + (point.y - start.y) * (end.y - start.y)
        if (dotProduct < 0) {
            return false
        }

        val segmentLengthSquared =
            (end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)
        if (dotProduct > segmentLengthSquared) {
            return false
        }

        return true
    }

    /**
     * 检查点是否在地图范围内
     */
    private fun isPointInMapRange(x: Float, y: Float): Boolean {
        val mapView = parent.get() ?: return true // 如果获取不到地图视图，默认允许创建
        val mapData = mapView.mSrf.mapData

        // 计算地图边界
        val minX = mapData.originX
        val maxX = mapData.originX + mapData.width * mapData.resolution
        val minY = mapData.originY
        val maxY = mapData.originY + mapData.height * mapData.resolution

        return x in minX..maxX && y >= minY && y <= maxY
    }

    /**
     * 计算虚拟墙长度（毫米）
     */
    private fun calculateWallLength(startWorld: PointF, endWorld: PointF): Float {
        val dx = endWorld.x - startWorld.x
        val dy = endWorld.y - startWorld.y
        val lengthMeters = sqrt((dx * dx + dy * dy).toDouble()).toFloat()
        return lengthMeters * PROPORTION // 转换为毫米
    }

    /**
     * 计算点到线段的距离
     */
    private fun pointToLineDistance(point: PointF, lineStart: PointF, lineEnd: PointF): Float {
        val a = point.x - lineStart.x
        val b = point.y - lineStart.y
        val c = lineEnd.x - lineStart.x
        val d = lineEnd.y - lineStart.y

        val dot = a * c + b * d
        val lenSq = c * c + d * d
        val param = if (lenSq == 0f) -1f else dot / lenSq

        val xx: Float
        val yy: Float

        if (param < 0f) {
            xx = lineStart.x
            yy = lineStart.y
        } else if (param > 1f) {
            xx = lineEnd.x
            yy = lineEnd.y
        } else {
            xx = lineStart.x + param * c
            yy = lineStart.y + param * d
        }

        val dx = point.x - xx
        val dy = point.y - yy
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    /**
     * 查找点击的是线段的哪个端点
     */
    private fun findTouchedPoint(lineIndex: Int, point: PointF): Int {
        val mapView = parent.get() ?: return -1
        if (lineIndex < 0 || lineIndex >= virtualWall.LAYER[0].LINE.size) {
            return -1
        }

        val line = virtualWall.LAYER[0].LINE[lineIndex]
        val start = mapView.worldToScreen(line.BEGIN.X / PROPORTION, line.BEGIN.Y / PROPORTION)
        val end = mapView.worldToScreen(line.END.X / PROPORTION, line.END.Y / PROPORTION)
        
        // 保持与 findNearestLine 一致的端点点击精度，考虑地图缩放级别
        val endpointThreshold = (30f * scale).coerceIn(30f, 80f)

        val dxStart = point.x - start.x
        val dyStart = point.y - start.y
        val distStart = sqrt((dxStart * dxStart + dyStart * dyStart).toDouble()).toFloat()

        val dxEnd = point.x - end.x
        val dyEnd = point.y - end.y
        val distEnd = sqrt((dxEnd * dxEnd + dyEnd * dyEnd).toDouble()).toFloat()

        val lineLen = sqrt(((end.x - start.x) * (end.x - start.x) + (end.y - start.y) * (end.y - start.y)).toDouble()).toFloat()

        if (lineLen > 0) {
            val a = point.x - start.x
            val b = point.y - start.y
            val c = end.x - start.x
            val d = end.y - start.y
            val param = (a * c + b * d) / (c * c + d * d)

            // 如果虚拟墙在屏幕上显示很短，它的两个端点热区会互相覆盖。
            // 此时如果点击了线段的中间区域（三分之一到三分之二），强制判定为拖动整条线段。
            if (lineLen < endpointThreshold * 2.5f) {
                if (param > 0.33f && param < 0.67f) {
                    return 2
                }
            } else {
                // 如果墙比较长，且点击在端点热区外，则是拖动整条线
                if (param > 0f && param < 1f && distStart > endpointThreshold && distEnd > endpointThreshold) {
                    return 2
                }
            }
        }

        if (distStart <= endpointThreshold && distStart <= distEnd) {
            return 0
        } else if (distEnd <= endpointThreshold) {
            return 1
        }

        return 2 // 2表示点击了线段本身
    }

    /**
     * 停止边缘平移地图
     */
    private fun stopMapPanning() {
        isPanning = false
        panRunnable?.let { removeCallbacks(it) }
    }

    /**
     * 开启或更新边缘平移地图
     */
    private fun updateMapPanning(dx: Int, dy: Int, mapView: MapView) {
        panDx = dx
        panDy = dy
        if (!isPanning && (dx != 0 || dy != 0)) {
            isPanning = true
            if (panRunnable == null) {
                panRunnable = object : Runnable {
                    override fun run() {
                        if (!isPanning) return
                        val mv = parent.get() ?: return
                        
                        // 1. 平移地图
                        mv.onMapMove(panDx, panDy)
                        
                        // 2. 根据当前模式更新状态
                        if (currentWorkMode == WorkMode.MODE_VIRTUAL_WALL_ADD && isCreating && startPoint != null) {
                            startPoint?.offset(panDx.toFloat(), panDy.toFloat())
                        } else if (currentWorkMode == WorkMode.MODE_VIRTUAL_WALL_EDIT && isEditing && selectedLineIndex != -1 && touchedPointIndex != -1) {
                            if (virtualWall.LAYER.isNotEmpty() && selectedLineIndex >= 0 && selectedLineIndex < virtualWall.LAYER[0].LINE.size) {
                                val line = virtualWall.LAYER[0].LINE[selectedLineIndex]
                                val newWorldPoint = mv.screenToWorld(lastEventX, lastEventY)
                                val wDx = newWorldPoint.x - lastWorldX
                                val wDy = newWorldPoint.y - lastWorldY
                                lastWorldX = newWorldPoint.x
                                lastWorldY = newWorldPoint.y

                                if (touchedPointIndex == 0) {
                                    val newBeginX = line.BEGIN.X + wDx * PROPORTION
                                    val newBeginY = line.BEGIN.Y + wDy * PROPORTION
                                    if (isPointInMapRange(newBeginX / PROPORTION, newBeginY / PROPORTION)) {
                                        line.BEGIN.X = newBeginX
                                        line.BEGIN.Y = newBeginY
                                    }
                                } else if (touchedPointIndex == 1) {
                                    val newEndX = line.END.X + wDx * PROPORTION
                                    val newEndY = line.END.Y + wDy * PROPORTION
                                    if (isPointInMapRange(newEndX / PROPORTION, newEndY / PROPORTION)) {
                                        line.END.X = newEndX
                                        line.END.Y = newEndY
                                    }
                                } else if (touchedPointIndex == 2) {
                                    val newBeginX = line.BEGIN.X + wDx * PROPORTION
                                    val newBeginY = line.BEGIN.Y + wDy * PROPORTION
                                    val newEndX = line.END.X + wDx * PROPORTION
                                    val newEndY = line.END.Y + wDy * PROPORTION

                                    if (isPointInMapRange(newBeginX / PROPORTION, newBeginY / PROPORTION) &&
                                        isPointInMapRange(newEndX / PROPORTION, newEndY / PROPORTION)
                                    ) {
                                        line.BEGIN.X = newBeginX
                                        line.BEGIN.Y = newBeginY
                                        line.END.X = newEndX
                                        line.END.Y = newEndY
                                    }
                                }
                            }
                        }
                        
                        postInvalidate()
                        postDelayed(this, 16) // ~60fps
                    }
                }
            }
            post(panRunnable)
        } else if (dx == 0 && dy == 0) {
            stopMapPanning()
        }
    }

    /**
     * 处理触摸事件
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val mapView = parent.get() ?: return false
        
        // 处理多点触控（如双指缩放地图）
        if (event.pointerCount > 1) {
            // 当有多指触摸时，取消当前所有的虚拟墙编辑/创建操作
            if (isCreating) {
                isCreating = false
                startPoint = null
                currentPoint = null
                stopMapPanning()
                postInvalidate()
            }
            if (isEditing) {
                isEditing = false
                isDragging = false
                selectedLineIndex = -1
                touchedPointIndex = -1
                stopMapPanning()
                postInvalidate()
            }
            
            // 将事件传递给地图处理手势（如缩放、平移等）
            mapView.processMapGestures(event)
            return true
        }

        val consumed: Boolean

        when (currentWorkMode) {
            WorkMode.MODE_VIRTUAL_WALL_ADD -> {
                consumed = handleAddModeTouch(event, mapView)
            }

            WorkMode.MODE_VIRTUAL_WALL_EDIT -> {
                consumed = handleEditModeTouch(event, mapView)
            }

            WorkMode.MODE_VIRTUAL_WALL_DELETE -> {
                consumed = handleDeleteModeTouch(event, mapView)
            }

            WorkMode.MODE_VIRTUAL_WALL_TYPE_EDIT -> {
                consumed = handleTypeEditModeTouch(event, mapView)
            }

            else -> {
                consumed = false
            }
        }

        // 如果事件没有被消费，传递给父View处理地图拖动
        return consumed || super.onTouchEvent(event)
    }

    /**
     * 处理创建虚拟墙模式的触摸事件
     */
    private fun handleAddModeTouch(event: MotionEvent, mapView: MapView): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 开始创建虚拟墙
                isCreating = true
                // 复用PointF对象
                if (startPoint == null) startPoint = PointF()
                if (currentPoint == null) currentPoint = PointF()
                startPoint!!.set(event.x, event.y)
                currentPoint!!.set(event.x, event.y)
                postInvalidate()
                return true // 消费事件
            }

            MotionEvent.ACTION_MOVE -> {
                if (isCreating && currentPoint != null) {
                    // 更新当前点
                    currentPoint!!.set(event.x, event.y)
                    lastEventX = event.x
                    lastEventY = event.y
                    
                    // 边缘平移地图逻辑
                    val edgeThreshold = 100f
                    val panSpeed = 15
                    var dx = 0
                    var dy = 0

                    if (event.x < edgeThreshold) {
                        dx = panSpeed
                    } else if (event.x > mapView.viewWidth - edgeThreshold) {
                        dx = -panSpeed
                    }

                    if (event.y < edgeThreshold) {
                        dy = panSpeed
                    } else if (event.y > mapView.viewHeight - edgeThreshold) {
                        dy = -panSpeed
                    }

                    updateMapPanning(dx, dy, mapView)
                    
                    postInvalidate()
                    return true // 消费事件
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopMapPanning()
                // 完成创建虚拟墙
                var wallCreated = false
                if (isCreating && startPoint != null && currentPoint != null) {
                    // 转换为世界坐标
                    val startWorld = mapView.screenToWorld(startPoint!!.x, startPoint!!.y)
                    val endWorld = mapView.screenToWorld(currentPoint!!.x, currentPoint!!.y)

                    // 计算虚拟墙长度（毫米）
                    val length = calculateWallLength(startWorld, endWorld)

                    // 检查长度是否大于最小值且起点和终点都在地图范围内
                    if (length >= MIN_WALL_LENGTH && isPointInMapRange(
                            startWorld.x, startWorld.y
                        ) && isPointInMapRange(endWorld.x, endWorld.y)
                    ) {
                        virtualWall.LAYERSUM = 1
                        // 确保至少有一个图层
                        if (virtualWall.LAYER.isEmpty()) {
                            virtualWall.LAYER.add(VirWallLayerNew(ArrayList(), 0, 0))
                        }

                        // 创建新的虚拟墙线段
                        val newLine = VirtualWallLineNew(
                            PointNew(startWorld.x * PROPORTION, startWorld.y * PROPORTION),
                            PointNew(endWorld.x * PROPORTION, endWorld.y * PROPORTION),
                            virtualWall.LAYER[0].LINE.size + 1,
                            selectedConfig
                        )

                        // 添加到虚拟墙列表
                        virtualWall.LAYER[0].LINE.add(newLine)
                        virtualWall.LAYER[0].LINESUM = virtualWall.LAYER[0].LINE.size
                        wallCreated = true
                    }

                    // 重置状态
                    isCreating = false
                    // 不重置对象，只重置标志位，便于复用
                    postInvalidate()
                }
                isCreating = false

                // 如果创建了虚拟墙，消费事件；否则不消费，允许地图拖拽
                return wallCreated // 只有创建了虚拟墙才消费事件
            }
        }
        return false // 不消费事件
    }

    /**
     * 处理编辑虚拟墙模式的触摸事件
     */
    private fun handleEditModeTouch(event: MotionEvent, mapView: MapView): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val touchPoint = PointF(event.x, event.y)
                selectedLineIndex = findNearestLine(touchPoint)
                if (selectedLineIndex != -1) {
                    isEditing = true
                    isDragging = false
                    initialTouchX = event.x
                    initialTouchY = event.y
                    
                    val worldPoint = mapView.screenToWorld(event.x, event.y)
                    lastWorldX = worldPoint.x
                    lastWorldY = worldPoint.y
                    
                    touchedPointIndex = findTouchedPoint(selectedLineIndex, touchPoint)
                    postInvalidate()
                    return true // 消费事件
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isEditing && selectedLineIndex != -1 && touchedPointIndex != -1) {
                    if (!isDragging) {
                        val dx = Math.abs(event.x - initialTouchX)
                        val dy = Math.abs(event.y - initialTouchY)
                        if (dx > touchSlop || dy > touchSlop) {
                            isDragging = true
                            // 消除 touchSlop 带来的初始跳跃
                            val worldPoint = mapView.screenToWorld(event.x, event.y)
                            lastWorldX = worldPoint.x
                            lastWorldY = worldPoint.y
                        }
                    }

                    if (isDragging) {
                        val line = virtualWall.LAYER[0].LINE[selectedLineIndex]
                        val worldPoint = mapView.screenToWorld(event.x, event.y)
                        val dx = worldPoint.x - lastWorldX
                        val dy = worldPoint.y - lastWorldY
                        lastWorldX = worldPoint.x
                        lastWorldY = worldPoint.y
                        
                        lastEventX = event.x
                        lastEventY = event.y

                        // 边缘平移地图逻辑
                        val edgeThreshold = 100f
                        val panSpeed = 15
                        var panDx = 0
                        var panDy = 0

                        if (event.x < edgeThreshold) {
                            panDx = panSpeed
                        } else if (event.x > mapView.viewWidth - edgeThreshold) {
                            panDx = -panSpeed
                        }

                        if (event.y < edgeThreshold) {
                            panDy = panSpeed
                        } else if (event.y > mapView.viewHeight - edgeThreshold) {
                            panDy = -panSpeed
                        }

                        updateMapPanning(panDx, panDy, mapView)

                        // 检查点是否在地图范围内
                        if (touchedPointIndex == 0) {
                            val newBeginX = line.BEGIN.X + dx * PROPORTION
                            val newBeginY = line.BEGIN.Y + dy * PROPORTION
                            if (isPointInMapRange(newBeginX / PROPORTION, newBeginY / PROPORTION)) {
                                line.BEGIN.X = newBeginX
                                line.BEGIN.Y = newBeginY
                            }
                        } else if (touchedPointIndex == 1) {
                            val newEndX = line.END.X + dx * PROPORTION
                            val newEndY = line.END.Y + dy * PROPORTION
                            if (isPointInMapRange(newEndX / PROPORTION, newEndY / PROPORTION)) {
                                line.END.X = newEndX
                                line.END.Y = newEndY
                            }
                        } else if (touchedPointIndex == 2) {
                            val newBeginX = line.BEGIN.X + dx * PROPORTION
                            val newBeginY = line.BEGIN.Y + dy * PROPORTION
                            val newEndX = line.END.X + dx * PROPORTION
                            val newEndY = line.END.Y + dy * PROPORTION

                            if (isPointInMapRange(newBeginX / PROPORTION, newBeginY / PROPORTION) &&
                                isPointInMapRange(newEndX / PROPORTION, newEndY / PROPORTION)
                            ) {
                                line.BEGIN.X = newBeginX
                                line.BEGIN.Y = newBeginY
                                line.END.X = newEndX
                                line.END.Y = newEndY
                            }
                        }
                        postInvalidate()
                    }
                    return true // 消费事件
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                stopMapPanning()
                // 编辑完成后检查长度
                if (selectedLineIndex != -1 && selectedLineIndex < virtualWall.LAYER[0].LINE.size) {
                    val line = virtualWall.LAYER[0].LINE[selectedLineIndex]

                    // 直接使用世界坐标计算长度（毫米）
                    val length = sqrt(
                        (line.END.X - line.BEGIN.X).toDouble()
                            .pow(2.0) + (line.END.Y - line.BEGIN.Y).toDouble().pow(2.0)
                    ).toFloat()

                    // 如果长度小于最小值，删除该虚拟墙
                    if (length < MIN_WALL_LENGTH) {
                        virtualWall.LAYER[0].LINE.removeAt(selectedLineIndex)
                        virtualWall.LAYER[0].LINESUM = virtualWall.LAYER[0].LINE.size
                        // 更新线条编号
                        for ((index, lineItem) in virtualWall.LAYER[0].LINE.withIndex()) {
                            lineItem.LINENUM = index + 1
                        }
                        postInvalidate()
                    }
                }
                isEditing = false
                return false // 不消费事件，允许地图拖拽
            }
        }
        return false // 不消费事件
    }

    /**
     * 处理删除虚拟墙模式的触摸事件
     */
    private fun handleDeleteModeTouch(event: MotionEvent, mapView: MapView): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val touchPoint = PointF(event.x, event.y)
            val lineIndex = findNearestLine(touchPoint)
            if (lineIndex != -1) {
                // 删除选中的虚拟墙
                virtualWall.LAYER[0].LINE.removeAt(lineIndex)
                virtualWall.LAYER[0].LINESUM = virtualWall.LAYER[0].LINE.size
                // 更新线条编号
                for ((index, line) in virtualWall.LAYER[0].LINE.withIndex()) {
                    line.LINENUM = index + 1
                }
                postInvalidate()
                return true // 消费事件
            }
        }
        return false // 不消费事件
    }

    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        
        stopMapPanning()

        // 清理虚拟墙数据
        virtualWall.LAYER.forEach { it.LINE.clear() }
        virtualWall.LAYER.clear()

        // 清理创建虚拟墙相关变量
        isCreating = false
        startPoint = null
        currentPoint = null

        // 清理编辑虚拟墙相关变量
        selectedLineIndex = -1
        isEditing = false
        touchedPointIndex = -1

        // 清理父引用
        parent.clear()
    }

    /**
     * 处理编辑虚拟墙类型模式的触摸事件
     */
    private fun handleTypeEditModeTouch(event: MotionEvent, mapView: MapView): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val touchPoint = PointF(event.x, event.y)
            selectedLineIndex = findNearestLine(touchPoint)
            if (selectedLineIndex != -1) {
                // 触发回调，通知虚拟墙被点击
                val line = virtualWall.LAYER[0].LINE[selectedLineIndex]
                virtualWallClickListener?.onVirtualWallClick(selectedLineIndex, line.CONFIG)
                postInvalidate()
                return true // 消费事件
            }
        }
        return false // 不消费事件
    }

    /**
     * 更新虚拟墙类型
     * @param lineIndex 虚拟墙索引
     * @param newConfig 新的类型配置 (1: 重点虚拟墙, 2: 虚拟门, 3: 普通虚拟墙)
     */
    fun updateVirtualWallType(lineIndex: Int, newConfig: Int) {
        if (virtualWall.LAYER.isNotEmpty() && lineIndex >= 0 && lineIndex < virtualWall.LAYER[0].LINE.size) {
            virtualWall.LAYER[0].LINE[lineIndex].CONFIG = newConfig
            postInvalidate()
        }
    }

    /**
     * 设置是否启用绘制
     */
    fun setDrawingEnabled(enabled: Boolean) {
        this.isDrawingEnabled = enabled
        postInvalidate()
    }
}


