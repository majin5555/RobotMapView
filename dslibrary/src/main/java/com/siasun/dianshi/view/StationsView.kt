package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.siasun.dianshi.R
import com.siasun.dianshi.bean.CmsStation
import java.lang.ref.WeakReference

/**
 * 站点(避让点)
 */
@SuppressLint("ViewConstructor")
class StationsView(context: Context?, var parent: WeakReference<MapView>) :
    SlamWareBaseView(context, parent) {
    private var mPaint: Paint = Paint()
    private var radius = 10f

    //避让点
    private val cmsStations = mutableListOf<CmsStation>()

    init {
        mPaint.setColor(Color.RED)
        mPaint.isAntiAlias = true
        mPaint.style = Paint.Style.FILL
        mPaint.strokeWidth = 1f
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        cmsStations.forEach { station ->
            station.coordinate?.let {
                val locate = parent.get()!!.worldToScreen(it.x, it.y)
                drawCircle(canvas, locate.x, locate.y, radius, mPaint)
                drawLabel(
                    canvas,
                    station.evName ?: context.getString(R.string.cms_station),
                    locate.x,
                    locate.y,
                    mPaint
                )
            }
        }
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
