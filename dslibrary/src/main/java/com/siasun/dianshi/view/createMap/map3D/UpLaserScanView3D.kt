package com.siasun.dianshi.view.createMap.map3D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Matrix
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.view.SlamWareBaseView
import com.siasun.dianshi.view.WorkMode
import java.lang.ref.WeakReference
import kotlin.math.cos
import kotlin.math.sin

@SuppressLint("ViewConstructor")
class UpLaserScanView3D(context: Context?, val parent: WeakReference<CreateMapView3D>) :
    SlamWareBaseView<CreateMapView3D>(context, parent) {

    private val TAG = this::class.java.simpleName

    //激光点云 (使用 FloatArray 存储世界坐标 [x1, y1, x2, y2, ...])
    private var cloudPoints: FloatArray = FloatArray(0)
    private var pointCount: Int = 0

    // 关键帧缓存数组
    private var keyframeCloudBuf: FloatArray? = null
    private var keyframeWorldBuf: FloatArray? = null

    private val mWorldToPixelMatrix = Matrix()
    private val mTotalMatrix = Matrix()


    private var currentWorkMode = WorkMode.MODE_SHOW_MAP

    companion object {
        private val paint: Paint = Paint().apply {
            color = Color.RED
            strokeWidth = 5f
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
     * 建图上激光点云
     */
    fun updateUpLaserScan(laserData: laser_t) {
        if (laserData.ranges.size <= 6) {
            pointCount = 0
            return // 最少包含机器人位置数据
        }
        val mapView = parent.get() ?: return

        val isKeyframe = laserData.rad0.toInt() != -1
        val totalPoints = (laserData.ranges.size - 6) / 3
        val baseSampleInterval = 5
        val dynamicSampleInterval =
            maxOf(baseSampleInterval, (1f / mapView.mSrf.scale).toInt()) // 缩放越小，间隔越大

        // 预估最大需要的点数，避免频繁扩容
        val estimatedMaxPoints = (totalPoints / dynamicSampleInterval) + 10
        if (cloudPoints.size < estimatedMaxPoints * 2) {
            cloudPoints = FloatArray(estimatedMaxPoints * 2)
        }

        pointCount = 0

        // 准备关键帧缓存，并通过局部变量避免 smart cast 问题
        var keyframeCount = 0
        // 局部不可变引用，供循环和后续使用
        var localCloudBuf: FloatArray? = null
        var localWorldBuf: FloatArray? = null

        if (isKeyframe) {
            val needed = estimatedMaxPoints * 2
            if (keyframeCloudBuf == null || keyframeCloudBuf!!.size < needed) {
                keyframeCloudBuf = FloatArray(needed)
            }
            if (keyframeWorldBuf == null || keyframeWorldBuf!!.size < needed) {
                keyframeWorldBuf = FloatArray(needed)
            }
            localCloudBuf = keyframeCloudBuf
            localWorldBuf = keyframeWorldBuf
        }

        val robotX = mapView.robotPose[0]
        val robotY = mapView.robotPose[1]
        val robotTheta = mapView.robotPose[2]
        val cosT = cos(robotTheta)
        val sinT = sin(robotTheta)

        var keyIdx = 0
        for (i in 0 until totalPoints step dynamicSampleInterval) {
            val index = 6 + i * 6 // 跳过机器人位置数据（前6个元素）
            if (index + 2 >= laserData.ranges.size) break // 越界保护

            val laserX = laserData.ranges[index]
            val laserY = laserData.ranges[index + 1]
            val worldX = laserX * cosT - laserY * sinT + robotX
            val worldY = laserX * sinT + laserY * cosT + robotY

            if (pointCount * 2 + 1 < cloudPoints.size) {
                cloudPoints[pointCount * 2] = worldX
                cloudPoints[pointCount * 2 + 1] = worldY
                pointCount++
            }

            // 使用局部变量，编译器可智能转换
            if (isKeyframe && localCloudBuf != null && localWorldBuf != null) {
                if (keyIdx + 1 < localCloudBuf.size) {
                    localCloudBuf[keyIdx] = laserX
                    localCloudBuf[keyIdx + 1] = laserY
                    localWorldBuf[keyIdx] = worldX
                    localWorldBuf[keyIdx + 1] = worldY
                    keyIdx += 2
                    keyframeCount++
                }
            }
        }

        if (isKeyframe && keyframeCount > 0 && localCloudBuf != null && localWorldBuf != null) {
            mapView.mMapOutline3D?.addKeyFrames(
                laserData,
                localCloudBuf,
                localWorldBuf,
                keyframeCount
            )
        }

        postInvalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        if (pointCount > 0) {
            val mapView = parent.get() ?: return
            
            // 使用 Canvas 矩阵变换 (Hardware Accelerated)
            // 1. 构建变换矩阵
             var resolution = 0.05f
            synchronized(mapView.mSrf.mapData) {
                val mapData = mapView.mSrf.mapData
                resolution = mapData.resolution
                if (resolution <= 0) resolution = 0.05f

                mWorldToPixelMatrix.reset()
                mWorldToPixelMatrix.postTranslate(-mapData.originX, -mapData.originY)
                mWorldToPixelMatrix.postScale(1f / resolution, -1f / resolution)
                mWorldToPixelMatrix.postTranslate(0f, mapData.height.toFloat())
            }

            mTotalMatrix.set(mapView.outerMatrix)
            mTotalMatrix.preConcat(mWorldToPixelMatrix)

            canvas.concat(mTotalMatrix)
            
            // 2. 调整 Paint 大小以抵消缩放
             val totalScale = mapView.mSrf.scale / resolution
             if (totalScale > 0) {
                 paint.strokeWidth = 3f / totalScale
             }
             
            // 3. 直接绘制世界坐标点
            canvas.drawPoints(cloudPoints, 0, pointCount * 2, paint)
        }
        canvas.restore()
    }


    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理点云数据
        pointCount = 0
        cloudPoints = FloatArray(0)
        keyframeCloudBuf = null
        keyframeWorldBuf = null
        // 清理父引用
        parent.clear()
    }
}