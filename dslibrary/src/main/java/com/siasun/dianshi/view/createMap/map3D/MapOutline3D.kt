package com.siasun.dianshi.view.createMap.map3D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.Log
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.bean.ConstraintNode
import com.siasun.dianshi.view.SlamWareBaseView
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import com.siasun.dianshi.bean.KeyFrame
import com.siasun.dianshi.bean.KeyframePoint
import com.siasun.dianshi.view.createMap.CreateMapWorkMode
import kotlin.collections.get
import kotlin.math.cos
import kotlin.math.sin
import kotlin.times

/**
 * 建图地图轮廓
 */
@SuppressLint("ViewConstructor")
class MapOutline3D(context: Context?, val parent: WeakReference<CreateMapView3D>) :
    SlamWareBaseView<CreateMapView3D>(context, parent) {
    private val TAG = this::class.java.simpleName
    private var currentWorkMode = CreateMapWorkMode.MODE_SHOW_MAP


    //3D建图关键帧
    private val keyFrames = ConcurrentHashMap<Int, KeyFrame>()


    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: CreateMapWorkMode) {
        if (currentWorkMode == mode) return // 避免重复设置

        currentWorkMode = mode

    }

    companion object {
        val mPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            strokeWidth = 3f
        }

        val greenPaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.FILL
            strokeWidth = 5f
        }
    }


    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        canvas.save()
        // 只有在绘制启用状态下才绘制点云
        if (keyFrames.isNotEmpty()) {
            val mapView = parent.get() ?: return

            keyFrames.values.forEach { frame ->
                val robotScreen = mapView.worldToScreen(frame.robotPos[0], frame.robotPos[1])
                // 绘制关键帧位置
                canvas.drawPoint(robotScreen.x, robotScreen.y, greenPaint)

                frame.points?.let { points ->
                    val contour = points.map { mapView.worldToScreen(it.x, it.y) }

                    // 创建用于绘制点的FloatArray
                    val pointArray = FloatArray(contour.size * 2)
                    contour.forEachIndexed { index, point ->
                        pointArray[index * 2] = point.x
                        pointArray[index * 2 + 1] = point.y
                    }
                    canvas.drawPoints(pointArray, mPaint.apply {
                        strokeWidth = 3f
                        style = Paint.Style.FILL
                    }) // 绘制轮廓点
                }
            }
        }
        canvas.restore()

    }


    /***
     * 添加关键帧
     */
    fun addKeyFrames(
        laserData: laser_t, keyPoints: MutableList<KeyframePoint>?, robotPose: FloatArray
    ) {
        val mapView = parent.get() ?: return
        val rad0 = laserData.rad0.toInt()

        if (!keyFrames.containsKey(rad0)) {
            Log.i(TAG, "关键帧 ID $rad0")
            Log.i(TAG, "关键帧 x ${robotPose[0]}")
            Log.i(TAG, "关键帧 y ${robotPose[1]}")
            Log.i(TAG, "关键帧 t ${robotPose[2]}")
            Log.i(TAG, "关键帧 z ${robotPose[3]}")
            Log.i(TAG, "关键帧 roll ${robotPose[4]}")
            Log.i(TAG, "关键帧 pitch ${robotPose[5]}")

            //关键帧第一帧 要单独显示
            if (rad0 == 0) {
                mapView.mConstrainNodes?.addConstraintNodes(
                    ConstraintNode(
                        rad0,
                        robotPose[0].toDouble(),
                        robotPose[1].toDouble(),
                        robotPose[2].toDouble()
                    )
                )
            }

            keyFrames[rad0] = KeyFrame(keyPoints, robotPose)
            mapView.isStartRevSubMaps = true
        }
    }

    /**
     * 外部接口：更新关键帧数据 nav做回环检测 3D
     */
    fun parseOptPose(laserData: laser_t) {
        // 新增：设置采样间隔（可根据实际需求调整，如每2个关键帧处理1个）
        val SAMPLE_INTERVAL = 2
        var processedCount = 0

        if (laserData.ranges.isEmpty()) return

        // 按采样间隔遍历数据（步长为4*SAMPLE_INTERVAL，每个关键帧占4个Float）
        for (i in 0 until laserData.ranges.size step 4 * SAMPLE_INTERVAL) {
            // 关键帧ID
            val rad0: Int = laserData.ranges[i].toInt()
            // 关键帧位置
            val radX: Float = laserData.ranges[i + 1]
            val radY: Float = laserData.ranges[i + 2]
            val radT: Float = laserData.ranges[i + 3]

            // 获取关键帧数据（非空校验）
            val keyFrame = keyFrames[rad0] ?: continue

            // 更新机器人位置（原子操作，避免中间状态）
            keyFrame.robotPos[0] = radX
            keyFrame.robotPos[1] = radY
            keyFrame.robotPos[2] = radT

            // 优化点：批量更新点云坐标（使用数学运算优化）
            val cosT = cos(radT)
            val sinT = sin(radT)
            val radXOffset = radX
            val radYOffset = radY

            // 仅更新当前关键帧的点云（避免遍历所有关键帧）
            keyFrame.points?.forEach { item ->
                // 复用预计算的三角函数值
                item.x = item.cloudX * cosT - item.cloudY * sinT + radXOffset
                item.y = item.cloudX * sinT + item.cloudY * cosT + radYOffset
            }

            processedCount++
        }
        Log.d(TAG, "更新关键帧数据：处理 $processedCount 个关键帧")
    }

    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理点云数据
        keyFrames.clear()
        // 清理父引用
        parent.clear()
    }
}
