package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.siasun.dianshi.R
import com.siasun.dianshi.bean.ElevatorPoint
import java.lang.ref.WeakReference

/**
 * 乘梯点
 */
@SuppressLint("ViewConstructor")
class ElevatorView(context: Context?, parent: WeakReference<MapView>) :
    SlamWareBaseView(context, parent) {

    // 绘图参数常量
    private companion object {
        const val BASE_RADIUS = 10f
        const val BASE_TEXT_SIZE = 10f
    }

    // 绘图画笔
    private val mPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeWidth = 1f
        style = Paint.Style.FILL
        textSize = BASE_TEXT_SIZE
    }

    // 乘梯点
    private var elevatorsList = mutableListOf<ElevatorPoint>()


    // 控制是否绘制
    private var isDrawingEnabled: Boolean = true

    /**
     * 设置乘梯点数据
     */
    fun setElevators(list: MutableList<ElevatorPoint>?) {
        list?.let {
            elevatorsList.clear()
            elevatorsList.addAll(it)
            postInvalidate()
        }
    }

    /**
     * 清除一体机数据
     */
    fun clearElevators() {
        elevatorsList.clear()
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isDrawingEnabled) {
            val mapView = mParent.get() ?: return

            for (item in elevatorsList) {
                item.gatePoint?.let { gate ->
                    mPaint.setColor(Color.GRAY)
                    val pointLocation = mapView.worldToScreen(gate.x, gate.y)
                    drawCircle(canvas, pointLocation, BASE_RADIUS, mPaint)
                    pointLocation.x += 10f
                    pointLocation.y += 10f
                    drawLabel(canvas, context.getString(R.string.gate_point), pointLocation, mPaint)
                }

                item.pstPark?.let { pstPark ->
                    mPaint.setColor(Color.GRAY)
                    val pointLocation = mapView.worldToScreen(pstPark.x, pstPark.y)
                    drawCircle(canvas, pointLocation, BASE_RADIUS, mPaint)
                    pointLocation.x += 10f
                    pointLocation.y += 10f
                    drawLabel(
                        canvas, context.getString(R.string.pst_park_point), pointLocation, mPaint
                    )
                }
            }
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
