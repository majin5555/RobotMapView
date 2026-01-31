package com.siasun.dianshi.view.createMap.map2D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import com.siasun.dianshi.R
import com.siasun.dianshi.view.SlamWareBaseView
import java.lang.ref.WeakReference

/**
 * 建图 机器人图标 实时位置
 */
@SuppressLint("ViewConstructor")
class RobotView2D(context: Context?, val parent: WeakReference<CreateMapView2D>) :
    SlamWareBaseView<CreateMapView2D>(context, parent) {


    private var currentWorkMode = CreateMapView2D.WorkMode.MODE_SHOW_MAP

    // 机器人相关
    private val robotBitmap: Bitmap? by lazy {
        BitmapFactory.decodeResource(resources, R.mipmap.current_location)
    }


    companion object {
        val robotPaint = Paint().apply {
            isAntiAlias = true
            alpha = 255 // 完全不透明
        }
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: CreateMapView2D.WorkMode) {
        if (currentWorkMode == mode) return // 避免重复设置

        currentWorkMode = mode

    }


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        val mapView = parent.get() ?: return
        val bitmap = robotBitmap ?: return

        val p = mapView.mSrf.worldToScreen(
            mapView.robotPose[0],
            mapView.robotPose[1]
        )


        canvas.translate(p.x, p.y)
        canvas.rotate(-Math.toDegrees(mapView.robotPose[2].toDouble()).toFloat())
        canvas.drawBitmap(bitmap, -bitmap.width / 2f, -bitmap.height / 2f, robotPaint)
        canvas.restore()
    }



    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 释放Bitmap资源，防止内存泄漏
        robotBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
    }

}
