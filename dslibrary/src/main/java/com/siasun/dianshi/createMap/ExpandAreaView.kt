package com.siasun.dianshi.createMap

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.siasun.dianshi.bean.End
import com.siasun.dianshi.bean.ExpandArea
import com.siasun.dianshi.bean.Start
import com.siasun.dianshi.createMap.map2D.CreateMapView2D
import com.siasun.dianshi.view.SlamWareBaseView
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

/**
 * 扩展地图的View
 * 支持拖拽生成矩形功能
 */
@SuppressLint("ViewConstructor")
class ExpandAreaView(context: Context?, parent: WeakReference<CreateMapView2D>) :
    SlamWareBaseView<CreateMapView2D>(context, parent) {
    private var currentWorkMode = CreateMapView2D.WorkMode.MODE_SHOW_MAP


    // 创建过程状态
    private var isCreating = false
    private var createStartPoint: Start? = null
    private var createEndPoint: End? = null


    // 画笔定义（使用伴生对象创建静态实例，避免重复创建）
    companion object {
        private val creatingRectPaint = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.FILL
            strokeWidth = 3f
            alpha = 180 // 半透明
            isAntiAlias = true
        }
    }


    // 复用的RectF对象
    private val tempRect = RectF()

    // 区域创建完成监听器
    private var onExpandAreaCreatedListener: OnExpandAreaCreatedListener? = null

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: CreateMapView2D.WorkMode) {
        if (currentWorkMode == mode) return // 避免重复设置

        currentWorkMode = mode

    }

    /**
     * 重置创建状态
     */
    fun resetCreateState() {
        isCreating = false
        createStartPoint = null
        createEndPoint = null
        postInvalidate()
    }

    /**
     * 设置区域创建完成监听器
     */
    fun setOnExpandAreaCreatedListener(listener: OnExpandAreaCreatedListener) {
        this.onExpandAreaCreatedListener = listener
    }

    /**
     * 区域创建完成监听器接口
     */
    interface OnExpandAreaCreatedListener {
        fun onExpandAreaCreated(area: ExpandArea)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 只在扩展地图增加区域模式下响应触摸事件
        if (currentWorkMode != CreateMapView2D.WorkMode.MODE_EXTEND_MAP_ADD_REGION) {
            return false
        }

        val mapView = mParent.get() ?: return false
        return handleCreateModeTouch(event, mapView)
    }

    /**
     * 处理创建模式的触摸事件
     */
    private fun handleCreateModeTouch(event: MotionEvent, mapView: CreateMapView2D): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 只有在 MODE_EXTEND_MAP_ADD_REGION 模式下，并且当前没有正在创建的区域时，才能开始新的绘制
                if (currentWorkMode == CreateMapView2D.WorkMode.MODE_EXTEND_MAP_ADD_REGION && !isCreating) {
                    val worldPoint = mapView.screenToWorld(event.x, event.y)

                    // 开始创建新区域
                    isCreating = true
                    createStartPoint = Start(worldPoint.x, worldPoint.y)
                    createEndPoint = End(worldPoint.x, worldPoint.y)
                    postInvalidate()
                    return true
                }
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
                    val newArea = ExpandArea(
                        start = createStartPoint!!,
                        end = createEndPoint!!,
                    )

                    onExpandAreaCreatedListener?.onExpandAreaCreated(newArea)
                    // 移除 resetCreateState() 调用，需要手动清除才能再次绘制
                    return true
                }
            }
        }
        return false
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val mapView = mParent.get() ?: return

        canvas.save()


        // 绘制正在创建的区域
        if (createStartPoint != null && createEndPoint != null) {
            val leftTop = mapView.worldToScreen(createStartPoint!!.x, createStartPoint!!.y)
            val rightBottom = mapView.worldToScreen(createEndPoint!!.x, createEndPoint!!.y)

            tempRect.set(
                min(leftTop.x, rightBottom.x),
                min(leftTop.y, rightBottom.y),
                max(leftTop.x, rightBottom.x),
                max(leftTop.y, rightBottom.y)
            )
            canvas.drawRect(tempRect, creatingRectPaint)
        }
        
        canvas.restore()
    }

}