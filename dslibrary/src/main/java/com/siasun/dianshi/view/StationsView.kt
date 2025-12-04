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
    SlamWareBaseView(context, parent) {
    private var mPaint: Paint = Paint()
    private var radius = 10f

    // 避让点
    private val cmsStations = mutableListOf<CmsStation>()

    // 用于点击检测的避让点屏幕坐标映射
    private val stationScreenPositions = mutableMapOf<CmsStation, Pair<Float, Float>>()

    // 当前工作模式
    private var currentWorkMode = MapView.WorkMode.MODE_SHOW_MAP

    // 点击事件回调接口
    interface OnStationClickListener {
        fun onStationClick(station: CmsStation)
    }

    // 点击事件监听器
    private var onStationClickListener: OnStationClickListener? = null

    /**
     * 设置避让点点击监听器
     */
    fun setOnStationClickListener(listener: OnStationClickListener) {
        this.onStationClickListener = listener
    }

    init {
        mPaint.setColor(Color.RED)
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.FILL
        mPaint.strokeWidth = 1f
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 清空之前的屏幕坐标映射
        stationScreenPositions.clear()

        cmsStations.forEach { station ->
            station.coordinate?.let {
                val locate = parent.get()!!.worldToScreen(it.x, it.y)
                // 保存避让点的屏幕坐标，用于点击检测
                stationScreenPositions[station] = Pair(locate.x, locate.y)

                // 根据工作模式调整绘制样式
                if (currentWorkMode == MapView.WorkMode.MODE_CMS_STATION_EDIT) {
                    // 修改避让点模式下，绘制更大的圆圈和更粗的边框，增加视觉提示
                    mPaint.color = Color.GREEN
                    mPaint.style = Paint.Style.STROKE
                    mPaint.strokeWidth = 3f
                    drawCircle(canvas, locate, radius + 5, mPaint)

                    // 恢复填充样式和颜色
                    mPaint.style = Paint.Style.FILL
                    mPaint.color = Color.RED
                    drawCircle(canvas, locate, radius, mPaint)
                } else {
                    // 普通模式下，保持原有的绘制样式
                    mPaint.color = Color.RED
                    mPaint.style = Paint.Style.FILL
                    drawCircle(canvas, locate, radius, mPaint)
                }

                drawLabel(
                    canvas,
                    station.evName ?: context.getString(R.string.cms_station),
                    PointF(
                        locate.x,
                        locate.y
                    ),
                    mPaint
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 只有在修改避让点模式下才响应点击事件
        if (currentWorkMode == MapView.WorkMode.MODE_CMS_STATION_EDIT && event.action == MotionEvent.ACTION_DOWN) {
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
                    onStationClickListener?.onStationClick(station)
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
            cmsStations.addAll(it)
            postInvalidate()
        }
    }
}
