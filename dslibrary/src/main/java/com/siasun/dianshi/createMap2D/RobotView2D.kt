package com.siasun.dianshi.createMap2D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import com.siasun.dianshi.R
import com.siasun.dianshi.view.MapView
import com.siasun.dianshi.view.SlamWareBaseView
import java.lang.ref.WeakReference

/**
 * 建图 机器人图标 实时位置
 */
@SuppressLint("ViewConstructor")
class RobotView2D(context: Context?, val parent: WeakReference<CreateMapView2D>) :
    SlamWareBaseView<CreateMapView2D>(context, parent) {

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
                // 重置变换矩阵，避免变换累积导致的跳动
                onRobotMatrix.reset()
                // 将世界坐标转换为屏幕坐标
                val screenPos = mapView.worldToScreen(pose[0].toFloat(), pose[1].toFloat())

                // 计算图标中心点偏移，使图标中心与坐标点重合
                val offsetX = -bitmap.width / 2f
                val offsetY = -bitmap.height / 2f

                // 设置变换矩阵：
                // 1. 先平移到原点（以图标中心为锚点）
                onRobotMatrix.postTranslate(offsetX, offsetY)
                // 2. 然后应用旋转（以图标中心为轴心）
                onRobotMatrix.postRotate(
                    -pose[2].toFloat(),
                    0f, 0f // 旋转轴心为图标中心
                )
                // 3. 最后平移到屏幕目标位置
                onRobotMatrix.postTranslate(screenPos.x, screenPos.y)

                // 绘制机器人图标
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
