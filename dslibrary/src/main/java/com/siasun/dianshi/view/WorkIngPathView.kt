package com.siasun.dianshi.view

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import com.siasun.dianshi.R
import java.lang.ref.WeakReference
import java.util.Collections

/**
 *  工作路径
 *  有任务状态下的路径
 */
class WorkIngPathView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView(context, parent) {
    private var radius = 8f

    private var mPaint: Paint = Paint()
    private var pathList: MutableList<PointF>? = null

    //机器人有任务 实时路径
    private val cartPosList = Collections.synchronizedList(mutableListOf<PointF>())

    init {
        mPaint.strokeWidth = 1f
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.FILL
        mPaint.setAlpha(100)
        mPaint.setColor(Color.parseColor("#d2f0f4"))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        //绘制实时路径
        pathList?.forEach {
            val point = parent.get()!!.worldToScreen(it.x, it.y)
            drawCircle(
                canvas,
                point.x, point.y, radius * scale, mPaint
            )
        }
    }

    /**
     * 机器人有任务实时路径
     */
    fun setData(mCarPoint: PointF) {
        cartPosList.add(mCarPoint)
        postInvalidate()
    }


}
