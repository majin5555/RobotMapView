package com.siasun.dianshi.view.createMap.map2D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.bean.KeyFrame2D
import com.siasun.dianshi.bean.OldKeyFrame
import com.siasun.dianshi.view.SlamWareBaseView
import com.siasun.dianshi.view.WorkMode
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap

/**
 * 建图上关键帧ID
 */
@SuppressLint("ViewConstructor")
class MapCreatingKeyFramView2D(context: Context?, val parent: WeakReference<CreateMapView2D>) :
    SlamWareBaseView<CreateMapView2D>(context, parent) {
    private val keyFrames2D = ConcurrentHashMap<Int, KeyFrame2D>()

    //定位模式地图关键帧地图路线
    private val mMapPath: MutableList<OldKeyFrame> = mutableListOf()
    private var currentWorkMode = WorkMode.MODE_SHOW_MAP

    companion object {
        private val paint: Paint = Paint().apply {
            color = Color.GREEN
            strokeWidth = 8f
            style = Paint.Style.FILL
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
            for (i in 0 until mLaserT.ranges.size) {
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

    /**
     * 关键帧数据
     */
    fun updateData(laserData: laser_t) {
        val rad0 = laserData.rad0.toInt()
        // 仅显示第一个关键帧（假设为0或1），以及第5个、第10个等5的倍数
        if (rad0 != -1 && (rad0 == 1 || rad0 % 5 == 0)) {
            val mapView = parent.get() ?: return
            if (!keyFrames2D.containsKey(rad0)) {
                keyFrames2D[rad0] = KeyFrame2D(mapView.robotPose.clone())
            }
        }
        postInvalidate()
    }


    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        drawKeyFrameOld(canvas)
        drawKeyFrame(canvas)
        canvas.restore()
    }

    private fun drawKeyFrame(canvas: Canvas) {
        val mapView = parent.get() ?: return
        if (keyFrames2D.isEmpty()) return

        val points = FloatArray(keyFrames2D.size * 2)
        var index = 0
        keyFrames2D.values.forEach { frame ->
            val screenPt = mapView.worldToScreen(frame.robotPos[0], frame.robotPos[1])
            points[index++] = screenPt.x
            points[index++] = screenPt.y
        }

        // 绘制成绿色的原点
        paint.strokeCap = Paint.Cap.ROUND
        canvas.drawPoints(points, paint)
    }


    private fun drawKeyFrameOld(canvas: Canvas) {
        val mapView = parent.get() ?: return

        for (point in mMapPath) {
            val worldToScreen = mapView.worldToScreen(point.x, point.y)
            // 使用局部变量减少重复计算
            val mPoints = floatArrayOf(worldToScreen.x, worldToScreen.y)
            canvas.drawPoints(mPoints, paint)
        }
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
