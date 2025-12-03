package com.siasun.dianshi.view

import VirWallLayerNew
import VirtualWallNew
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import java.lang.ref.WeakReference

/**
 * 虚拟墙的View
 */
@SuppressLint("ViewConstructor")
class VirtualLineView(
    context: Context?, parent: WeakReference<MapView>
) : SlamWareBaseView(context, parent) {
    private val LINE_WIDTH = 2f
    private var radius = 5f

    private val PROPORTION = 1000//虚拟墙文件上的是毫米 在本地显示要除1000

    private val mPaint: Paint = Paint()

    //虚拟墙
    private var virtualWall: VirtualWallNew = VirtualWallNew(1, mutableListOf<VirWallLayerNew>())

    // 保存parent引用以便安全访问
    private var mapViewRef: WeakReference<MapView>? = parent

    init {
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.FILL
        mPaint.setColor(Color.BLUE)
        mPaint.strokeWidth = 1f
    }


    /***
     * 1重点虚拟墙
     * 2虚拟门
     * 3普通虚拟墙
     */
    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 安全获取MapView引用
        val mapView = mapViewRef?.get() ?: return

        if (virtualWall.LAYER.size > 0) virtualWall.LAYER[0].let {
            mPaint.strokeWidth = LINE_WIDTH

            for (line in it.LINE) {
                // 根据 CONFIG 值设置不同的画笔样式和颜色
                when (line.CONFIG) {
                    1 -> {
                        // 红色实线 重点虚拟墙
                        mPaint.color = Color.RED
                        mPaint.pathEffect = null
                    }

                    2 -> {
                        // 红色虚线 虚拟门
                        mPaint.color = Color.RED
                        mPaint.pathEffect =
                            DashPathEffect(floatArrayOf(5f, 5f), 0f)
                    }

                    3 -> {
                        // 蓝色实线 普通虚拟墙
                        mPaint.color = Color.BLUE
                        mPaint.pathEffect = null
                    }
                }
                //起点
                val start = mapView.worldToScreen(
                    line.BEGIN.X / PROPORTION, line.BEGIN.Y / PROPORTION
                )
                //终点
                val end = mapView.worldToScreen(
                    line.END.X / PROPORTION, line.END.Y / PROPORTION
                )
                canvas.drawLine(start.x, start.y, end.x, end.y, mPaint)
                canvas.drawCircle(start.x, start.y, radius, mPaint)
                canvas.drawCircle(end.x, end.y, radius, mPaint)
            }
        }
    }

    /**
     * 设置虚拟墙
     */
    fun setVirtualWall(virtualWall: VirtualWallNew) {
        this.virtualWall = virtualWall
        postInvalidate()
    }

    /**
     * 清理资源，防止内存泄漏
     */
    private fun cleanup() {
        mapViewRef?.clear()
        mapViewRef = null
    }

    /**
     * View被移除时调用，清理资源
     */
    override fun onDetachedFromWindow() {
        cleanup()
        super.onDetachedFromWindow()
    }

}


