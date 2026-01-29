package com.siasun.dianshi.createMap2D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
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

    private var agvPose: FloatArray? = null
    val matrixRobot = Matrix()

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


    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mapView = parent.get() ?: return

        agvPose?.let { pose ->
            robotBitmap?.let { bitmap ->
                // 重置变换矩阵，避免变换累积导致的跳动
                matrixRobot.reset()
                val screenPos = mapView.worldToScreen(pose[0], pose[1])
                matrixRobot.postTranslate(-bitmap.width / 2f, -bitmap.height / 2f) // 以中心为锚点
                matrixRobot.postRotate(-Math.toDegrees(pose[2].toDouble()).toFloat(),0f,0f) // 旋转方向调整
                matrixRobot.postTranslate(screenPos.x, screenPos.y)
                canvas.drawBitmap(bitmap, matrixRobot, robotPaint)
            }
        }
    }


    /**
     * 车体实时坐标
     */
    fun setAgvData(array: FloatArray) {
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
        // 清理其他资源
        agvPose = null
    }

    override fun setMatrixWithScale(matrix: Matrix, scale: Float) {
        super.setMatrixWithScale(matrix, scale)
    }

    override fun setMatrixWithRotation(matrix: Matrix, rotation: Float) {
        super.setMatrixWithRotation(matrix, rotation)
    }
}
