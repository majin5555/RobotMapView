package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.siasun.dianshi.bean.MergedPoseItem
import java.lang.ref.WeakReference

/**
 * 顶视路线视图
 * 用于在地图上绘制机器人的顶视路线
 */
@SuppressLint("ViewConstructor")
class TopViewPathView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView<MapView>(context, parent) {

    // 基础线宽（像素）
    private val BASE_LINE_WIDTH = 1f

    // 最小线宽，避免缩放过小时线宽过细
    private val MIN_LINE_WIDTH = 0.5f

    // 用于减少绘制点数的采样率，每隔2个点绘制一个
    private val SAMPLE_RATE = 2

    // 在绘制时动态计算屏幕坐标，确保路线与地图缩放同步
    private val routePath = Path()

    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    // 顶视路线数据列表 - 使用同步列表确保线程安全
    private val topViewRouteList = ArrayList<MergedPoseItem>()

    // 绘制画笔 - 移至伴生对象，避免重复创建
    companion object {
        private val mPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            color = Color.parseColor("#CC33FF")
            strokeWidth = 1f
        }
    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isDrawingEnabled || topViewRouteList.isEmpty()) {
            return
        }

        // 获取MapView实例，避免重复调用get()
        val mapView = parent.get() ?: return

        // 根据当前缩放比例调整线宽，确保在不同缩放级别下都有良好的视觉效果
        mPaint.strokeWidth = maxOf(MIN_LINE_WIDTH, BASE_LINE_WIDTH * scale)
        routePath.reset()

        var isFirstPoint = true
        // 采样绘制，减少绘制点数以提高性能
        for (i in 0 until topViewRouteList.size step SAMPLE_RATE) {
            val pose = topViewRouteList[i]

            // 绘制时动态转换为屏幕坐标
            val screenPoint = mapView.worldToScreen(pose.x.toFloat(), pose.y.toFloat())
            if (isFirstPoint) {
                routePath.moveTo(screenPoint.x, screenPoint.y)
                isFirstPoint = false
            } else {
                routePath.lineTo(screenPoint.x, screenPoint.y)
            }
        }
        // 绘制路线
        canvas.drawPath(routePath, mPaint)
    }

    /**
     * 设置顶视路线数据源
     * @param data 路线数据列表
     */
    fun setTopViewPath(data: List<MergedPoseItem>) {
        topViewRouteList.clear()
        if (data.isNotEmpty()) {
            // 预分配容量，避免频繁扩容
            topViewRouteList.ensureCapacity(data.size)
            topViewRouteList.addAll(data)
        }
        postInvalidate()
    }

    /**
     * 追加顶视路线数据
     * @param data 要追加的路线数据列表
     */
    fun addTopViewPath(data: List<MergedPoseItem>) {
        if (data.isNotEmpty()) {
            // 预分配容量，避免频繁扩容
            topViewRouteList.ensureCapacity(topViewRouteList.size + data.size)
            topViewRouteList.addAll(data)
        }
        postInvalidate()
    }

    /**
     * 清除顶视路线
     */
    fun clearTopViewPath() {
        topViewRouteList.clear()
        postInvalidate()
    }

    /**
     * 设置路线颜色
     * @param color 颜色值
     */
    fun setRouteColor(color: Int) {
        mPaint.color = color
        postInvalidate()
    }

    /**
     * 设置是否启用绘制
     * @param enabled 是否启用绘制
     */
    fun setDrawingEnabled(enabled: Boolean) {
        this.isDrawingEnabled = enabled
        postInvalidate()
    }

    /**
     * 获取当前是否启用绘制
     * @return 是否启用绘制
     */
    fun isDrawingEnabled(): Boolean {
        return isDrawingEnabled
    }

    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理数据列表
        topViewRouteList.clear()
        // 清理路径
        routePath.reset()
        // 清理parent引用
        parent.clear()
    }
}
