package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import android.view.MotionEvent
import com.siasun.dianshi.bean.CleanAreaNew
import java.lang.ref.WeakReference
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * 清扫区域点点view(暂时是开始点)
 */
@SuppressLint("ViewConstructor")
class PolygonEditViewPoint(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView<MapView>(context, parent) {

    private val mapViewRef: WeakReference<MapView> = parent

    private var isDrawingEnabled: Boolean = false

    private var currentWorkMode = WorkMode.MODE_SHOW_MAP

    companion object {
        private val startPointPaint = Paint().apply {
            style = Paint.Style.FILL
            color = Color.MAGENTA
            isAntiAlias = true
        }
        private const val START_POINT_RADIUS = 10f
        private const val START_POINT_TOUCH_TOLERANCE = 40f
    }

    private var isStartPointDragging = false
    private var lastTouchX = 0f
    private var lastTouchY = 0f

    private var onStartPointEditListener: OnStartPointEditListener? = null


    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        isDrawingEnabled = mode == WorkMode.EDIT_START_POINT
        // 如果退出编辑模式，清空选中状态
        this.currentWorkMode = mode
        invalidate()
    }

    /**
     * 更新开始点
     */
    fun cleanAreaStartPoint() {
        val mapView = mapViewRef.get() ?: return
        mapView.mPolygonEditView?.updateAreaStartPoint(0f, 0f)
    }

    /**
     * 设置开始点编辑监听器
     */
    fun setOnStartPointEditListener(listener: OnStartPointEditListener?) {
        this.onStartPointEditListener = listener
    }

    /**
     * 开始点编辑回调接口
     */
    interface OnStartPointEditListener {
        //fun onStartPointDragStart(area: CleanAreaNew) {}
        //fun onStartPointDragging(area: CleanAreaNew, newX: Float, newY: Float) {}
        fun onStartPointDragEnd(area: CleanAreaNew)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDrawingEnabled) {
            return false
        }

        val x = event.x
        val y = event.y

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val mapView = mapViewRef.get() ?: return false
                mapView.mPolygonEditView?.selectedArea?.let { area ->
                    val screenPoint =
                        mapView.worldToScreen(area.areaStartPoint.x, area.areaStartPoint.y)
                    val startPointX = screenPoint.x
                    val startPointY = screenPoint.y
                    val distance = sqrt(
                        (x - startPointX).toDouble().pow(2.0) + (y - startPointY).toDouble()
                            .pow(2.0)
                    )
                    if (distance <= START_POINT_TOUCH_TOLERANCE) {
                        isStartPointDragging = true
                        lastTouchX = x
                        lastTouchY = y
//                        onStartPointEditListener?.onStartPointDragStart(area)
                        return true
                    }
                }
                return false
            }

            MotionEvent.ACTION_MOVE -> {
                if (isStartPointDragging) {
                    val mapView = mapViewRef.get() ?: return false
                    val worldPoint = mapView.screenToWorld(x, y)
                    if (mapView.mPolygonEditView?.isStartPointInArea(worldPoint.x, worldPoint.y) == true) {
                        mapView.mPolygonEditView?.updateAreaStartPoint(worldPoint.x, worldPoint.y)
                    }
                    lastTouchX = x
                    lastTouchY = y
                    invalidate()
                    return true
                }
                return false
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isStartPointDragging) {
                    val mapView = mapViewRef.get() ?: return false
                    mapView.mPolygonEditView?.selectedArea?.let { area ->
                        mapView.mPolygonEditView?.validateStartPoint()
                        onStartPointEditListener?.onStartPointDragEnd(
                            area
                        )
                    }
                    isStartPointDragging = false
                    return true
                }
                return false
            }
        }
        return false
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isDrawingEnabled) {
            canvas.save()
            val mapView = mapViewRef.get() ?: return
            mapView.mPolygonEditView?.let { selectedView ->
                selectedView.selectedArea?.let { area ->
                    val screenPoint =
                        mapView.worldToScreen(area.areaStartPoint.x, area.areaStartPoint.y)
                    canvas.drawCircle(
                        screenPoint.x, screenPoint.y, START_POINT_RADIUS, startPointPaint
                    )
                }
            }
            canvas.restore()
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
    }
}
