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
class VirtualLineView(
    context: Context?, parent: WeakReference<MapView>
) : SlamWareBaseView(context, parent) {
    private val LINE_WIDTH = 2f
    private var radius = 5f

    private val PROPORTION = 1000//虚拟墙文件上的是毫米 在本地显示要除1000
    private val MIN_WALL_LENGTH = 100f // 最小虚拟墙长度（毫米），防止添加过短的虚拟墙

    private val mPaint: Paint = Paint()
    private var mSelectedPaint: Paint = Paint()

    //虚拟墙
    private var virtualWall: VirtualWallNew = VirtualWallNew(1, mutableListOf<VirWallLayerNew>())

    fun getVirtualWall(): VirtualWallNew {
        return virtualWall
    }

    // 保存parent引用以便安全访问
    private var mapViewRef: WeakReference<MapView>? = parent

    // 当前工作模式
    private var currentWorkMode: MapView.WorkMode = MapView.WorkMode.MODE_SHOW_MAP

    // 创建虚拟墙相关变量
    private var isCreating = false
    private var startPoint: PointF? = null
    private var currentPoint: PointF? = null
    private var selectedConfig = 3 // 默认创建普通虚拟墙

    // 编辑虚拟墙相关变量
    private var selectedLineIndex = -1
    private var isEditing = false
    private var touchedPointIndex = -1 // 0-起点，1-终点

    // 用于绘制虚线的路径效果
    private var dashPathEffect: DashPathEffect? = null

    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    init {
        mPaint.isAntiAlias = true
        mPaint.color = Color.BLUE
        mPaint.strokeWidth = 1f
        mPaint.style = Paint.Style.STROKE

        // 初始化选中状态画笔
        mSelectedPaint.isAntiAlias = true
        mSelectedPaint.style = Paint.Style.STROKE
        mSelectedPaint.color = Color.GREEN
        mSelectedPaint.strokeWidth = 4f

        // 初始化虚线效果
        dashPathEffect = DashPathEffect(floatArrayOf(10f, 5f), 0f)
    }


    /***
     * 1重点虚拟墙
     * 2虚拟门
     * 3普通虚拟墙
     */
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isDrawingEnabled) {
            // 安全获取MapView引用
            val mapView = mapViewRef?.get() ?: return

            if (virtualWall.LAYER.size > 0) virtualWall.LAYER[0].let {
                mPaint.strokeWidth = LINE_WIDTH
                mSelectedPaint.strokeWidth = 4f
                val scaledRadius = radius

                for ((index, line) in it.LINE.withIndex()) {
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
                    //起点
                    val start = mapView.worldToScreen(
                        line.BEGIN.X / PROPORTION, line.BEGIN.Y / PROPORTION
                    )
                    //终点
                    val end = mapView.worldToScreen(
                        line.END.X / PROPORTION, line.END.Y / PROPORTION
                    )

                    // 如果是选中的线，使用选中画笔
                    if (currentWorkMode == MapView.WorkMode.MODE_VIRTUAL_WALL_EDIT && index == selectedLineIndex) {
                        canvas.drawLine(start.x, start.y, end.x, end.y, mSelectedPaint)

                        // 绘制绿色实心端点圆
                        val originalSelectedStyle = mSelectedPaint.style
                        mSelectedPaint.style = Paint.Style.FILL
                        canvas.drawCircle(start.x, start.y, scaledRadius, mSelectedPaint)
                        canvas.drawCircle(end.x, end.y, scaledRadius, mSelectedPaint)
                        mSelectedPaint.style = originalSelectedStyle
                    } else {
                        canvas.drawLine(start.x, start.y, end.x, end.y, mPaint)

                        // 绘制实心端点圆
                        val originalStyle = mPaint.style
                        mPaint.style = Paint.Style.FILL
                        canvas.drawCircle(start.x, start.y, scaledRadius, mPaint)
                        canvas.drawCircle(end.x, end.y, scaledRadius, mPaint)
                        mPaint.style = originalStyle
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
                    startPoint!!.x,
                    startPoint!!.y,
                    currentPoint!!.x,
                    currentPoint!!.y,
                    mPaint
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
            this.virtualWall.LAYER.add(VirWallLayerNew(mutableListOf(), 0, 0))
        }
        postInvalidate()
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: MapView.WorkMode) {
        currentWorkMode = mode
        // 重置状态
        if (mode != MapView.WorkMode.MODE_VIRTUAL_WALL_ADD) {
            isCreating = false
            startPoint = null
            currentPoint = null
        }
        if (mode != MapView.WorkMode.MODE_VIRTUAL_WALL_EDIT) {
            selectedLineIndex = -1
            isEditing = false
            touchedPointIndex = -1
        }
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
        val mapView = mapViewRef?.get() ?: return -1
        val scaledRadius = radius * scale * 2
        val lineClickThreshold = LINE_WIDTH * 5 * scale // 线段点击阈值，设置为线宽的5倍乘以缩放比例

        if (virtualWall.LAYER.isEmpty() || virtualWall.LAYER[0].LINE.isEmpty()) {
            return -1
        }

        for ((index, line) in virtualWall.LAYER[0].LINE.withIndex()) {
            val start = mapView.worldToScreen(line.BEGIN.X / PROPORTION, line.BEGIN.Y / PROPORTION)
            val end = mapView.worldToScreen(line.END.X / PROPORTION, line.END.Y / PROPORTION)

            // 检查是否点击了起点或终点
            if (isPointInCircle(point, start, scaledRadius) || isPointInCircle(
                    point,
                    end,
                    scaledRadius
                )
            ) {
                return index
            }

            // 检查是否点击了线段
            if (isPointOnLineSegment(point, start, end, lineClickThreshold)) {
                return index
            }
        }

        return -1
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
        point: PointF,
        start: PointF,
        end: PointF,
        threshold: Float
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
        val mapView = mapViewRef?.get() ?: return true // 如果获取不到地图视图，默认允许创建
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
        val lengthMeters = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
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
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    /**
     * 查找点击的是线段的哪个端点
     */
    private fun findTouchedPoint(lineIndex: Int, point: PointF): Int {
        val mapView = mapViewRef?.get() ?: return -1
        if (lineIndex < 0 || lineIndex >= virtualWall.LAYER[0].LINE.size) {
            return -1
        }

        val line = virtualWall.LAYER[0].LINE[lineIndex]
        val start = mapView.worldToScreen(line.BEGIN.X / PROPORTION, line.BEGIN.Y / PROPORTION)
        val end = mapView.worldToScreen(line.END.X / PROPORTION, line.END.Y / PROPORTION)
        val scaledRadius = radius * scale * 2

        if (isPointInCircle(point, start, scaledRadius)) {
            return 0
        } else if (isPointInCircle(point, end, scaledRadius)) {
            return 1
        }

        return -1
    }

    /**
     * 处理触摸事件
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val mapView = mapViewRef?.get() ?: return false
        var consumed = false

        when (currentWorkMode) {
            MapView.WorkMode.MODE_VIRTUAL_WALL_ADD -> {
                consumed = handleAddModeTouch(event, mapView)
            }

            MapView.WorkMode.MODE_VIRTUAL_WALL_EDIT -> {
                consumed = handleEditModeTouch(event, mapView)
            }

            MapView.WorkMode.MODE_VIRTUAL_WALL_DELETE -> {
                consumed = handleDeleteModeTouch(event, mapView)
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
                startPoint = PointF(event.x, event.y)
                currentPoint = PointF(event.x, event.y)
                postInvalidate()
                return true // 消费事件
            }

            MotionEvent.ACTION_MOVE -> {
                if (isCreating) {
                    // 更新当前点
                    currentPoint = PointF(event.x, event.y)
                    postInvalidate()
                    return true // 消费事件
                }
            }

            MotionEvent.ACTION_UP -> {
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
                            startWorld.x,
                            startWorld.y
                        ) && isPointInMapRange(endWorld.x, endWorld.y)
                    ) {
                        // 确保至少有一个图层
                        if (virtualWall.LAYER.isEmpty()) {
                            virtualWall.LAYER.add(VirWallLayerNew(mutableListOf(), 0, 0))
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
                    startPoint = null
                    currentPoint = null
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
                    touchedPointIndex = findTouchedPoint(selectedLineIndex, touchPoint)
                    postInvalidate()
                    return true // 消费事件
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isEditing && selectedLineIndex != -1 && touchedPointIndex != -1) {
                    val line = virtualWall.LAYER[0].LINE[selectedLineIndex]
                    val worldPoint = mapView.screenToWorld(event.x, event.y)

                    // 检查点是否在地图范围内
                    if (isPointInMapRange(worldPoint.x, worldPoint.y)) {
                        if (touchedPointIndex == 0) {
                            line.BEGIN =
                                PointNew(worldPoint.x * PROPORTION, worldPoint.y * PROPORTION)
                        } else {
                            line.END =
                                PointNew(worldPoint.x * PROPORTION, worldPoint.y * PROPORTION)
                        }

                        postInvalidate()
                        return true // 消费事件
                    }
                }
            }

            MotionEvent.ACTION_UP -> {
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
    private fun cleanup() {
        mapViewRef?.clear()
        mapViewRef = null
        dashPathEffect = null
    }

    /**
     * View被移除时调用，清理资源
     */
    override fun onDetachedFromWindow() {
        cleanup()
        super.onDetachedFromWindow()
    }

    /**
     * 设置是否启用绘制
     */
    fun setDrawingEnabled(enabled: Boolean) {
        this.isDrawingEnabled = enabled
        postInvalidate()
    }
}


