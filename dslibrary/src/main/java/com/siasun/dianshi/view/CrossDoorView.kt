package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import com.siasun.dianshi.bean.CrossDoor
import com.siasun.dianshi.bean.DoorMsg
import java.lang.ref.WeakReference
import kotlin.random.Random

/**
 * 过门View
 */
@SuppressLint("ViewConstructor")
class CrossDoorView(
    context: Context?, val parent: WeakReference<MapView>
) : SlamWareBaseView(context, parent) {

    // 当前工作模式
    private var currentWorkMode: MapView.WorkMode = MapView.WorkMode.MODE_SHOW_MAP

    // 控制是否绘制
    private var isDrawingEnabled = true

    // 当前正在创建的过门
    private var currentCrossDoor: CrossDoor? = null

    // 过门列表
    private val crossDoorList = mutableListOf<CrossDoor>()

    // 编辑状态
    private var isEditing = false
    private var selectedCrossDoor: CrossDoor? = null
    private var selectedPointType = SelectedPointType.NONE
    private var isDragging = false
    private val dragThreshold = 30f // 点击检测阈值（像素）

    // 选中点类型枚举
    enum class SelectedPointType {
        NONE, START_POINT, END_POINT
    }

    // 画笔定义
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0099FF")
        style = Paint.Style.STROKE
        strokeWidth = 4f
        // 设置虚线样式：10px实线，5px间隔
        pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 5f), 0f)
    }

    private val pointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#00CCFF")
        style = Paint.Style.FILL
    }

    private val pointStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        style = Paint.Style.STROKE
        strokeWidth = 2f
    }

    private val selectedPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#FFFF00")
        style = Paint.Style.FILL
    }

    private val doorMsgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        textSize = 14f
        isAntiAlias = true
    }

    /***
     * 绘制过门
     */
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mapView = parent.get() ?: return

        // 绘制所有过门
        for (crossDoor in crossDoorList) {
            // 将世界坐标转换为屏幕坐标
            val startScreenPoint =
                mapView.worldToScreen(crossDoor.start_point.x, crossDoor.start_point.y)
            val endScreenPoint = mapView.worldToScreen(crossDoor.end_point.x, crossDoor.end_point.y)

            // 绘制过门面
            drawLine(canvas, startScreenPoint, endScreenPoint, linePaint)

            // 绘制起点端点
            if (isEditing && selectedCrossDoor == crossDoor && selectedPointType == SelectedPointType.START_POINT) {
                // 选中的起点端点（黄色）
                drawCircle(canvas, startScreenPoint, 10f, selectedPointPaint)
                drawCircle(canvas, startScreenPoint, 10f, pointStrokePaint)
            } else {
                drawCircle(canvas, startScreenPoint, 8f, pointPaint)
                drawCircle(canvas, startScreenPoint, 8f, pointStrokePaint)
            }

            // 绘制终点端点
            if (isEditing && selectedCrossDoor == crossDoor && selectedPointType == SelectedPointType.END_POINT) {
                // 选中的终点端点（黄色）
                drawCircle(canvas, endScreenPoint, 10f, selectedPointPaint)
                drawCircle(canvas, endScreenPoint, 10f, pointStrokePaint)
            } else {
                drawCircle(canvas, endScreenPoint, 8f, pointPaint)
                drawCircle(canvas, endScreenPoint, 8f, pointStrokePaint)
            }

            // 绘制过门信息
            val centerX = (startScreenPoint.x + endScreenPoint.x) / 2f
            val centerY = (startScreenPoint.y + endScreenPoint.y) / 2f
            drawLabel(
                canvas,
                crossDoor.door_msg.door_sn,
                PointF(centerX, centerY - 15f),
                doorMsgPaint
            )
        }

        // 只有在添加过门模式下才绘制当前正在创建的过门
        if (currentWorkMode == MapView.WorkMode.MODE_CROSS_DOOR_ADD && currentCrossDoor != null) {
            val crossDoor = currentCrossDoor!!

            // 将世界坐标转换为屏幕坐标
            val startScreenPoint =
                mapView.worldToScreen(crossDoor.start_point.x, crossDoor.start_point.y)
            val endScreenPoint = mapView.worldToScreen(crossDoor.end_point.x, crossDoor.end_point.y)

            // 绘制过门面（使用不同的颜色表示正在创建）
            val creatingLinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#00FF00")
                style = Paint.Style.STROKE
                strokeWidth = 4f
                alpha = 180
            }
            drawLine(canvas, startScreenPoint, endScreenPoint, creatingLinePaint)

            // 绘制起点端点
            val creatingPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.parseColor("#00FF00")
                style = Paint.Style.FILL
                alpha = 180
            }
            val creatingStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                style = Paint.Style.STROKE
                strokeWidth = 2f
                alpha = 180
            }
            drawCircle(canvas, startScreenPoint, 8f, creatingPointPaint)
            drawCircle(canvas, startScreenPoint, 8f, creatingStrokePaint)

            // 绘制终点端点
            drawCircle(canvas, endScreenPoint, 8f, creatingPointPaint)
            drawCircle(canvas, endScreenPoint, 8f, creatingStrokePaint)
        }
    }

    /**
     * 处理触摸事件
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isEditing) {
            return false
        }

        val mapView = parent.get() ?: return false
        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 检查是否点击了某个过门的端点
                for (crossDoor in crossDoorList) {
                    val startScreenPoint = mapView.worldToScreen(crossDoor.start_point.x, crossDoor.start_point.y)
                    val endScreenPoint = mapView.worldToScreen(crossDoor.end_point.x, crossDoor.end_point.y)

                    // 检查是否点击了起点
                    val dxStart = x - startScreenPoint.x
                    val dyStart = y - startScreenPoint.y
                    if (dxStart * dxStart + dyStart * dyStart <= dragThreshold * dragThreshold) {
                        selectedCrossDoor = crossDoor
                        selectedPointType = SelectedPointType.START_POINT
                        isDragging = true
                        postInvalidate()
                        return true
                    }

                    // 检查是否点击了终点
                    val dxEnd = x - endScreenPoint.x
                    val dyEnd = y - endScreenPoint.y
                    if (dxEnd * dxEnd + dyEnd * dyEnd <= dragThreshold * dragThreshold) {
                        selectedCrossDoor = crossDoor
                        selectedPointType = SelectedPointType.END_POINT
                        isDragging = true
                        postInvalidate()
                        return true
                    }
                }

                // 没有点击到任何端点，取消选中
                selectedCrossDoor = null
                selectedPointType = SelectedPointType.NONE
                postInvalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging && selectedCrossDoor != null) {
                    // 将屏幕坐标转换为世界坐标
                    val worldPoint = mapView.screenToWorld(x, y)

                    // 更新选中端点的坐标
                    when (selectedPointType) {
                        SelectedPointType.START_POINT -> {
                            selectedCrossDoor!!.start_point = PointF(worldPoint.x, worldPoint.y)
                        }
                        SelectedPointType.END_POINT -> {
                            selectedCrossDoor!!.end_point = PointF(worldPoint.x, worldPoint.y)
                        }
                        SelectedPointType.NONE -> {}
                    }

                    postInvalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
            }
        }

        return true
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: MapView.WorkMode) {
        currentWorkMode = mode
        // 当进入编辑过门模式时，启用编辑功能
        isEditing = (mode == MapView.WorkMode.MODE_CROSS_DOOR_EDIT)
        postInvalidate()
    }

    /**
     * 设置是否启用绘制
     */
    fun setDrawingEnabled(enabled: Boolean) {
        isDrawingEnabled = enabled
        postInvalidate()
    }

    /**
     * 在屏幕中心创建一个新的过门
     */
    fun createCrossDoorAtCenter() {
        val mapView = parent.get() ?: return

        // 计算屏幕中心坐标
        val centerX = mapView.viewWidth / 2f
        val centerY = mapView.viewHeight / 2f

        // 将屏幕中心坐标转换为世界坐标
        val worldCenter = mapView.screenToWorld(centerX, centerY)

        // 创建过门的两个端点（水平方向，长度为20单位）
        val startPoint = PointF(worldCenter.x - 10f, worldCenter.y)
        val endPoint = PointF(worldCenter.x + 10f, worldCenter.y)

        // 创建新的过门
        currentCrossDoor = CrossDoor(
            id = Random.nextInt(1000, 9999),
            map_id = 2,
            door_msg = DoorMsg(door_sn = "DOOR_${Random.nextInt(1000, 9999)}", type = "cross_door"),
            start_point = startPoint,
            end_point = endPoint
        )

        // 触发重绘
        postInvalidate()
    }

    /**
     * 清除当前正在创建的过门
     */
    fun clearCurrentCrossDoor() {
        currentCrossDoor = null
        postInvalidate()
    }

    /**
     * 添加过门到列表
     */
    fun addCrossDoor(crossDoor: CrossDoor) {
        val mapView = parent.get() ?: return

        // 计算屏幕中心坐标
        val centerX = mapView.viewWidth / 2f
        val centerY = mapView.viewHeight / 2f

        // 将屏幕中心坐标转换为世界坐标
        val worldCenter = mapView.screenToWorld(centerX, centerY)

        // 创建过门的两个端点（水平方向，长度为20单位）
        val startPoint = PointF(worldCenter.x - 10f, worldCenter.y)
        val endPoint = PointF(worldCenter.x + 10f, worldCenter.y)

        crossDoor.start_point = startPoint
        crossDoor.end_point = endPoint
        crossDoorList.add(crossDoor)
        postInvalidate()
    }

    /**
     * 添加多个过门
     */
    fun addCrossDoors(crossDoors: List<CrossDoor>) {


        crossDoorList.addAll(crossDoors)
        postInvalidate()
    }

    /**
     * 移除过门
     */
    fun removeCrossDoor(crossDoor: CrossDoor) {
        crossDoorList.remove(crossDoor)
        postInvalidate()
    }

    /**
     * 清空过门列表
     */
    fun clearCrossDoors() {
        crossDoorList.clear()
        postInvalidate()
    }

    /**
     * 获取当前正在创建的过门
     */
    fun getCurrentCrossDoor(): CrossDoor? {
        return currentCrossDoor
    }

    /**
     * 设置当前正在创建的过门（用于从外部传入）
     */
    fun setCurrentCrossDoor(crossDoor: CrossDoor) {
        currentCrossDoor = crossDoor
        postInvalidate()
    }
}


