package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import com.siasun.dianshi.R
import java.lang.ref.WeakReference

/**
 * 机器人图标 实时位置 、有任务状态下的路径
 */
@SuppressLint("ViewConstructor")
class RobotView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView(context, parent) {

    private var mPaint: Paint? = null
    private var robotPaint: Paint? = null
    private var agvPose: DoubleArray? = null
    private val onRobotMatrix = Matrix()

    // 机器人相关
    private val robotBitmap: Bitmap? by lazy {
        BitmapFactory.decodeResource(resources, R.mipmap.current_location)
    }

    init {
        // 初始化画笔
        initPaints()
    }

    private fun initPaints() {
        mPaint = Paint().apply {
            strokeWidth = 1f
            isAntiAlias = true
            style = Paint.Style.FILL
            alpha = 100
            color = Color.parseColor("#d2f0f4")
        }
        robotPaint = Paint().apply {
            isAntiAlias = true
            alpha = 255 // 完全不透明
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mapView = parent.get() ?: return
        agvPose?.let { pose ->
            robotBitmap?.let { bitmap ->
                val point = mapView.worldToScreen(pose[0].toFloat(), pose[1].toFloat())
                // 重置并设置变换矩阵
                onRobotMatrix.reset()
                // 先平移到目标位置
                onRobotMatrix.postTranslate(point.x, point.y)
                // 然后应用缩放
                onRobotMatrix.postScale(scale, scale, point.x, point.y)
                // 最后应用旋转（以图标中心为轴心）
                onRobotMatrix.postRotate(
                    -Math.toDegrees(pose[2]).toFloat(),
                    point.x,
                    point.y
                )
                // 绘制机器人图标（使用专用画笔）
                robotPaint?.let {
                    canvas.drawBitmap(bitmap, onRobotMatrix, it)
                }
            }
        }
    }


    /**
     * 车体实时坐标
     */
    fun setAgvData(array: DoubleArray) {
        agvPose = array
        postInvalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 释放Bitmap资源，防止内存泄漏
        robotBitmap?.let { bitmap ->
            if (!bitmap.isRecycled) {
                bitmap.recycle()
            }
        }
        // 清理Paint对象
        mPaint?.reset()
        mPaint = null
        robotPaint?.reset()
        robotPaint = null
        // 清理其他资源
        agvPose = null
    }
}
