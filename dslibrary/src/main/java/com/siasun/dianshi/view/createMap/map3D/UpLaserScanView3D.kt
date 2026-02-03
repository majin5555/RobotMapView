package com.siasun.dianshi.view.createMap.map3D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.Log
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.bean.KeyframePoint
import com.siasun.dianshi.view.SlamWareBaseView
import com.siasun.dianshi.view.createMap.CreateMapWorkMode
import java.lang.ref.WeakReference
import kotlin.math.cos
import kotlin.math.sin

/**
 * 建图上激光点云
 */
@SuppressLint("ViewConstructor")
class UpLaserScanView3D(context: Context?, val parent: WeakReference<CreateMapView3D>) :
    SlamWareBaseView<CreateMapView3D>(context, parent) {
    private val TAG = this::class.java.simpleName

    //激光点云
    private val cloudList: MutableList<PointF> = mutableListOf()

    private var currentWorkMode = CreateMapWorkMode.MODE_SHOW_MAP

    companion object {
        private val paint: Paint = Paint().apply {
            color = Color.RED
            strokeWidth = 3f
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
    fun updateUpLaserScan(laserData: laser_t) {
        cloudList.clear()
        if (laserData.ranges.size <= 6) return // 最少包含机器人位置数据
        val mapView = parent.get() ?: return
        var keyPoints: MutableList<KeyframePoint>? = null

        if (laserData.rad0.toInt() != -1) {
            keyPoints = mutableListOf()
        }

        // 动态计算采样间隔（根据数据量和缩放比例）
        val totalPoints = (laserData.ranges.size - 6) / 3 // 总激光点数（排除机器人位置）
        val baseSampleInterval = when {
            totalPoints > 600 -> 10  // 数据量极大时，间隔10
            totalPoints > 400 -> 5  // 数据量较大时，间隔5
            else -> 2  // 数据量较小时，间隔2
        }
        val dynamicSampleInterval =
            maxOf(baseSampleInterval, (1f / mapView.mSrf.scale).toInt()) // 缩放越小，间隔越大

        // 遍历激光点，按采样间隔降采样
        for (i in 0 until totalPoints step dynamicSampleInterval) {
            val index = 6 + i * 6 // 跳过机器人位置数据（前6个元素）
            if (index + 2 >= laserData.ranges.size) break // 越界保护

            val laserX = laserData.ranges[index]
            val laserY = laserData.ranges[index + 1]

            // 坐标变换（仅计算有效点）
            val cosT = cos(mapView.robotPose[2])
            val sinT = sin(mapView.robotPose[2])
            val laserXNew = laserX * cosT - laserY * sinT + mapView.robotPose[0]
            val laserYNew = laserX * sinT + laserY * cosT + mapView.robotPose[1]

            cloudList.add(PointF(laserXNew, laserYNew))

            // 仅在关键帧时收集完整点云数据
            keyPoints?.add(KeyframePoint(laserX, laserY, laserXNew, laserYNew))
        }
        //添加地图轮廓关键帧
        mapView.mMapOutline3D?.addKeyFrames(laserData, keyPoints)
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
