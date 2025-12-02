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

/**
 * 机器人图标 实时位置 、有任务状态下的路径
 */
class RobotView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView(context, parent) {
    private var radius = 8f

    private var mPaint: Paint = Paint()
    private var pathList: MutableList<PointF>? = null
    private var agvPose: DoubleArray? = null
    private val onRobotMatrix = Matrix()

    // 机器人相关
    private val robotBitmap: Bitmap? by lazy {
        BitmapFactory.decodeResource(resources, R.mipmap.current_location)
    }

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
            canvas.drawCircle(
                point.x, point.y, radius * scale, mPaint
            )
        }
    
        agvPose?.let { pose ->
            robotBitmap?.let { bitmap ->
                val point = parent.get()!!
                    .worldToScreen(pose[0].toFloat(), pose[1].toFloat())
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
                // 绘制机器人图标（使用矩阵变换）
                canvas.drawBitmap(bitmap, onRobotMatrix, mPaint)
            }
        }
    }

    /**
     * 机器人有任务实时路径
     */
    fun setData(list: MutableList<PointF>) {
        pathList = list
        postInvalidate()
    }

    /**
     * 车体实时坐标
     */
    fun setAgvData(array: DoubleArray) {
        agvPose = array
        postInvalidate()
    }
}
