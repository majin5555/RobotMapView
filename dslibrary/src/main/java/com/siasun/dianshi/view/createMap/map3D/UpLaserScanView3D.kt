package com.siasun.dianshi.view.createMap.map3D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import com.siasun.dianshi.view.SlamWareBaseView
import com.siasun.dianshi.view.createMap.CreateMapWorkMode
import java.lang.ref.WeakReference

/**
 * 建图上激光点云
 */
@SuppressLint("ViewConstructor")
class UpLaserScanView3D(context: Context?, val parent: WeakReference<CreateMapView3D>) :
    SlamWareBaseView<CreateMapView3D>(context, parent) {
    private val TAG = "UpLaserScanView3D"

    //激光点云
    private val cloudList: MutableList<PointF> = mutableListOf()

    private var currentWorkMode = CreateMapWorkMode.MODE_SHOW_MAP

    companion object {
        private val paint: Paint = Paint().apply {
            color = Color.RED
            strokeWidth = 2f
            style = Paint.Style.FILL
        }
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: CreateMapWorkMode) {
        if (currentWorkMode == mode) return // 避免重复设置

        currentWorkMode = mode

    }

    /**
     * 上激光点云
     */
    fun updateUpLaserScan(laserXNew: Float, laserYNew: Float) {
        cloudList.clear()
        cloudList.add(PointF(laserXNew, laserYNew))
        postInvalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        if (cloudList.isNotEmpty()) {
            val mapView = parent.get() ?: return
            cloudList.forEach {
                val p = mapView.worldToScreen(it.x, it.y)
                canvas.drawPoint(p.x, p.y, paint)
            }
        }
        canvas.restore()
    }


    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理点云数据
        cloudList.clear()
        // 清理父引用
        parent.clear()
    }
}
