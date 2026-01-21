package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import com.siasun.dianshi.bean.CrossDoor
import java.lang.ref.WeakReference

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

    // 画笔定义
    private val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#0099FF")
        style = Paint.Style.STROKE
        strokeWidth = 4f
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

    /***
     * 绘制过门
     */
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mapView = parent.get() ?: return

        // 只有在添加过门模式下才绘制当前正在创建的过门
        if (currentWorkMode == MapView.WorkMode.MODE_CROSS_DOOR_ADD && currentCrossDoor != null) {
            val crossDoor = currentCrossDoor!!

            // 将世界坐标转换为屏幕坐标
            val startScreenPoint =
                mapView.worldToScreen(crossDoor.start_point.x, crossDoor.start_point.y)
            val endScreenPoint = mapView.worldToScreen(crossDoor.end_point.x, crossDoor.end_point.y)

            // 绘制过门面
            drawLine(canvas, startScreenPoint, endScreenPoint, linePaint)

            // 绘制起点端点
            drawCircle(canvas, startScreenPoint, 8f, pointPaint)
            drawCircle(canvas, startScreenPoint, 8f, pointStrokePaint)

            // 绘制终点端点
            drawCircle(canvas, endScreenPoint, 8f, pointPaint)
            drawCircle(canvas, endScreenPoint, 8f, pointStrokePaint)
        }
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: MapView.WorkMode) {
        currentWorkMode = mode
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
        currentCrossDoor = CrossDoor(startPoint, endPoint)

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
}


