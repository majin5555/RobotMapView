package com.siasun.dianshi.view.createMap.map3D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.view.SlamWareBaseView
import com.siasun.dianshi.view.WorkMode
import java.lang.ref.WeakReference
import com.siasun.dianshi.bean.OldKeyFrame

/**
 * 扩展地图定位模式 所有关键帧
 */
@SuppressLint("ViewConstructor")
class AllKeyFrameView3D(context: Context?, val parent: WeakReference<CreateMapView3D>) :
    SlamWareBaseView<CreateMapView3D>(context, parent) {
    private val TAG = this::class.java.simpleName

    //定位模式地图关键帧地图路线
    private val mMapPath: MutableList<OldKeyFrame> = mutableListOf()

    private var currentWorkMode = WorkMode.MODE_SHOW_MAP

    companion object {
        private val paint: Paint = Paint().apply {
            color = Color.parseColor("#800080")
            strokeWidth = 10f
            strokeCap = Paint.Cap.ROUND
            style = Paint.Style.FILL
        }

        private val textPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLUE
            textSize = 20f
            textAlign = Paint.Align.CENTER
        }

        private val arrowPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLUE
            style = Paint.Style.FILL
        }

        private const val RAD_TO_DEG = 57.29578f

        private val arrowPath: Path = Path().apply {
            moveTo(26f, 0f)
            lineTo(14f, -6f)
            lineTo(14f, 6f)
            close()
        }
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        if (currentWorkMode == mode) return // 避免重复设置

        currentWorkMode = mode

    }

    /**
     * 外部接口：更新关键帧数据 拓展地图时显示所有关键帧位置
     */
    fun parseKeyFramePose(mLaserT: laser_t) {
        mMapPath.clear()
        if (mLaserT.ranges.isNotEmpty()) {
            // 每3个数据为一组: x, y, theta
            for (i in 0 until mLaserT.ranges.size / 3) {
                //关键帧  x
                val radX: Float = mLaserT.ranges[3 * i]
                //关键帧  y
                val radY: Float = mLaserT.ranges[3 * i + 1]
                //关键帧角度 theta
                val theta: Float = mLaserT.ranges[3 * i + 2]
                mMapPath.add(OldKeyFrame(radX, radY, theta, i))
            }
        }
    }

    @SuppressLint("DrawAllocation", "DefaultLocale")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mapView = parent.get() ?: return
        canvas.save()
        for (point in mMapPath) {
            val worldToScreen = mapView.worldToScreen(point.x, point.y)
            // 使用局部变量减少重复计算
            val mPoints = floatArrayOf(worldToScreen.x, worldToScreen.y)
            canvas.drawPoints(mPoints, paint)

            val rotation = -point.theta * RAD_TO_DEG
            canvas.save()
            canvas.translate(worldToScreen.x, worldToScreen.y)
            canvas.rotate(rotation)
            canvas.drawPath(arrowPath, arrowPaint)
            canvas.restore()

            canvas.drawText("ID:${point.id}", worldToScreen.x, worldToScreen.y + 25f, textPaint)
        }
        canvas.restore()
    }


    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理父引用
        parent.clear()
    }
}
