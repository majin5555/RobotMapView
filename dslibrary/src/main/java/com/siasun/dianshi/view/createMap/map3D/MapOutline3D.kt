package com.siasun.dianshi.view.createMap.map3D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.bean.ConstraintNode
import com.siasun.dianshi.view.SlamWareBaseView
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import com.siasun.dianshi.bean.KeyFrame
import com.siasun.dianshi.bean.KeyframePoint
import com.siasun.dianshi.view.WorkMode
import kotlin.math.cos
import kotlin.math.sin

import android.graphics.Matrix
import android.util.Log

/**
 * 建图地图轮廓
 */
@SuppressLint("ViewConstructor")
class MapOutline3D(context: Context?, val parent: WeakReference<CreateMapView3D>) :
    SlamWareBaseView<CreateMapView3D>(context, parent) {
    private val TAG = this::class.java.simpleName
    private var currentWorkMode = WorkMode.MODE_SHOW_MAP

    //3D建图关键帧
    private val keyFrames3D = ConcurrentHashMap<Int, KeyFrame>()

    // 缓存点数，避免每次遍历计算
    private val mCachedPointCount = AtomicInteger(0)

    // 缓存点云绘制数组，避免频繁GC
    private var mPointArray: FloatArray? = null
    private var isDirty = false

    private val mWorldToPixelMatrix = Matrix()

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        if (currentWorkMode == mode) return // 避免重复设置

        currentWorkMode = mode

    }

    companion object {
        val mPaint = Paint().apply {
            color = Color.BLACK
            style = Paint.Style.FILL
            strokeWidth = 3f
        }

        val mGreenDrawPaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.FILL
            strokeWidth = 5f
            strokeCap = Paint.Cap.ROUND
        }
    }


    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.save()
        val mapView = parent.get() ?: return
        if (keyFrames3D.isNotEmpty()) {
            // 1. 构建世界坐标到地图像素坐标的变换矩阵
            // 注意：必须在同步块中获取 mapData 数据
            var resolution = 0.05f
            synchronized(mapView.mSrf.mapData) {
                val mapData = mapView.mSrf.mapData
                resolution = mapData.resolution
                if (resolution <= 0) resolution = 0.05f

                // 构建 World -> Pixel 矩阵
                // px = (wx - originX) / resolution
                // py = height - (wy - originY) / resolution
                mWorldToPixelMatrix.reset()
                mWorldToPixelMatrix.postTranslate(-mapData.originX, -mapData.originY)
                mWorldToPixelMatrix.postScale(1f / resolution, -1f / resolution)
                mWorldToPixelMatrix.postTranslate(0f, mapData.height.toFloat())
            }

            // 2. 组合矩阵：Total = OuterMatrix * WorldToPixelMatrix
            // 注意：Canvas的concat顺序是 preConcat，所以先 concat OuterMatrix (Pixel->Screen)，再 concat WorldToPixel (World->Pixel)
            // 实际上 canvas.concat(M) 等价于 current = current * M.
            // 我们希望 point * M_total -> screen.
            // screen = Outer * Pixel
            // Pixel = WorldToPixel * World
            // screen = Outer * (WorldToPixel * World)
            // 所以 M_total = Outer * WorldToPixel
            val totalMatrix = Matrix(mapView.outerMatrix)
            totalMatrix.preConcat(mWorldToPixelMatrix)

            // 3. 应用矩阵到 Canvas
            canvas.concat(totalMatrix)

            // 4. 调整 Paint 线宽，抵消缩放影响，保持屏幕上固定像素大小
            // 总缩放比例 approx = mapScale / resolution
            val totalScale = mapView.mSrf.scale / resolution
            if (totalScale > 0) {
                mPaint.strokeWidth = 3f / totalScale
                mGreenDrawPaint.strokeWidth = 5f / totalScale
            }

            // 5. 准备点云数据 (仅在脏标记时更新)
            // 获取预估需要的数组大小
            val totalPointsCount = mCachedPointCount.get()

            if (totalPointsCount > 0) {
                // 优化：复用数组，避免频繁分配内存
                if (mPointArray == null || mPointArray!!.size < totalPointsCount * 2) {
                    mPointArray = FloatArray(totalPointsCount * 2)
                    isDirty = true // 数组扩容需要重新填充
                }

                val pointArray = mPointArray!!
                var index = 0

                // 如果数据脏了，重新填充世界坐标
                synchronized(keyFrames3D) {
                    if (isDirty) {
                        keyFrames3D.values.forEach { frame ->
                            frame.points?.forEach { point ->
                                // 直接存储世界坐标，无需 worldToScreen 转换
                                if (index + 1 < pointArray.size) {
                                    pointArray[index++] = point.x
                                    pointArray[index++] = point.y
                                }
                            }
                        }
                        isDirty = false
                    } else {
                        // 如果不脏，index 需要跳到末尾以便绘制正确数量
                        // 注意：这里假设 totalPointsCount 与实际点数一致
                        // 如果出现不一致（如并发修改），可能会有问题，但 isDirty 机制通常能保证
                        index = totalPointsCount * 2
                        // 安全截断
                        if (index > pointArray.size) index = pointArray.size
                    }
                }

                // 一次性绘制所有点云
                canvas.drawPoints(pointArray, 0, index, mPaint)
            }

            // 6. 批量绘制关键帧位置 (也使用世界坐标)
            synchronized(keyFrames3D) {
                keyFrames3D.values.forEach { frame ->
                    canvas.drawPoint(frame.robotPos[0], frame.robotPos[1], mGreenDrawPaint)
                }
            }
        }
        canvas.restore()
    }


    /***
     * 添加关键帧
     */
    fun addKeyFrames(laserData: laser_t, keyPoints: MutableList<KeyframePoint>?) {
        val mapView = parent.get() ?: return
        val rad0 = laserData.rad0.toInt()
        if (rad0 != -1) {
            if (!keyFrames3D.containsKey(rad0)) {
//            Log.i(TAG, "关键帧 ID $rad0")
//            Log.i(TAG, "关键帧 x ${robotPose[0]}")
//            Log.i(TAG, "关键帧 y ${robotPose[1]}")
//            Log.i(TAG, "关键帧 t ${robotPose[2]}")
//            Log.i(TAG, "关键帧 z ${robotPose[3]}")
//            Log.i(TAG, "关键帧 roll ${robotPose[4]}")
//            Log.i(TAG, "关键帧 pitch ${robotPose[5]}")
                //关键帧第一帧 要单独显示
                if (rad0 == 0) {
                    mapView.mConstrainNodes?.addConstraintNodes(
                        ConstraintNode(
                            rad0,
                            mapView.robotPose[0].toDouble(),
                            mapView.robotPose[1].toDouble(),
                            mapView.robotPose[2].toDouble()
                        )
                    )
                }

                // 累加点数缓存
                keyPoints?.size?.let { count ->
                    if (count > 0) {
                        mCachedPointCount.addAndGet(count)
                    }
                }

                keyFrames3D[rad0] = KeyFrame(keyPoints, mapView.robotPose.clone())
                mapView.isStartRevSubMaps = true
                isDirty = true // 数据更新，标记脏
            }
        }
    }

    /**
     * 外部接口：更新关键帧数据 nav做回环检测 3D
     */
    fun parseOptPose(laserData: laser_t) {
        Log.d(TAG, "3D回环检测开始 ${laserData.ranges.size}")

        var processedCount = 0

        if (laserData.ranges.isEmpty()) return

        // 标记脏数据，需要重绘
        var hasUpdate = false

        synchronized(keyFrames3D) {
            // 遍历所有数据（每个关键帧占4个Float: ID, X, Y, Theta）
            // 修复：确保循环不会越界，检查 i+3 是否在范围内
            val size = laserData.ranges.size
            for (i in 0 until (size - 6) step 4) {
                // 关键帧ID
                val rad0: Int = laserData.ranges[i].toInt()
                // 关键帧位置
                val radX: Float = laserData.ranges[i + 1]
                val radY: Float = laserData.ranges[i + 2]
                val radT: Float = laserData.ranges[i + 3]
                val radZ: Float = laserData.ranges[i + 4]
                val radRoll: Float = laserData.ranges[i + 5]
                val radPitch: Float = laserData.ranges[i + 6]

                // 获取关键帧数据（非空校验）
                val keyFrame = keyFrames3D[rad0] ?: continue

                // 更新机器人位置（原子操作，避免中间状态）
                keyFrame.robotPos[0] = radX
                keyFrame.robotPos[1] = radY
                keyFrame.robotPos[2] = radT
                keyFrame.robotPos[3] = radZ
                keyFrame.robotPos[4] = radRoll
                keyFrame.robotPos[5] = radPitch

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
                hasUpdate = true
            }
            if (hasUpdate) isDirty = true
        }

        mPaint.color = Color.BLUE
        Log.d(TAG, "3D回环检测结束 更新关键帧数据：处理 $processedCount 个关键帧")
    }

    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理点云数据
        keyFrames3D.clear()
        mCachedPointCount.set(0)
        // 清理父引用
        parent.clear()
    }
}
