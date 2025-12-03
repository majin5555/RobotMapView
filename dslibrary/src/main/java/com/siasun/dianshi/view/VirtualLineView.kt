package com.siasun.dianshi.view

import VirWallLayerNew
import VirtualWallNew
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import java.lang.ref.WeakReference

/**
 * 虚拟墙的View
 */
@SuppressLint("ViewConstructor")
class VirtualLineView(
    context: Context?, var parent: WeakReference<MapView>
) : SlamWareBaseView(context, parent) {
    private val LINE_WIDTH = 1f
    private var radius = 5f

    private val PROPORTION = 1000//虚拟墙文件上的是毫米 在本地显示要除1000

    private val mPaint: Paint = Paint()

    //虚拟墙
    private var virtualWall: VirtualWallNew = VirtualWallNew(1, mutableListOf<VirWallLayerNew>())

    init {
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.FILL
        mPaint.setColor(Color.BLUE)
        mPaint.strokeWidth = 1f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (virtualWall.LAYER.size > 0)
            virtualWall.LAYER[0].let {
                mPaint.strokeWidth = LINE_WIDTH * scale

                for (line in it.LINE) {
                    val start = parent.get()!!.worldToScreen(
                        line.BEGIN.X / PROPORTION, line.BEGIN.Y / PROPORTION
                    )
                    val end = parent.get()!!.worldToScreen(
                        line.END.X / PROPORTION, line.END.Y / PROPORTION
                    )
                    canvas.drawLine(start.x, start.y, end.x, end.y, mPaint)
                    canvas.drawCircle(start.x, start.y, radius * scale, mPaint)
                    canvas.drawCircle(end.x, end.y, radius * scale, mPaint)
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
}

