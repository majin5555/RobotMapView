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
import com.siasun.dianshi.bean.RFID
import com.siasun.dianshi.view.StationsView.OnStationClickListener
import com.siasun.dianshi.view.StationsView.OnStationDeleteListener
import java.lang.ref.WeakReference
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator
import kotlin.collections.set
import kotlin.math.sqrt

/******************************************
 * 类描述：
 *
 * @author: why
 * @time: 2026/3/5 11:45
 ******************************************/

@SuppressLint("ViewConstructor")
class RFIDView(context: Context?, var parent: WeakReference<MapView>) :
    SlamWareBaseView<MapView>(context, parent) {

    private val rfIds = ArrayList<RFID>()

    // 用于点击检测的避让点屏幕坐标映射
    private val rfIdScreenPositions = mutableMapOf<RFID, Pair<Float, Float>>()

    // 当前工作模式
    private var currentWorkMode = WorkMode.MODE_SHOW_MAP

    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    private var radius = 10f

    // 点击事件监听器
    private var onRFIdClickListener: OnRFIdClickListener? = null

    private var onRFIdDeleteClickListener: OnRFIdDeleteListener? = null

    // 点击事件回调接口
    interface OnRFIdClickListener {
        fun onRFIdClick(rfId: RFID)
    }

    // 删除事件回调接口
    interface OnRFIdDeleteListener {
        fun onRFIdDelete(rfId: RFID)
    }

    // 可复用的对象，避免重复创建
    private val reusablePointF = PointF()

    companion object {
        private val mPaint = Paint().apply {
            color = Color.GRAY
            isAntiAlias = true
            style = Paint.Style.FILL
            strokeWidth = 1f
        }
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        currentWorkMode = mode
        // 根据工作模式调整绘制和交互行为
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isDrawingEnabled) {
            // 清空之前的屏幕坐标映射
            rfIds.clear()

            // 获取MapView实例，避免重复调用get()
            val mapView = parent.get() ?: return

            rfIds.forEach { rfId ->
                // 使用世界坐标转换为屏幕坐标
                val locate = mapView.worldToScreen(rfId.tagX, rfId.tagY)
                // 保存避让点的屏幕坐标，用于点击检测
                rfIdScreenPositions[rfId] = Pair(rfId.tagX, rfId.tagY)
                when (currentWorkMode) {
                    WorkMode.WORK_MODE_EDIT_RF_ID -> {
                        // 修改避让点模式下，绘制更大的圆圈和更粗的边框，增加视觉提示
                        mPaint.color = Color.GREEN
                        mPaint.style = Paint.Style.STROKE
                        mPaint.strokeWidth = 3f
                        drawCircle(canvas, locate, radius + 5, mPaint)

                        // 恢复填充样式和颜色
                        mPaint.style = Paint.Style.FILL
                        mPaint.color = Color.GRAY
                        drawCircle(canvas, locate, radius, mPaint)
                    }

                    WorkMode.WORK_MODE_DELETE_RF_ID -> {
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
                        mPaint.color = Color.GRAY
                        mPaint.style = Paint.Style.FILL
                        drawCircle(canvas, locate, radius, mPaint)
                    }
                }

                // 复用PointF对象
                reusablePointF.set(locate.x, locate.y)
                drawLabel(
                    canvas,
                    "${context.getString(R.string.rf_id)} : ${rfId.tId}",
                    reusablePointF,
                    mPaint
                )
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 在所有模式下都响应避让点点击事件
        if (event.action == MotionEvent.ACTION_DOWN) {
            val x = event.x
            val y = event.y

            // 检查是否点击到了某个避让点
            for ((rfId, position) in rfIdScreenPositions) {
                val dx = x - position.first
                val dy = y - position.second
                // 计算点击位置与避让点的距离
                val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                // 如果点击在避让点的半径范围内
                if (distance <= radius * 2) { // 扩大点击检测范围，提高用户体验
                    // 根据当前工作模式选择调用不同的监听器
                    when (currentWorkMode) {
                        WorkMode.MODE_CMS_STATION_DELETE -> {
                            // 删除模式下，调用删除监听器
                            onRFIdDeleteClickListener?.onRFIdDelete(rfId)
                        }

                        else -> {
                            // 其他模式下，调用点击监听器
                            onRFIdClickListener?.onRFIdClick(rfId)
                        }
                    }
                    return true
                }
            }
        }
        return super.onTouchEvent(event)

    }

    fun setRFIds(list: MutableList<RFID>?) {
        rfIds.clear()
        list?.let {
            // 预分配容量，避免频繁扩容
            rfIds.ensureCapacity(it.size)
            rfIds.addAll(it)
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
     * 设置rfId点击监听器
     */
    fun setOnRFIdClickListener(listener: OnRFIdClickListener) {
        this.onRFIdClickListener = listener
    }

    fun setOnRFIdDeleteClickListener(listener: OnRFIdDeleteListener) {
        this.onRFIdDeleteClickListener = listener
    }
}
