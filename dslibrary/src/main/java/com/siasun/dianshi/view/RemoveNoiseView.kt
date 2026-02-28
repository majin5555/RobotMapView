package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.view.MotionEvent
import android.view.ViewConfiguration
import java.lang.ref.WeakReference
import kotlin.math.abs

/**
 * 噪点擦除视图
 * 用于在地图上绘制矩形区域以去除噪点
 */
@SuppressLint("ViewConstructor")
class RemoveNoiseView(context: Context?, parent: WeakReference<MapView>) :
    SlamWareBaseView<MapView>(context, parent) {

    // 当前工作模式
    private var currentWorkMode = WorkMode.MODE_SHOW_MAP

    // 触摸点坐标
    private val startPoint = PointF()
    private val endPoint = PointF()

    // 绘制的矩形
    private var rectLeft = 0f
    private var rectTop = 0f
    private var rectRight = 0f
    private var rectBottom = 0f

    // 存储所有绘制的矩形
    private val rectList = ArrayList<RectF>()

    // 是否正在绘制
    private var isDrawing = false
    
    // 最小滑动距离，用于区分点击和拖动
    private val touchSlop: Int

    // 是否是3D模式
    private var is3D = false

    init {
        val configuration = ViewConfiguration.get(context!!)
        touchSlop = configuration.scaledTouchSlop
    }

    // 绘图画笔 - 使用伴生对象创建静态实例，避免重复创建
    companion object {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLUE
            style = Paint.Style.STROKE
            strokeWidth = 2f // 增加线条宽度，提高可见性
        }
    }

    // 监听器 - 使用WeakReference防止内存泄漏
//    private var onRemoveNoiseListener: OnRemoveNoiseListener? = null

    // 复用的PointF对象，减少内存分配
    private val leftTopPoint = PointF()
    private val rightBottomPoint = PointF()

    // 复用的RectF对象
    private val tempRect = RectF()

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        if (currentWorkMode == mode) return // 避免重复设置

        currentWorkMode = mode
        // 如果不是擦除噪点模式，重置绘制状态
        if (mode != WorkMode.MODE_REMOVE_NOISE) {
            resetDrawingState()
        }
    }

    /**
     * 设置是否是3D模式
     */
    fun set3D(is3D: Boolean) {
        this.is3D = is3D
    }

    /**
     * 重置绘制状态
     */
    private fun resetDrawingState() {
        isDrawing = false
        startPoint.set(0f, 0f)
        endPoint.set(0f, 0f)
        resetRect()
        rectList.clear()
        invalidate()
    }

    /**
     * 获取所有绘制的噪点区域
     */
    fun getRects(): List<RectF> {
        return ArrayList(rectList)
    }

    /**
     * 清除已绘制的矩形线框
     */
    fun clearDrawing() {
        resetDrawingState()
    }

    /**
     * 重置矩形坐标
     */
    private fun resetRect() {
        rectLeft = 0f
        rectTop = 0f
        rectRight = 0f
        rectBottom = 0f
    }

//    /**
//     * 擦除噪点监听器接口
//     */
//    interface OnRemoveNoiseListener {
//        /**
//         * 当用户完成噪点擦除操作时调用
//         * @param leftTop 矩形左上角的屏幕坐标
//         * @param rightBottom 矩形右下角的屏幕坐标
//         */
//        fun onRemoveNoise(leftTop: PointF, rightBottom: PointF)
//
//        /**
//         * 当用户删除已绘制的噪点区域时调用
//         * @param rect 被删除的矩形（屏幕坐标）
//         */
//        fun onRemoveNoiseDeleted(rect: RectF)
//    }

    /**
     * 设置擦除噪点监听器
     */
//    fun setOnRemoveNoiseListener(listener: OnRemoveNoiseListener?) {
//        onRemoveNoiseListener = listener
//    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 只有在擦除噪点模式下才响应触摸事件
        if (currentWorkMode != WorkMode.MODE_REMOVE_NOISE) {
            return false
        }

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                handleActionDown(event)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                handleActionMove(event)
                return true
            }

            MotionEvent.ACTION_UP -> {
                handleActionUp(event)
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 处理按下事件
     */
    private fun handleActionDown(event: MotionEvent) {
        // 检查触摸点是否在地图范围内
        val x = event.x.coerceIn(0f, width.toFloat())
        val y = event.y.coerceIn(0f, height.toFloat())

        startPoint.set(x, y)
        endPoint.set(x, y)
        isDrawing = true
    }

    /**
     * 处理移动事件
     */
    private fun handleActionMove(event: MotionEvent) {
        if (!isDrawing) return

        // 检查触摸点是否在地图范围内
        val x = event.x.coerceIn(0f, width.toFloat())
        val y = event.y.coerceIn(0f, height.toFloat())

        // 更新结束点并计算矩形
        endPoint.set(x, y)
        updateRectFromPoints()
        invalidate() // 触发重绘
    }

    /**
     * 处理抬起事件
     */
    private fun handleActionUp(event: MotionEvent) {
        if (!isDrawing) return

        // 检查是否为点击事件（移动距离小于 touchSlop）
        val dx = abs(event.x - startPoint.x)
        val dy = abs(event.y - startPoint.y)
        if (dx < touchSlop && dy < touchSlop) {
            // 是点击事件，检查是否点击了某个矩形
            handleTap(event.x, event.y)
            isDrawing = false
            resetRect()
            invalidate()
            return
        }

        // 检查触摸点是否在地图范围内
        val x = event.x.coerceIn(0f, width.toFloat())
        val y = event.y.coerceIn(0f, height.toFloat())

        // 更新结束点并计算矩形
        endPoint.set(x, y)
        updateRectFromPoints()

        // 将当前矩形添加到列表中
        if (rectLeft != rectRight && rectTop != rectBottom) {
            // 3D模式下只支持绘制1个框
            if (is3D) {
                rectList.clear()
            }
            rectList.add(RectF(rectLeft, rectTop, rectRight, rectBottom))
//             // 触发回调
//            leftTopPoint.set(rectLeft, rectTop)
//            rightBottomPoint.set(rectRight, rectBottom)
//            onRemoveNoiseListener?.onRemoveNoise(leftTopPoint, rightBottomPoint)
        }

        // 重置当前绘制状态，准备下一次绘制
        isDrawing = false
        resetRect()
        invalidate()
    }

    /**
     * 处理点击事件，删除被点击的矩形
     */
    private fun handleTap(x: Float, y: Float) {
        // 倒序遍历，优先删除上层（后绘制）的矩形
        for (i in rectList.indices.reversed()) {
            val rect = rectList[i]
            if (rect.contains(x, y)) {
                rectList.removeAt(i)
//                onRemoveNoiseListener?.onRemoveNoiseDeleted(rect)
                break // 每次点击只删除一个
            }
        }
    }

    /**
     * 根据起始点和结束点更新矩形坐标
     */
    private fun updateRectFromPoints() {
        // 计算矩形坐标
        rectLeft = startPoint.x.coerceAtMost(endPoint.x)
        rectTop = startPoint.y.coerceAtMost(endPoint.y)
        rectRight = startPoint.x.coerceAtLeast(endPoint.x)
        rectBottom = startPoint.y.coerceAtLeast(endPoint.y)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 绘制所有已保存的矩形
        for (rect in rectList) {
            canvas.drawRect(rect, paint)
        }

        // 绘制当前正在拖动的矩形
        if (isDrawing && rectLeft != rectRight && rectTop != rectBottom) {
            tempRect.set(rectLeft, rectTop, rectRight, rectBottom)
            canvas.drawRect(tempRect, paint)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        // 清理资源，防止内存泄漏
//        onRemoveNoiseListener = null

        // 重置状态
        resetDrawingState()
    }
}
