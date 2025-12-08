package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import com.siasun.dianshi.R
import com.siasun.dianshi.bean.End
import com.siasun.dianshi.bean.PositingArea
import com.siasun.dianshi.bean.Start
import java.lang.ref.WeakReference

/**
 * 定位区域视图
 * 支持定位区域的显示、创建、编辑和删除功能
 */
class PostingAreasView(context: Context?, parent: WeakReference<MapView>) :
    SlamWareBaseView(context, parent) {

    // 定位区域列表
    private val positingAreas: MutableList<PositingArea> = mutableListOf()

    // 选中的定位区域ID
    private var selectedAreaId: Long? = null

    // 工作模式状态
    private var isEditMode = false
    private var isDeleteMode = false
    private var isCreateMode = false

    // 创建过程状态
    private var isCreating = false
    private var createStartPoint: Start? = null
    private var createEndPoint: End? = null

    // 拖拽状态
    private var isDragging = false
    private var dragHandle = DragHandle.NONE
    private var selectedArea: PositingArea? = null
    private var startDragPoint = PointF()
    private var originalStartPoint = PointF()
    private var originalEndPoint = PointF()

    // 监听器
    private var onPositingAreaEditedListener: OnPositingAreaEditedListener? = null
    private var onPositingAreaDeletedListener: OnPositingAreaDeletedListener? = null
    private var onPositingAreaCreatedListener: OnPositingAreaCreatedListener? = null

    // 画笔定义（使用lazy初始化，提高性能）
    private val creatingRectPaint by lazy { createCreatingRectPaint() }
    private val rectPaint by lazy { createRectPaint() }
    private val selectedRectPaint by lazy { createSelectedRectPaint() }
    private val handlePaint by lazy { createHandlePaint() }
    private val deleteRectPaint by lazy { createDeleteRectPaint() }
    private val textPaint by lazy { createTextPaint() }

    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    // 拖拽手柄枚举
    private enum class DragHandle {
        NONE, TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT, CENTER
    }

    // 监听器接口定义
    interface OnPositingAreaEditedListener {
        fun onPositingAreaEdited(area: PositingArea)
    }

    interface OnPositingAreaDeletedListener {
        fun onPositingAreaDeleted(area: PositingArea)
    }

    interface OnPositingAreaCreatedListener {
        fun onPositingAreaCreated(area: PositingArea)
    }

    /**
     * 设置定位区域数据
     */
    fun setPositingAreas(areas: MutableList<PositingArea>?) {
        areas?.let {
            positingAreas.clear()
            positingAreas.addAll(it)
            postInvalidate()
        }
    }

    /**
     * 获取定位区域列表
     */
    fun getData(): MutableList<PositingArea> = positingAreas

    /**
     * 设置选中的定位区域
     */
    fun setSelectedArea(area: PositingArea?) {
        selectedAreaId = area?.id
        selectedArea = area
        postInvalidate()
    }

    /**
     * 设置选中的定位区域ID
     */
    fun setSelectedAreaId(areaId: Long?) {
        selectedAreaId = areaId
        selectedArea = areaId?.let { findAreaById(it) }
        postInvalidate()
    }

    /**
     * 清除选中状态
     */
    fun clearSelectedArea() {
        selectedAreaId = null
        selectedArea = null
        postInvalidate()
    }

    /**
     * 设置工作模式
     */
    fun setEditMode(mode: MapView.WorkMode) {
        isEditMode = mode == MapView.WorkMode.MODE_POSITING_AREA_EDIT
        isDeleteMode = mode == MapView.WorkMode.MODE_POSITING_AREA_DELETE
        isCreateMode = mode == MapView.WorkMode.MODE_POSITING_AREA_ADD

        if (!isEditMode && !isDeleteMode && !isCreateMode) {
            // 退出所有操作模式时清除状态
            resetEditState()
            clearSelectedArea()
        }

        if (isCreateMode) {
            // 进入创建模式时清除选择
            clearSelectedArea()
        }

        postInvalidate()
    }

    /**
     * 获取创建模式状态
     */
    fun isCreateMode(): Boolean = isCreateMode

    /**
     * 获取编辑模式状态
     */
    fun isEditMode(): Boolean = isEditMode

    /**
     * 获取删除模式状态
     */
    fun isDeleteMode(): Boolean = isDeleteMode

    /**
     * 删除指定的定位区域
     */
    fun deletePositingArea(area: PositingArea) {
        positingAreas.remove(area)
        if (selectedAreaId == area.id) {
            clearSelectedArea()
        }
        onPositingAreaDeletedListener?.onPositingAreaDeleted(area)
        postInvalidate()
    }

    /**
     * 根据ID删除定位区域
     */
    fun deletePositingAreaById(areaId: Long) {
        findAreaById(areaId)?.let { deletePositingArea(it) }
    }

    /**
     * 设置编辑完成监听器
     */
    fun setOnPositingAreaEditedListener(listener: OnPositingAreaEditedListener?) {
        this.onPositingAreaEditedListener = listener
    }

    /**
     * 设置删除完成监听器
     */
    fun setOnPositingAreaDeletedListener(listener: OnPositingAreaDeletedListener?) {
        this.onPositingAreaDeletedListener = listener
    }

    /**
     * 设置创建完成监听器
     */
    fun setOnPositingAreaCreatedListener(listener: OnPositingAreaCreatedListener?) {
        this.onPositingAreaCreatedListener = listener
    }

    /**
     * 重置编辑状态
     */
    private fun resetEditState() {
        isDragging = false
        dragHandle = DragHandle.NONE
    }

    /**
     * 根据ID查找定位区域
     */
    private fun findAreaById(areaId: Long): PositingArea? = positingAreas.find { it.id == areaId }

    /**
     * 画笔创建方法（分离关注点，提高代码可读性）
     */
    private fun createCreatingRectPaint() = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 3f
        alpha = 180 // 半透明
        isAntiAlias = true
    }

    private fun createRectPaint() = Paint().apply {
        color = Color.BLUE
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    private fun createSelectedRectPaint() = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
    }

    private fun createHandlePaint() = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        strokeWidth = 2f
        isAntiAlias = true
    }

    private fun createDeleteRectPaint() = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        alpha = 128 // 半透明
        strokeWidth = 2f
        isAntiAlias = true
    }

    private fun createTextPaint() = Paint().apply {
        color = Color.BLACK
        textSize = 14f
        isAntiAlias = true
    }

    /**
     * 检查触摸点是否在编辑手柄上
     */
    private fun getDragHandle(touchX: Float, touchY: Float): DragHandle {
        val mapView = mParent.get() ?: return DragHandle.NONE
        val area = selectedArea ?: return DragHandle.NONE

        val (left, top, right, bottom) = getScreenRect(area, mapView)

        // 手柄大小
        val handleSize = 20f

        // 检查各个手柄
        if (isPointInRect(
                touchX, touchY, left - handleSize / 2, top - handleSize / 2, handleSize, handleSize
            )
        ) {
            return DragHandle.TOP_LEFT
        }
        if (isPointInRect(
                touchX, touchY, right - handleSize / 2, top - handleSize / 2, handleSize, handleSize
            )
        ) {
            return DragHandle.TOP_RIGHT
        }
        if (isPointInRect(
                touchX,
                touchY,
                left - handleSize / 2,
                bottom - handleSize / 2,
                handleSize,
                handleSize
            )
        ) {
            return DragHandle.BOTTOM_LEFT
        }
        if (isPointInRect(
                touchX,
                touchY,
                right - handleSize / 2,
                bottom - handleSize / 2,
                handleSize,
                handleSize
            )
        ) {
            return DragHandle.BOTTOM_RIGHT
        }
        if (isPointInRect(touchX, touchY, left, top, right - left, bottom - top)) {
            return DragHandle.CENTER
        }

        return DragHandle.NONE
    }

    /**
     * 检查点是否在矩形内
     */
    private fun isPointInRect(
        x: Float, y: Float, rectLeft: Float, rectTop: Float, rectWidth: Float, rectHeight: Float
    ): Boolean {
        return x >= rectLeft && x <= rectLeft + rectWidth && y >= rectTop && y <= rectTop + rectHeight
    }

    /**
     * 将世界坐标转换为屏幕矩形
     */
    private fun getScreenRect(area: PositingArea, mapView: MapView): RectCoordinates {
        val leftTop = mapView.worldToScreen(area.start.x, area.start.y)
        val rightBottom = mapView.worldToScreen(area.end.x, area.end.y)

        return RectCoordinates(
            left = minOf(leftTop.x, rightBottom.x),
            top = minOf(leftTop.y, rightBottom.y),
            right = maxOf(leftTop.x, rightBottom.x),
            bottom = maxOf(leftTop.y, rightBottom.y)
        )
    }

    /**
     * 屏幕矩形坐标数据类
     */
    private data class RectCoordinates(
        val left: Float, val top: Float, val right: Float, val bottom: Float
    )

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val mapView = mParent.get() ?: return false

        // 处理创建模式
        if (isCreateMode) {
            return handleCreateModeTouch(event, mapView)
        }

        // 处理删除模式
        if (isDeleteMode) {
            return handleDeleteModeTouch(event, mapView)
        }

        // 处理编辑模式
        if (isEditMode && selectedAreaId != null) {
            return handleEditModeTouch(event, mapView)
        }

        return false
    }

    /**
     * 处理创建模式的触摸事件
     */
    private fun handleCreateModeTouch(event: MotionEvent, mapView: MapView): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val worldPoint = mapView.screenToWorld(event.x, event.y)

                // 开始创建新区域
                isCreating = true
                createStartPoint = Start(worldPoint.x, worldPoint.y)
                createEndPoint = End(worldPoint.x, worldPoint.y)
                postInvalidate()
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isCreating && createStartPoint != null) {
                    val worldPoint = mapView.screenToWorld(event.x, event.y)
                    createEndPoint = End(worldPoint.x, worldPoint.y)
                    postInvalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isCreating && createStartPoint != null && createEndPoint != null) {
                    val newArea = PositingArea(
                        id = System.currentTimeMillis(),
                        0,
                        0,
                        0,
                        0,
                        start = createStartPoint!!,
                        end = createEndPoint!!,
                        false
                    )

                    // 添加到列表并通知监听器
                    positingAreas.add(newArea)
                    onPositingAreaCreatedListener?.onPositingAreaCreated(newArea)

                    // 重置创建状态
                    isCreating = false
                    createStartPoint = null
                    createEndPoint = null
                    postInvalidate()
                    return true
                }
            }
        }
        return false
    }

    /**
     * 处理删除模式的触摸事件
     */
    private fun handleDeleteModeTouch(event: MotionEvent, mapView: MapView): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                getClickedArea(event.x, event.y, mapView)?.let {
                    deletePositingArea(it)
                    return true
                }
            }
        }
        return false
    }

    /**
     * 处理编辑模式的触摸事件
     */
    private fun handleEditModeTouch(event: MotionEvent, mapView: MapView): Boolean {
        // 确保selectedArea始终有效
        if (selectedArea == null || selectedArea?.id != selectedAreaId) {
            selectedArea = selectedAreaId?.let { findAreaById(it) }
        }
        val area = selectedArea ?: return false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                dragHandle = getDragHandle(event.x, event.y)
                if (dragHandle != DragHandle.NONE) {
                    isDragging = true
                    selectedArea = area
                    startDragPoint.set(event.x, event.y)
                    originalStartPoint.set(area.start.x, area.start.y)
                    originalEndPoint.set(area.end.x, area.end.y)
                    return true
                }
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging && selectedArea != null) {
                    val worldDelta = calculateWorldDelta(mapView, event.x, event.y)
                    updateAreaPosition(selectedArea!!, worldDelta)
                    postInvalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                if (isDragging && selectedArea != null) {
                    // 通知编辑完成
                    onPositingAreaEditedListener?.onPositingAreaEdited(selectedArea!!)
                }
                resetEditState()
                return true
            }
        }
        return false
    }

    /**
     * 计算屏幕坐标差对应的世界坐标差
     */
    private fun calculateWorldDelta(mapView: MapView, currentX: Float, currentY: Float): PointF {
        val screenPoint1 = PointF(startDragPoint.x, startDragPoint.y)
        val screenPoint2 = PointF(currentX, currentY)
        val worldPoint1 = mapView.screenToWorld(screenPoint1.x, screenPoint1.y)
        val worldPoint2 = mapView.screenToWorld(screenPoint2.x, screenPoint2.y)

        return PointF(
            worldPoint2.x - worldPoint1.x, worldPoint2.y - worldPoint1.y
        )
    }

    /**
     * 根据拖拽手柄和位移更新区域位置
     */
    private fun updateAreaPosition(area: PositingArea, delta: PointF) {
        when (dragHandle) {
            DragHandle.TOP_LEFT -> {
                area.start.x = originalStartPoint.x + delta.x
                area.start.y = originalStartPoint.y + delta.y
            }

            DragHandle.TOP_RIGHT -> {
                area.end.x = originalEndPoint.x + delta.x
                area.start.y = originalStartPoint.y + delta.y
            }

            DragHandle.BOTTOM_LEFT -> {
                area.start.x = originalStartPoint.x + delta.x
                area.end.y = originalEndPoint.y + delta.y
            }

            DragHandle.BOTTOM_RIGHT -> {
                area.end.x = originalEndPoint.x + delta.x
                area.end.y = originalEndPoint.y + delta.y
            }

            DragHandle.CENTER -> {
                area.start.x = originalStartPoint.x + delta.x
                area.start.y = originalStartPoint.y + delta.y
                area.end.x = originalEndPoint.x + delta.x
                area.end.y = originalEndPoint.y + delta.y
            }

            else -> {}
        }
    }

    /**
     * 获取点击位置的定位区域
     */
    private fun getClickedArea(x: Float, y: Float, mapView: MapView): PositingArea? {
        for (area in positingAreas) {
            val (left, top, right, bottom) = getScreenRect(area, mapView)
            if (x in left..right && y >= top && y <= bottom) {
                return area
            }
        }
        return null
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isDrawingEnabled) {
            val mapView = mParent.get() ?: return

            // 绘制创建中的矩形
            if (isCreating && createStartPoint != null && createEndPoint != null) {
                val creatingArea = PositingArea(
                    0, 0, 0, 0, 0, start = createStartPoint!!, end = createEndPoint!!, false
                )
                val (left, top, right, bottom) = getScreenRect(creatingArea, mapView)
                canvas.drawRect(left, top, right, bottom, creatingRectPaint)
            }

            // 绘制所有定位区域
            if (positingAreas.size > 0) {
                positingAreas.forEachIndexed { index, area ->
                    val isSelected = selectedAreaId == area.id
                    drawPositingArea(canvas, area, index, isSelected, mapView)
                }
            }
        }
    }

    /**
     * 绘制单个定位区域
     */
    private fun drawPositingArea(
        canvas: Canvas, area: PositingArea, index: Int, isSelected: Boolean, mapView: MapView
    ) {
        val (left, top, right, bottom) = getScreenRect(area, mapView)
        val paintToUse = if (isSelected) selectedRectPaint else rectPaint

        if (!isSelected) {
            // 根据融合类型设置颜色
            rectPaint.color = when (area.topViewFusion) {
                0 -> Color.RED // 只使用激光定位
                1 -> Color.YELLOW // 只使用顶视定位
                2 -> Color.BLUE // 激光融合顶视定位
                else -> Color.BLACK
            }
        }

        // 删除模式下绘制半透明红色覆盖
        if (isDeleteMode) {
            canvas.drawRect(left, top, right, bottom, deleteRectPaint)
        }

        // 绘制矩形边框
        canvas.drawRect(left, top, right, bottom, paintToUse)

        // 绘制区域名称
        val textX = right + 5f // 矩形右侧偏右5像素
        val textY = bottom - 5f // 矩形底部偏上5像素
        canvas.drawText(
            "${resources.getString(R.string.positing_area)}$index", textX, textY, textPaint
        )

        // 编辑模式下绘制编辑手柄
        if (isSelected && isEditMode) {
            drawEditHandles(canvas, left, top, right, bottom)
        }
    }

    /**
     * 绘制编辑手柄
     */
    private fun drawEditHandles(
        canvas: Canvas, left: Float, top: Float, right: Float, bottom: Float
    ) {
        val handleSize = 20f

        // 绘制四个角的编辑手柄
        canvas.drawRect(
            left - handleSize / 2,
            top - handleSize / 2,
            left + handleSize / 2,
            top + handleSize / 2,
            handlePaint
        )
        canvas.drawRect(
            right - handleSize / 2,
            top - handleSize / 2,
            right + handleSize / 2,
            top + handleSize / 2,
            handlePaint
        )
        canvas.drawRect(
            left - handleSize / 2,
            bottom - handleSize / 2,
            left + handleSize / 2,
            bottom + handleSize / 2,
            handlePaint
        )
        canvas.drawRect(
            right - handleSize / 2,
            bottom - handleSize / 2,
            right + handleSize / 2,
            bottom + handleSize / 2,
            handlePaint
        )
    }

    /**
     * 设置是否启用绘制
     */
    fun setDrawingEnabled(enabled: Boolean) {
        this.isDrawingEnabled = enabled
        postInvalidate()
    }
}
