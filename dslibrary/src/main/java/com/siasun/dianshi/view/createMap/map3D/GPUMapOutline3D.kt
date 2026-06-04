package com.siasun.dianshi.view.createMap.map3D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.bean.ConstraintNode
import com.siasun.dianshi.bean.KeyFrame
import com.siasun.dianshi.bean.KeyframePoint
import com.siasun.dianshi.view.SlamWareBaseView
import com.siasun.dianshi.view.WorkMode
import java.lang.ref.WeakReference
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.cos
import kotlin.math.sin
import androidx.core.graphics.withSave
import androidx.core.graphics.withTranslation
import androidx.core.graphics.withMatrix

@SuppressLint("ViewConstructor")
class GPUMapOutline3D(context: Context?, val parent: WeakReference<CreateMapView3D>) :
    SlamWareBaseView<CreateMapView3D>(context, parent) {

    private val TAG = this::class.java.simpleName
    private var currentWorkMode = WorkMode.MODE_SHOW_MAP

    // GPU 点云渲染器
    private lateinit var pointCloudView: PointCloudTextureView

    // 关键帧数据
    private val keyFrames3D = ConcurrentHashMap<Int, KeyFrame>()
    private val mCachedPointCount = AtomicInteger(0)

    // 矩阵相关（使用 android.graphics.Matrix）
    private val mWorldToPixelMatrix = Matrix()
    private val mTotalMatrix = Matrix()
    private val mInverseMatrix = Matrix()
    private val mTempPts = FloatArray(2)

    // 成员变量区新增
    private val mMatrixValues = FloatArray(9)          // 复用数组，避免每帧 new
    // 如果 KeyFrame 类不支持 idString，可以维护一个缓存 Map
    private val idStringCache = ConcurrentHashMap<Int, String>()

    private var isDrawingEnabled = false

    private val mGreenDrawPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        strokeWidth = 8f
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }
    private val mArrowPath = Path().apply {
        moveTo(12f, 0f)
        lineTo(-6f, -4f)
        lineTo(-6f, 4f)
        close()
    }
    private val mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textSize = 10f
        textAlign = Paint.Align.CENTER
    }

    init {
        setBackgroundColor(Color.TRANSPARENT)
        setWillNotDraw(false)  // 需要 onDraw 绘制关键帧

        // 创建并添加点云子视图
        pointCloudView = PointCloudTextureView(context!!)
        pointCloudView.setBackgroundColor(Color.TRANSPARENT)
        addView(pointCloudView) // 直接加入 ViewGroup
    }

    fun setDrawingEnabled(enabled: Boolean) {
        isDrawingEnabled = enabled
        invalidate()
    }

    fun setWorkMode(mode: WorkMode) {
        currentWorkMode = mode
    }

    // ---------- 确保子 View 正确布局 ----------
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        // 测量点云视图，使其填充整个父容器
        val child = getChildAt(0)
        child?.measure(widthMeasureSpec, heightMeasureSpec)
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val child = getChildAt(0)
        child?.layout(0, 0, r - l, b - t)
    }

    // ---------- 点云数据更新 ----------
    fun addKeyFrames(laserData: laser_t, keyPoints: MutableList<KeyframePoint>?) {
        val mapView = parent.get() ?: return
        val rad0 = laserData.rad0.toInt()
        if (rad0 != -1 && (rad0 == 0 || rad0 % 4 == 0)) {
            if (!keyFrames3D.containsKey(rad0)) {
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

                synchronized(keyFrames3D) {
                    keyPoints?.size?.let { count ->
                        if (count > 0) mCachedPointCount.addAndGet(count)
                    }
                    keyFrames3D[rad0] = KeyFrame(keyPoints, mapView.robotPose.clone())
                    mapView.isStartRevSubMaps = true

                    // 追加点到 GPU 渲染器
                    keyPoints?.let { pts ->
                        val arr = FloatArray(pts.size * 2)
                        for (i in pts.indices) {
                            arr[i * 2] = pts[i].x
                            arr[i * 2 + 1] = pts[i].y
                        }
                        pointCloudView.addPoints(arr)
                    }
                }
            }
        }
    }

    var rangeSize = 0
    fun parseOptPose(laserData: laser_t) {
        if (laserData.ranges.isEmpty()) return
        if (rangeSize == laserData.ranges.size) return
        rangeSize = laserData.ranges.size

        var hasUpdate = false
        synchronized(keyFrames3D) {
            val size = laserData.ranges.size
            for (i in 0 until size step 4) {
                val rad0 = laserData.ranges[i].toInt()
                val radX = laserData.ranges[i + 1]
                val radY = laserData.ranges[i + 2]
                val robotTheta = laserData.ranges[i + 3]

                val keyFrame = keyFrames3D[rad0] ?: continue
                keyFrame.robotPos[0] = radX
                keyFrame.robotPos[1] = radY
                keyFrame.robotPos[2] = robotTheta

                val cosT = cos(robotTheta)
                val sinT = sin(robotTheta)
                keyFrame.points?.forEach { item ->
                    item.x = item.cloudX * cosT - item.cloudY * sinT + radX
                    item.y = item.cloudX * sinT + item.cloudY * cosT + radY
                }
                hasUpdate = true
            }
        }

        if (hasUpdate) {
            reloadAllPoints()
            invalidate()
        }
    }

    private fun reloadAllPoints() {
        pointCloudView.clearDots()
        val totalFloats = mCachedPointCount.get() * 2
        if (totalFloats == 0) return

        val buffer = FloatArray(totalFloats)
        var idx = 0
        synchronized(keyFrames3D) {
            for (frame in keyFrames3D.values) {
                frame.points?.forEach { p ->
                    if (idx + 1 < totalFloats) {
                        buffer[idx++] = p.x
                        buffer[idx++] = p.y
                    }
                }
            }
        }
        if (idx > 0) {
            pointCloudView.addPoints(buffer.copyOf(idx))
        }
    }

    // ---------- 变换矩阵同步（外部调用） ----------
    /**
     * 由 CreateMapView3D 调用，传入 outerMatrix、地图分辨率等
     */
    fun updateMatrixAndScale(
        outerMatrix: Matrix,
        scale: Float,
        resolution: Float,
        mapWidth: Int,
        mapHeight: Int,
        originX: Float,
        originY: Float
    ) {
        // 构建 World -> Pixel 矩阵
        mWorldToPixelMatrix.reset()
        mWorldToPixelMatrix.postTranslate(-originX, -originY)
        mWorldToPixelMatrix.postScale(1f / resolution, -1f / resolution)
        mWorldToPixelMatrix.postTranslate(0f, mapHeight.toFloat())

        // 总变换 = outerMatrix * worldToPixel
        mTotalMatrix.set(outerMatrix)
        mTotalMatrix.preConcat(mWorldToPixelMatrix)

        // 传递给点云渲染器
        pointCloudView.setTransformMatrix(mTotalMatrix)

        // 同时更新基类矩阵（如果需要）
        setMatrixWithScale(outerMatrix, scale)

        invalidate()
    }

    // ---------- 绘制关键帧（Canvas） ----------
    // ---------- 优化后的 onDraw ----------
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!isDrawingEnabled || keyFrames3D.isEmpty()) return

        canvas.withMatrix(mTotalMatrix) {
            // 使用复用数组获取缩放值
            mTotalMatrix.getValues(mMatrixValues)
            val totalScale = Math.abs(mMatrixValues[Matrix.MSCALE_X])
            if (totalScale > 0) {
                mGreenDrawPaint.strokeWidth = 8f / totalScale
            }

            synchronized(keyFrames3D) {
                drawKeyFrameAngles(this, totalScale)
                drawKeyFrameIds(this)
            }
        }
    }

    // 优化角度计算：全程 Float，避免 Double 装箱
    private fun drawKeyFrameAngles(canvas: Canvas, totalScale: Float) {
        if (totalScale <= 0) return
        val inverseScale = 1f / totalScale
        for ((_, frame) in keyFrames3D) {
            canvas.withTranslation(frame.robotPos[0], frame.robotPos[1]) {
                // 直接用浮点数计算角度，不产生 Double
                val degrees = frame.robotPos[2] * (180f / Math.PI.toFloat())
                rotate(degrees)
                scale(inverseScale, inverseScale)
                drawPath(mArrowPath, mGreenDrawPaint)
            }
        }
    }

    // 使用缓存的字符串，避免重复 toString()
    private fun drawKeyFrameIds(canvas: Canvas) {
        canvas.withSave {
            mTotalMatrix.invert(mInverseMatrix)
            concat(mInverseMatrix)
            val textOffset = -(mTextPaint.descent() + mTextPaint.ascent()) / 2f + 10f

            for ((id, frame) in keyFrames3D) {
                mTempPts[0] = frame.robotPos[0]
                mTempPts[1] = frame.robotPos[1]
                mTotalMatrix.mapPoints(mTempPts)

                // 从缓存获取字符串，如果没有则创建并缓存
                val text = idStringCache.getOrPut(id) { id.toString() }
                drawText(text, mTempPts[0], mTempPts[1] + textOffset, mTextPaint)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        keyFrames3D.clear()
        mCachedPointCount.set(0)
        pointCloudView.clearDots()
    }
}