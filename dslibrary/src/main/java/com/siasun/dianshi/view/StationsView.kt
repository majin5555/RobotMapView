package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import com.siasun.dianshi.R
import com.siasun.dianshi.bean.CmsStation
import java.lang.ref.WeakReference
import kotlin.math.sqrt

/**
 * 站点(避让点)
 */
@SuppressLint("ViewConstructor")
class StationsView(context: Context?, var parent: WeakReference<MapView>) :
    SlamWareBaseView<MapView>(context, parent) {
    private var radius = 10f

    // 避让点 - 使用同步列表确保线程安全
    private val cmsStations = ArrayList<CmsStation>()

    // 用于点击检测的避让点屏幕坐标映射
    private val stationScreenPositions = mutableMapOf<CmsStation, Pair<Float, Float>>()

    // 当前工作模式
    private var currentWorkMode = MapView.WorkMode.MODE_SHOW_MAP

    // 点击事件回调接口
    interface OnStationClickListener {
        fun onStationClick(station: CmsStation)
    }

    // 删除事件回调接口
    interface OnStationDeleteListener {
        fun onStationDelete(station: CmsStation)
    }

    // 点击事件监听器
    private var onStationClickListener: OnStationClickListener? = null
    // 删除事件监听器
    private var onStationDeleteListener: OnStationDeleteListener? = null

    // 可复用的对象，避免重复创建
    private val reusablePointF = PointF()

    // Paint对象移至伴生对象，避免重复创建
    companion object {
        private val mPaint = Paint().apply {
            color = Color.RED
            isAntiAlias = true
            style = Paint.Style.FILL
            strokeWidth = 1f
        }
    }

    /**
     * 设置避让点点击监听器
     */
    fun setOnStationClickListener(listener: OnStationClickListener) {
        this.onStationClickListener = listener
    }

    /**
     * 设置避让点删除监听器
     */
    fun setOnStationDeleteListener(listener: OnStationDeleteListener) {
        this.onStationDeleteListener = listener
    }

    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isDrawingEnabled) {

            // 清空之前的屏幕坐标映射
            stationScreenPositions.clear()

            // 获取MapView实例，避免重复调用get()
            val mapView = parent.get() ?: return

            cmsStations.forEach { station ->
                station.coordinate?.let {
                    // 使用世界坐标转换为屏幕坐标
                    val locate = mapView.worldToScreen(it.x, it.y)
                    // 保存避让点的屏幕坐标，用于点击检测
                    stationScreenPositions[station] = Pair(locate.x, locate.y)

                    // 根据工作模式调整绘制样式
                    when (currentWorkMode) {
                        MapView.WorkMode.MODE_CMS_STATION_EDIT -> {
                            // 修改避让点模式下，绘制更大的圆圈和更粗的边框，增加视觉提示
                            mPaint.color = Color.GREEN
                            mPaint.style = Paint.Style.STROKE
                            mPaint.strokeWidth = 3f
                            drawCircle(canvas, locate, radius + 5, mPaint)

                            // 恢复填充样式和颜色
                            mPaint.style = Paint.Style.FILL
                            mPaint.color = Color.RED
                            drawCircle(canvas, locate, radius, mPaint)
                        }
                        MapView.WorkMode.MODE_CMS_STATION_DELETE -> {
                            // 删除避让点模式下，绘制红色边框和填充，增加删除视觉提示
                            mPaint.color = Color.RED
                            mPaint.style = Paint.Style.STROKE
                            mPaint.strokeWidth = 4f
                            drawCircle(canvas, locate, radius + 5, mPaint)

                            // 填充红色
                            mPaint.style = Paint.Style.FILL
                            drawCircle(canvas, locate, radius, mPaint)
                        }
                        else -> {
                            // 普通模式下，保持原有的绘制样式
                            mPaint.color = Color.RED
                            mPaint.style = Paint.Style.FILL
                            drawCircle(canvas, locate, radius, mPaint)
                        }
                    }

                    // 复用PointF对象
                    reusablePointF.set(locate.x, locate.y)
                    drawLabel(
                        canvas,
                        station.evName ?: context.getString(R.string.cms_station),
                        reusablePointF,
                        mPaint
                    )
                }
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 在所有模式下都响应避让点点击事件
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            // 检查是否点击到了某个避让点
            for ((station, position) in stationScreenPositions) {
                val dx = x - position.first
                val dy = y - position.second
                // 计算点击位置与避让点的距离
                val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                // 如果点击在避让点的半径范围内
                if (distance <= radius * 2) { // 扩大点击检测范围，提高用户体验
                    // 根据当前工作模式选择调用不同的监听器
                    when (currentWorkMode) {
                        MapView.WorkMode.MODE_CMS_STATION_DELETE -> {
                            // 删除模式下，调用删除监听器
                            onStationDeleteListener?.onStationDelete(station)
                        }
                        else -> {
                            // 其他模式下，调用点击监听器
                            onStationClickListener?.onStationClick(station)
                        }
                    }
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: MapView.WorkMode) {
        currentWorkMode = mode
        // 根据工作模式调整绘制和交互行为
        postInvalidate()
    }

    /**
     * 设置避让点数据源
     */
    fun setCmsStations(list: MutableList<CmsStation>?) {
        cmsStations.clear()
        list?.let {
            // 预分配容量，避免频繁扩容
            cmsStations.ensureCapacity(it.size)
            cmsStations.addAll(it)
            postInvalidate()
        }
    }

    /**
     * 设置是否启用绘制
     */
    fun setDrawingEnabled(enabled: Boolean) {
        this.isDrawingEnabled = enabled
        postInvalidate()
    }

    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理监听器引用
        onStationClickListener = null
        // 清理数据列表
        cmsStations.clear()
        // 清理映射表
        stationScreenPositions.clear()
        // 清理parent引用
        parent.clear()
    }
}
