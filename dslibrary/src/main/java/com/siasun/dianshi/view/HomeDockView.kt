package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import androidx.core.content.ContextCompat
import com.siasun.dianshi.R
import com.siasun.dianshi.bean.MachineStation
import com.siasun.dianshi.view.WorkMode
import java.lang.ref.WeakReference

/**
 * 充电桩（一体机）位姿信息视图
 */
@SuppressLint("ViewConstructor")
class HomeDockView(context: Context?, parent: WeakReference<MapView>) :
    SlamWareBaseView<MapView>(context, parent) {

    /**
     * 充电站点击事件监听器接口
     */
    interface OnMachineStationClickListener {
        /**
         * 当点击充电站时调用
         * @param station 被点击的充电站
         * @param type 点击的类型：0-对接点，1-准备点，2-等待点，3-结束停放点
         */
        fun onMachineStationClick(station: MachineStation, type: Int)
    }

    /**
     * 充电站删除事件监听器接口
     */
    interface OnMachineStationDeleteListener {
        /**
         * 当删除充电站时调用
         * @param station 被删除的充电站
         * @param type 删除的类型：0-对接点，1-准备点，2-等待点，3-结束停放点
         */
        fun onMachineStationDelete(station: MachineStation, type: Int)
    }

    // 点击事件监听器
    private var onMachineStationClickListener: OnMachineStationClickListener? = null
    // 删除事件监听器
    private var onMachineStationDeleteListener: OnMachineStationDeleteListener? = null

    // 绘图参数常量
    private companion object {
        const val BASE_RADIUS = 10f
        const val BASE_TEXT_SIZE = 10f
        const val LABEL_OFFSET = 15f  // 标签与图标的偏移量
        const val RING_RADIUS = BASE_RADIUS + 5f // 环的半径
        const val RING_STROKE_WIDTH = 2f // 环的线宽

        // 使用伴生对象存储Paint，避免重复创建
        val mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeWidth = 1f
            style = Paint.Style.FILL
            textSize = BASE_TEXT_SIZE
        }
    }

    // 当前工作模式
    private var currentWorkMode: WorkMode = WorkMode.MODE_SHOW_MAP

    // 复用的PointF对象，减少内存分配
    private val screenPoint = PointF()
    private val labelPoint = PointF()

    // 预加载的字符串资源
    private val machineLocatePointText by lazy { resources.getString(R.string.machine_locate_point) }
    private val machineGatePointText by lazy { resources.getString(R.string.machine_gate_point) }
    private val machineWaitPointText by lazy { resources.getString(R.string.machine_wait_point) }
    private val machineFinishPointText by lazy { resources.getString(R.string.machine_finish_point) }

    // 一体机数据
    private var machineStation: MachineStation? = null

    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    /**
     * 设置一体机数据
     * @param station 一体机数据对象
     */
    fun setHomePose(station: MachineStation?) {
        machineStation = station
        postInvalidate()
    }

    fun getData(): MachineStation? = machineStation

    /**
     * 清除一体机数据
     */
    fun clearHomePose() {
        machineStation = null
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isDrawingEnabled) return

        // 获取MapView实例
        val mapView = mParent.get() ?: return
        val station = machineStation ?: return

        // 绘制一体机对接点
        drawLocatePoint(canvas, mapView, station)

        // 绘制对接一体机准备点
        drawGatePoint(canvas, mapView, station)

        // 绘制等待点
        drawWaitPoint(canvas, mapView, station)

        // 绘制充电结束停放点
        drawFinishPoint(canvas, mapView, station)
    }

    /**
     * 绘制一体机对接点
     */
    private fun drawLocatePoint(canvas: Canvas, mapView: MapView, station: MachineStation) {
        station.locate?.let { locate ->
            // 获取世界坐标转屏幕坐标的结果，并复用PointF对象
            val tempPoint = mapView.worldToScreen(locate.x, locate.y)
            screenPoint.set(tempPoint.x, tempPoint.y)
            labelPoint.set(screenPoint.x + LABEL_OFFSET, screenPoint.y - LABEL_OFFSET)

            mPaint.color = ContextCompat.getColor(context, R.color.color_175E7A)
            drawCircle(canvas, screenPoint, BASE_RADIUS, mPaint)
            drawModeRing(canvas, screenPoint)
            mPaint.color=Color.BLACK
            drawLabel(
                canvas,
                machineLocatePointText,
                labelPoint,
                mPaint
            )
        }
    }

    /**
     * 绘制对接一体机准备点
     */
    private fun drawGatePoint(canvas: Canvas, mapView: MapView, station: MachineStation) {
        station.gate?.let { gate ->
            // 获取世界坐标转屏幕坐标的结果，并复用PointF对象
            val tempPoint = mapView.worldToScreen(gate.x, gate.y)
            screenPoint.set(tempPoint.x, tempPoint.y)
            labelPoint.set(screenPoint.x + LABEL_OFFSET, screenPoint.y - LABEL_OFFSET)

            mPaint.color = ContextCompat.getColor(context, R.color.color_54A8BA)
            drawRect(canvas, screenPoint, mPaint)
            drawModeRing(canvas, screenPoint)
            mPaint.color=Color.BLACK
            drawLabel(
                canvas,
                machineGatePointText,
                labelPoint,
                mPaint
            )
        }
    }

    /**
     * 绘制等待点
     */
    private fun drawWaitPoint(canvas: Canvas, mapView: MapView, station: MachineStation) {
        station.wait?.coordinate?.let { coordinate ->
            // 获取世界坐标转屏幕坐标的结果，并复用PointF对象
            val tempPoint = mapView.worldToScreen(coordinate.x, coordinate.y)
            screenPoint.set(tempPoint.x, tempPoint.y)
            labelPoint.set(screenPoint.x + LABEL_OFFSET, screenPoint.y - LABEL_OFFSET)

            mPaint.color = ContextCompat.getColor(context, R.color.color_C85024)
            drawTriangle(canvas, screenPoint, mPaint)
            drawModeRing(canvas, screenPoint)
            mPaint.color=Color.BLACK
            drawLabel(
                canvas,
                machineWaitPointText,
                labelPoint,
                mPaint
            )
        }
    }

    /**
     * 绘制充电结束停放点
     */
    private fun drawFinishPoint(canvas: Canvas, mapView: MapView, station: MachineStation) {
        station.finish?.let { finish ->
            // 获取世界坐标转屏幕坐标的结果，并复用PointF对象
            val tempPoint = mapView.worldToScreen(finish.x, finish.y)
            screenPoint.set(tempPoint.x, tempPoint.y)
            labelPoint.set(screenPoint.x + LABEL_OFFSET, screenPoint.y - LABEL_OFFSET)

            mPaint.color = ContextCompat.getColor(context, R.color.color_FFD44F)
            drawDiamond(canvas, screenPoint, mPaint)
            drawModeRing(canvas, screenPoint)
            mPaint.color=Color.BLACK
            drawLabel(
                canvas,
                machineFinishPointText,
                labelPoint,
                mPaint
            )
        }
    }

    /**
     * 根据当前工作模式绘制不同颜色的环
     */
    private fun drawModeRing(canvas: Canvas, point: PointF) {
        when (currentWorkMode) {
            WorkMode.MODE_MACHINE_STATION_EDIT -> {
                // 编辑模式绘制绿色环
                mPaint.style = Paint.Style.STROKE
                mPaint.strokeWidth = RING_STROKE_WIDTH
                mPaint.color = Color.GREEN// 绿色
                canvas.drawCircle(point.x, point.y, RING_RADIUS, mPaint)
                // 恢复填充样式
                mPaint.style = Paint.Style.FILL
            }

            WorkMode.MODE_MACHINE_STATION_DELETE -> {
                // 删除模式绘制红色环
                mPaint.style = Paint.Style.STROKE
                mPaint.strokeWidth = RING_STROKE_WIDTH
                mPaint.color = Color.RED// 红色
                canvas.drawCircle(point.x, point.y, RING_RADIUS, mPaint)
                // 恢复填充样式
                mPaint.style = Paint.Style.FILL
            }

            else -> {
                // 其他模式不绘制环
            }
        }
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        currentWorkMode = mode
        postInvalidate()
    }

    /**
     * 设置充电站点击事件监听器
     */
    fun setOnMachineStationClickListener(listener: OnMachineStationClickListener?) {
        this.onMachineStationClickListener = listener
    }

    /**
     * 设置充电站删除事件监听器
     */
    fun setOnMachineStationDeleteListener(listener: OnMachineStationDeleteListener?) {
        this.onMachineStationDeleteListener = listener
    }

    /**
     * 设置是否启用绘制
     */
    fun setDrawingEnabled(enabled: Boolean) {
        this.isDrawingEnabled = enabled
        postInvalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!isDrawingEnabled || machineStation == null) {
            return false
        }

        // 只有在编辑或删除模式下才响应点击事件
        if (currentWorkMode != WorkMode.MODE_MACHINE_STATION_EDIT &&
            currentWorkMode != WorkMode.MODE_MACHINE_STATION_DELETE
        ) {
            return false
        }

        // 检查点击事件类型
        if (event.action == MotionEvent.ACTION_DOWN) {
            val mapView = mParent.get() ?: return false
            val station = machineStation ?: return false
            val screenPoint = PointF(event.x, event.y)

            // 检查是否点击了某个充电站点
            val clickedType = checkPointClick(screenPoint, mapView, station)
            if (clickedType != -1) {
                // 根据当前工作模式调用不同的监听器
                when (currentWorkMode) {
                    WorkMode.MODE_MACHINE_STATION_EDIT -> {
                        onMachineStationClickListener?.onMachineStationClick(station, clickedType)
                    }
                    WorkMode.MODE_MACHINE_STATION_DELETE -> {
                        onMachineStationDeleteListener?.onMachineStationDelete(station, clickedType)
                    }
                    else -> {
                        // 其他模式不处理
                    }
                }
                return true
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * 检查点击是否在某个充电站点上
     * @return 点击的类型：-1-未点击，0-对接点，1-准备点，2-等待点，3-结束停放点
     */
    private fun checkPointClick(
        screenPoint: PointF,
        mapView: MapView,
        station: MachineStation
    ): Int {
        // 检查对接点
        station.locate?.let {
            val point = mapView.worldToScreen(it.x, it.y)
            if (isPointInCircle(screenPoint, point, RING_RADIUS)) {
                return 0
            }
        }

        // 检查准备点
        station.gate?.let {
            val point = mapView.worldToScreen(it.x, it.y)
            if (isPointInCircle(screenPoint, point, RING_RADIUS)) {
                return 1
            }
        }

        // 检查等待点
        station.wait?.coordinate?.let {
            val point = mapView.worldToScreen(it.x, it.y)
            if (isPointInCircle(screenPoint, point, RING_RADIUS)) {
                return 2
            }
        }

        // 检查结束停放点
        station.finish?.let {
            val point = mapView.worldToScreen(it.x, it.y)
            if (isPointInCircle(screenPoint, point, RING_RADIUS)) {
                return 3
            }
        }

        return -1
    }

    /**
     * 判断点是否在圆内
     */
    private fun isPointInCircle(point: PointF, circleCenter: PointF, radius: Float): Boolean {
        val dx = point.x - circleCenter.x
        val dy = point.y - circleCenter.y
        return dx * dx + dy * dy <= radius * radius
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理资源，防止内存泄漏
        clearHomePose()
        isDrawingEnabled = false
        onMachineStationClickListener = null
        onMachineStationDeleteListener = null
    }
}
