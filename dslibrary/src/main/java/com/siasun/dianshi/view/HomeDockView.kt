package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import androidx.core.content.ContextCompat
import com.siasun.dianshi.R
import com.siasun.dianshi.bean.MachineStation
import java.lang.ref.WeakReference

/**
 * 充电桩（一体机）位姿信息视图
 */
@SuppressLint("ViewConstructor")
class HomeDockView(context: Context?, parent: WeakReference<MapView>) :
    SlamWareBaseView(context, parent) {

    // 绘图参数常量
    private companion object {
        const val BASE_RADIUS = 10f
        const val BASE_TEXT_SIZE = 10f
        const val LABEL_OFFSET = 15f  // 标签与图标的偏移量
    }

    // 绘图画笔
    private val mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
        style = Paint.Style.FILL
        textSize = BASE_TEXT_SIZE
    }

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

    /**
     * 清除一体机数据
     */
    fun clearHomePose() {
        machineStation = null
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isDrawingEnabled) {
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
            drawFinishPoint(canvas, mapView, station)}
        }

        /**
         * 绘制一体机对接点
         */
        private fun drawLocatePoint(canvas: Canvas, mapView: MapView, station: MachineStation) {
            station.locate?.let { locate ->
                val screenPoint = mapView.worldToScreen(locate.x, locate.y)
                val labelPoint = PointF(screenPoint.x + LABEL_OFFSET, screenPoint.y - LABEL_OFFSET)

                mPaint.color = ContextCompat.getColor(context, R.color.color_175E7A)
                drawCircle(canvas, screenPoint, BASE_RADIUS, mPaint)
                drawLabel(
                    canvas,
                    context.getString(R.string.machine_locate_point),
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
                val screenPoint = mapView.worldToScreen(gate.x, gate.y)
                val labelPoint = PointF(screenPoint.x + LABEL_OFFSET, screenPoint.y - LABEL_OFFSET)

                mPaint.color = ContextCompat.getColor(context, R.color.color_54A8BA)
                drawRect(canvas, screenPoint, mPaint)
                drawLabel(
                    canvas,
                    context.getString(R.string.machine_gate_point),
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
                val screenPoint = mapView.worldToScreen(coordinate.x, coordinate.y)
                val labelPoint = PointF(screenPoint.x + LABEL_OFFSET, screenPoint.y - LABEL_OFFSET)

                mPaint.color = ContextCompat.getColor(context, R.color.color_C85024)
                drawTriangle(canvas, screenPoint, mPaint)
                drawLabel(
                    canvas,
                    context.getString(R.string.machine_wait_point),
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
                val screenPoint = mapView.worldToScreen(finish.x, finish.y)
                val labelPoint = PointF(screenPoint.x + LABEL_OFFSET, screenPoint.y - LABEL_OFFSET)

                mPaint.color = ContextCompat.getColor(context, R.color.color_FFD44F)
                drawDiamond(canvas, screenPoint, mPaint)
                drawLabel(
                    canvas,
                    context.getString(R.string.machine_finish_point),
                    labelPoint,
                    mPaint
                )
            }
        }

        /**
         * 设置是否启用绘制
         */
        fun setDrawingEnabled(enabled: Boolean) {
            this.isDrawingEnabled = enabled
            postInvalidate()
        }
    }
