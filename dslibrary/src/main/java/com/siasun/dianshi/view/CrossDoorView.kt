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
) : SlamWareBaseView<MapView>(context, parent) {

    // 当前工作模式
    private var currentWorkMode: WorkMode = WorkMode.MODE_SHOW_MAP

    // 控制是否绘制
    private var isDrawingEnabled = true

    // 过门列表
    private val crossDoorList = mutableListOf<CrossDoor>()

    // 编辑状态
    private var isEditing = false
    private var isDeleting = false
    private var selectedCrossDoor: CrossDoor? = null
    private var selectedPointType = SelectedPointType.NONE
    private var isDragging = false
    private val dragThreshold = 30f // 点击检测阈值（像素）

    // 防连点机制：记录上次点击的时间戳
    private var lastClickTime: Long = 0
    private val clickInterval: Long = 500 // 点击间隔（毫秒）

    // 选中点类型枚举
    enum class SelectedPointType {
        NONE, START_POINT, END_POINT, LINE
    }

    private var lastTouchX = 0f
    private var lastTouchY = 0f

    // 手势检测器，用于处理双击事件
    private val gestureDetector = android.view.GestureDetector(context, object : android.view.GestureDetector.SimpleOnGestureListener() {
        override fun onDoubleTap(e: MotionEvent): Boolean {
            if (!isEditing && !isDeleting) return false
            val mapView = parent.get() ?: return false
            val x = e.x
            val y = e.y
            
            for (crossDoor in crossDoorList) {
                val startScreenPoint = mapView.worldToScreen(crossDoor.start_point.x, crossDoor.start_point.y)
                val endScreenPoint = mapView.worldToScreen(crossDoor.end_point.x, crossDoor.end_point.y)
                
                val distanceToLine = pointToLineDistance(x, y, startScreenPoint, endScreenPoint)
                if (distanceToLine <= dragThreshold) {
                    if (isDeleting) {
                        onCrossDoorDeleteClickListener?.onCrossDoorDeleteClick(crossDoor)
                    } else {
                        onCrossDoorLineClickListener?.onCrossDoorLineClick(crossDoor)
                    }
                    return true
                }
            }
            return false
        }
    })

    /**
     * 线点击事件监听器接口
     */
    interface OnCrossDoorLineClickListener {
        /**
         * 当点击过门线时调用
         * @param crossDoor 被点击的过门
         */
        fun onCrossDoorLineClick(crossDoor: CrossDoor)
    }

    /**
     * 删除过门监听器接口
     */
    interface OnCrossDoorDeleteClickListener {
        /**
         * 当点击过门线进行删除时调用
         * @param crossDoor 被点击的过门
         */
        fun onCrossDoorDeleteClick(crossDoor: CrossDoor)
    }

    // 线点击事件监听器
    private var onCrossDoorLineClickListener: OnCrossDoorLineClickListener? = null

    // 删除过门监听器
    private var onCrossDoorDeleteClickListener: OnCrossDoorDeleteClickListener? = null

    /**
     * 设置线点击事件监听器
     */
    fun setOnCrossDoorLineClickListener(listener: OnCrossDoorLineClickListener?) {
        onCrossDoorLineClickListener = listener
    }

    /**
     * 设置删除过门监听器
     */
    fun setOnCrossDoorDeleteClickListener(listener: OnCrossDoorDeleteClickListener?) {
        onCrossDoorDeleteClickListener = listener
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

//    private val doorMsgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//        color = Color.parseColor("#0099FF")
//        textSize = 14f
//        isAntiAlias = true
//    }

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

//            // 绘制过门信息
//            val centerX = (startScreenPoint.x + endScreenPoint.x) / 2f
//            val centerY = (startScreenPoint.y + endScreenPoint.y) / 2f
//            drawLabel(
//                canvas,
//                crossDoor.door_msg.type,
//                PointF(centerX, centerY - 15f),
//                doorMsgPaint
//            )
        }
    }

    /**
     * 处理触摸事件
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 编辑模式或删除模式下都可以处理触摸事件
        if (!isEditing && !isDeleting) {
            return false
        }

        if (gestureDetector.onTouchEvent(event)) {
            return true
        }

        val mapView = parent.get() ?: return false
        val x = event.x
        val y = event.y
        var handled = false

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 检查是否点击了某个过门的端点
                for (crossDoor in crossDoorList) {
                    val startScreenPoint =
                        mapView.worldToScreen(crossDoor.start_point.x, crossDoor.start_point.y)
                    val endScreenPoint =
                        mapView.worldToScreen(crossDoor.end_point.x, crossDoor.end_point.y)

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

                    // 检查是否点击了线（使用点到直线的距离公式）
                    val distanceToLine = pointToLineDistance(x, y, startScreenPoint, endScreenPoint)
                    if (distanceToLine <= dragThreshold) {
                        selectedCrossDoor = crossDoor
                        selectedPointType = SelectedPointType.LINE
                        isDragging = true
                        lastTouchX = x
                        lastTouchY = y
                        postInvalidate()
                        return true
                    }
                }

                // 没有点击到任何端点或线，取消选中
                selectedCrossDoor = null
                selectedPointType = SelectedPointType.NONE
                postInvalidate()
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging && selectedCrossDoor != null) {
                    if (selectedPointType == SelectedPointType.LINE) {
                        val lastWorld = mapView.screenToWorld(lastTouchX, lastTouchY)
                        val currWorld = mapView.screenToWorld(x, y)
                        val dx = currWorld.x - lastWorld.x
                        val dy = currWorld.y - lastWorld.y

                        selectedCrossDoor!!.start_point.x += dx
                        selectedCrossDoor!!.start_point.y += dy
                        selectedCrossDoor!!.end_point.x += dx
                        selectedCrossDoor!!.end_point.y += dy

                        lastTouchX = x
                        lastTouchY = y
                    } else {
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

                            else -> {}
                        }
                    }

                    postInvalidate()
                    handled = true
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    handled = true
                }
            }
        }

        return handled
    }

    /**
     * 计算点到直线的距离
     * @param x 点的x坐标
     * @param y 点的y坐标
     * @param startPoint 直线起点
     * @param endPoint 直线终点
     * @return 点到直线的距离
     */
    private fun pointToLineDistance(
        x: Float,
        y: Float,
        startPoint: PointF,
        endPoint: PointF
    ): Float {
        val dx = endPoint.x - startPoint.x
        val dy = endPoint.y - startPoint.y
        val l2 = dx * dx + dy * dy
        if (l2 == 0f) {
            val ddx = x - startPoint.x
            val ddy = y - startPoint.y
            return Math.sqrt((ddx * ddx + ddy * ddy).toDouble()).toFloat()
        }
        
        // 投影点在线段上的比例 t
        var t = ((x - startPoint.x) * dx + (y - startPoint.y) * dy) / l2
        t = Math.max(0f, Math.min(1f, t))
        
        // 投影点坐标
        val projX = startPoint.x + t * dx
        val projY = startPoint.y + t * dy
        
        val ddx = x - projX
        val ddy = y - projY
        return Math.sqrt((ddx * ddx + ddy * ddy).toDouble()).toFloat()
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        currentWorkMode = mode
        // 当进入编辑过门模式时，启用编辑功能
        isEditing = (mode == WorkMode.MODE_CROSS_DOOR_EDIT)
        // 当进入删除过门模式时，启用删除功能
        isDeleting = (mode == WorkMode.MODE_CROSS_DOOR_DELETE)
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
        setWorkMode(WorkMode.MODE_CROSS_DOOR_EDIT)

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
     * 获取过门
     */
    fun getCrossDoors(): MutableList<CrossDoor> {
        return crossDoorList;
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
}


