package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import com.siasun.dianshi.bean.ReflectorMapBean
import java.lang.ref.WeakReference

/**
 * 反光板地图 View
 */
@SuppressLint("ViewConstructor")
class ReflectMapView(
    context: Context?, val parent: WeakReference<MapView>
) : SlamWareBaseView<MapView>(context, parent) {

    //反光板相关
    private val reflectorAreaRect = RectF()
    val reflectorAreaStartPoint = PointF()
    val reflectorAreaEndPoint = PointF()

    // 绘图参数常量
    private companion object {
        val radius = 10f

        // 使用伴生对象存储Paint，避免重复创建
        val mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.YELLOW
            isAntiAlias = true
            style = Paint.Style.FILL
        }

        val mEditPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.GREEN
            strokeWidth = 3f
            isAntiAlias = true
        }

        val mCreatingPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.BLUE
            strokeWidth = 3f
            isAntiAlias = true
            // 虚线效果
            pathEffect = android.graphics.DashPathEffect(floatArrayOf(10f, 5f), 0f)
        }
    }

    // 当前工作模式
    private var currentWorkMode: WorkMode = WorkMode.MODE_SHOW_MAP
    private val reflectorMaps = mutableListOf<ReflectorMapBean>() //反光板数据

    // 监听器
    private var onReflectorAreaCreatedListener: OnReflectorAreaCreatedListener? = null

    private enum class DragMode {
        NONE, MOVE, RESIZE
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        currentWorkMode = mode
        if (mode != WorkMode.WORK_MODE_ADD_REFLECTOR_AREA) {
            cleanReflector()
        }
        postInvalidate()
    }

    /***
     * 绘制过门
     */
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mapView = parent.get() ?: return

        if (currentWorkMode == WorkMode.WORK_MODE_ADD_REFLECTOR_AREA) {
            canvas.drawRect(reflectorAreaRect, mCreatingPaint)
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val mapView = parent.get() ?: return false

        if (currentWorkMode == WorkMode.WORK_MODE_ADD_REFLECTOR_AREA) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    reflectorAreaStartPoint.set(event.x, event.y)
                    reflectorAreaEndPoint.set(event.x, event.y)
                    updateReflectorAreaRect()
                    postInvalidate()
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    reflectorAreaEndPoint.set(event.x, event.y)
                    updateReflectorAreaRect()
                    postInvalidate()
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    reflectorAreaEndPoint.set(event.x, event.y)
                    updateReflectorAreaRect()
                    postInvalidate()

                    // 回调
                    onReflectorAreaCreatedListener?.onReflectorAreaCreated(
                        PointF(reflectorAreaRect.left, reflectorAreaRect.top),
                        PointF(reflectorAreaRect.right, reflectorAreaRect.bottom)
                    )
                    return true
                }
            }
        }

        return false
    }

    private fun updateReflectorAreaRect() {
        reflectorAreaRect.left = minOf(reflectorAreaStartPoint.x, reflectorAreaEndPoint.x)
        reflectorAreaRect.top = minOf(reflectorAreaStartPoint.y, reflectorAreaEndPoint.y)
        reflectorAreaRect.right = maxOf(reflectorAreaStartPoint.x, reflectorAreaEndPoint.x)
        reflectorAreaRect.bottom = maxOf(reflectorAreaStartPoint.y, reflectorAreaEndPoint.y)
    }

    /**
     * 反光板区域创建监听器
     */
    interface OnReflectorAreaCreatedListener {
        fun onReflectorAreaCreated(topLeft: PointF, bottomRight: PointF)
    }

    fun setOnReflectorAreaCreatedListener(listener: OnReflectorAreaCreatedListener) {
        this.onReflectorAreaCreatedListener = listener
    }

    /**
     * 外部接口:设置反光板
     */
    fun setReflectorMap(list: MutableList<ReflectorMapBean>) {
        reflectorMaps.clear()
        reflectorMaps.addAll(list)
    }

    fun getData(): MutableList<ReflectorMapBean> {
        return reflectorMaps.toMutableList()
    }

    /**
     * 外部接口:清除反光板数据
     */
    fun cleanReflector() {
        reflectorAreaRect.set(0f, 0f, 0f, 0f)
        reflectorAreaStartPoint.set(0f, 0f)
        reflectorAreaEndPoint.set(0f, 0f)
    }

}


