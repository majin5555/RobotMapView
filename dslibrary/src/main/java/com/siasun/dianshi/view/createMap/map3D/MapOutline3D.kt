//package com.siasun.dianshi.view.createMap.map3D
//
//import android.annotation.SuppressLint
//import android.content.Context
//import android.graphics.Canvas
//import android.graphics.Color
//import android.graphics.Paint
//import com.ngu.lcmtypes.laser_t
//import com.siasun.dianshi.bean.ConstraintNode
//import com.siasun.dianshi.view.SlamWareBaseView
//import java.lang.ref.WeakReference
//import java.util.concurrent.ConcurrentHashMap
//import java.util.concurrent.atomic.AtomicInteger
//import com.siasun.dianshi.bean.KeyFrame
//import com.siasun.dianshi.view.WorkMode
//import kotlin.math.cos
//import kotlin.math.sin
//import java.nio.ByteBuffer
//import java.nio.ByteOrder
//import java.nio.FloatBuffer
//
//import android.graphics.Matrix
//import android.util.Log
//
///**
// * 建图地图轮廓
// */
//@SuppressLint("ViewConstructor")
//class MapOutline3D(context: Context?, val parent: WeakReference<CreateMapView3D>) :
//    SlamWareBaseView<CreateMapView3D>(context, parent) {
//    private val TAG = this::class.java.simpleName
//    private var currentWorkMode = WorkMode.MODE_SHOW_MAP
//
//    //3D建图关键帧
//    private val keyFrames3D = ConcurrentHashMap<Int, KeyFrame>()
//
//    // 缓存点数，避免每次遍历计算
//    private val mCachedPointCount = AtomicInteger(0)
//
//    // 空间哈希集，用于过滤重复点云数据，网格大小 0.05m
//    private val mPointGridMap = ConcurrentHashMap<Long, Byte>()
//
//    // 双缓冲点云绘制数组，使用 DirectBuffer 移至 Native 内存，彻底避免 Java 堆内存溢出和抖动
//    private var mBufferA: FloatBuffer? = null
//    private var mBufferB: FloatBuffer? = null
//
//    @Volatile
//    private var mDrawingBuffer: FloatBuffer? = null
//
//    @Volatile
//    private var mDrawingLength = 0
//
//    // 用于 onDraw 中分块读取 DirectBuffer 并绘制的临时数组，避免在 onDraw 中分配内存
//    private val mDrawChunkSize = 65536 // 每次绘制 32768 个点
//    private val mDrawChunk = FloatArray(mDrawChunkSize)
//
//    private val mBuildLock = Any()
//
//    private val mWorldToPixelMatrix = Matrix()
//    private val mTotalMatrix = Matrix()
//
//    // 控制是否绘制
//    private var isDrawingEnabled: Boolean = false
//
//    /**
//     * 设置是否启用绘制
//     */
//    fun setDrawingEnabled(enabled: Boolean) {
//        this.isDrawingEnabled = enabled
//        postInvalidate()
//    }
//
//    /**
//     * 设置工作模式
//     */
//    fun setWorkMode(mode: WorkMode) {
//        if (currentWorkMode == mode) return // 避免重复设置
//
//        currentWorkMode = mode
//
//    }
//
//    /**
//     * 在后台重建渲染数组，双缓冲无锁刷新
//     */
//    private fun rebuildPointArray() {
//        synchronized(mBuildLock) {
//            val totalPointsCount = mCachedPointCount.get()
//            if (totalPointsCount <= 0) return
//
//            // 获取非绘制状态的 buffer
//            val backBuffer = if (mDrawingBuffer === mBufferA) mBufferB else mBufferA
//
//            var targetBuffer = backBuffer
//            val requiredCapacity = totalPointsCount * 2
//
//            if (targetBuffer == null || targetBuffer.capacity() < requiredCapacity) {
//                // 2. 分配 DirectBuffer 并填充，移至 Native 内存，防止内存溢出和抖动
//                // 扩容策略：按需容量的 1.5 倍分配，避免频繁分配导致内存抖动
//                val capacityFloats = (requiredCapacity * 1.5).toInt() + 10000
//                val buffer =
//                    ByteBuffer.allocateDirect(capacityFloats * 4).order(ByteOrder.nativeOrder())
//                targetBuffer = buffer.asFloatBuffer()
//
//                if (mDrawingBuffer === mBufferA) {
//                    mBufferB = targetBuffer
//                } else {
//                    mBufferA = targetBuffer
//                }
//            }
//
//            var index = 0
//            for (frame in keyFrames3D.values) {
//                val pts = frame.points
//                if (pts != null) {
//                    val size = pts.size
//                    // pts format: [cloudX, cloudY, x, y, ...]
//                    var i = 0
//                    while (i + 3 < size) {
//                        if (index + 1 < targetBuffer!!.capacity()) {
//                            // 使用绝对 put，避免修改 position 导致和 onDraw 发生竞态
//                            targetBuffer.put(index++, pts[i + 2])
//                            targetBuffer.put(index++, pts[i + 3])
//                        }
//                        i += 4
//                    }
//                }
//            }
//
//            mDrawingBuffer = targetBuffer
//            mDrawingLength = index
//        }
//    }
//
//    companion object {
//        val mPaint = Paint().apply {
//            color = Color.BLACK
//            style = Paint.Style.FILL
//            strokeWidth = 3f
//        }
//
//        val mGreenDrawPaint = Paint().apply {
//            color = Color.GREEN
//            style = Paint.Style.FILL
//            strokeWidth = 8f
//            strokeCap = Paint.Cap.ROUND
//            strokeJoin = Paint.Join.ROUND
//        }
//
//        val mTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
//            color = Color.BLACK
//            textSize = 10f
//            textAlign = Paint.Align.CENTER
//        }
//    }
//
//
//    @SuppressLint("DrawAllocation")
//    override fun onDraw(canvas: Canvas) {
//        super.onDraw(canvas)
//        canvas.save()
//        val mapView = parent.get() ?: return
//        if (keyFrames3D.isNotEmpty()) {
//            // 1. 构建世界坐标到地图像素坐标的变换矩阵
//            // 注意：必须在同步块中获取 mapData 数据
//            var resolution = 0.05f
//            synchronized(mapView.mSrf.mapData) {
//                val mapData = mapView.mSrf.mapData
//                resolution = mapData.resolution
//                if (resolution <= 0) resolution = 0.05f
//
//                // 构建 World -> Pixel 矩阵
//                // px = (wx - originX) / resolution
//                // py = height - (wy - originY) / resolution
//                mWorldToPixelMatrix.reset()
//                mWorldToPixelMatrix.postTranslate(-mapData.originX, -mapData.originY)
//                mWorldToPixelMatrix.postScale(1f / resolution, -1f / resolution)
//                mWorldToPixelMatrix.postTranslate(0f, mapData.height.toFloat())
//            }
//
//            // 2. 组合矩阵：Total = OuterMatrix * WorldToPixelMatrix
//            // 注意：Canvas的concat顺序是 preConcat，所以先 concat OuterMatrix (Pixel->Screen)，再 concat WorldToPixel (World->Pixel)
//            // 实际上 canvas.concat(M) 等价于 current = current * M.
//            // 我们希望 point * M_total -> screen.
//            // screen = Outer * Pixel
//            // Pixel = WorldToPixel * World
//            // screen = Outer * (WorldToPixel * World)
//            // 所以 M_total = Outer * WorldToPixel
//            mTotalMatrix.set(mapView.outerMatrix)
//            mTotalMatrix.preConcat(mWorldToPixelMatrix)
//
//            // 3. 应用矩阵到 Canvas
//            canvas.concat(mTotalMatrix)
//
//            // 4. 调整 Paint 线宽，抵消缩放影响，保持屏幕上固定像素大小
//            // 总缩放比例 approx = mapScale / resolution
//            val totalScale = mapView.mSrf.scale / resolution
//            if (totalScale > 0) {
//                mPaint.strokeWidth = 3f / totalScale
//                mGreenDrawPaint.strokeWidth = 8f / totalScale
//            }
//
//            // 5. 使用无锁双缓冲分块读取 DirectBuffer 点云数据，并绘制
//            val pointBuffer = mDrawingBuffer
//            val length = mDrawingLength
//
//            if (pointBuffer != null && length > 0) {
//                // 保存当前的 position 为 0
//                pointBuffer.position(0)
//                var remaining = length
//                while (remaining > 0) {
//                    val readSize = Math.min(remaining, mDrawChunkSize)
//                    pointBuffer.get(mDrawChunk, 0, readSize)
//                    canvas.drawPoints(mDrawChunk, 0, readSize, mPaint)
//                    remaining -= readSize
//                }
//            }
//
//            drawKeyFrame(canvas)
//
//            //控制关键帧
//            if (isDrawingEnabled) {
//                // 7. 绘制关键帧角度
//                drawKeyFrameAngles(canvas, totalScale)
//
//                // 8. 绘制关键帧ID
//                drawKeyFrameIds(canvas, mTotalMatrix)
//            }
//        }
//        canvas.restore()
//    }
//
//    // 缓存关键帧位置点，避免每次 onDraw 时分配数组
//    private var mKeyFramePointsArray = FloatArray(0)
//
//    private fun drawKeyFrame(canvas: Canvas) {
////        keyFrames3D.values.forEach { frame ->
////            // 使用局部变量减少重复计算
////            val mPoints = floatArrayOf(frame.robotPos[0], frame.robotPos[1])
////            canvas.drawPoints(mPoints, mGreenDrawPaint)
////        }
//
//// 方案二（可选）：如果想保留批量绘制，可以收集所有点后一次性绘制（但关键帧数量通常不多，方案一已足够）
//        if (keyFrames3D.isEmpty()) return
//
//        val currentSize = keyFrames3D.size
//        if (mKeyFramePointsArray.size < currentSize * 2) {
//            mKeyFramePointsArray = FloatArray(currentSize * 2 + 20) // 多预留一些空间
//        }
//
//        var idx = 0
//        for (frame in keyFrames3D.values) {
//            mKeyFramePointsArray[idx++] = frame.robotPos[0]
//            mKeyFramePointsArray[idx++] = frame.robotPos[1]
//        }
//        canvas.drawPoints(mKeyFramePointsArray, 0, idx, mGreenDrawPaint)
//    }
//
//    // 预分配对象，避免 onDraw 中频繁 GC 和对象创建
//    private val mInverseMatrix = Matrix()
//    private val mTempPts = FloatArray(2)
//
//    /**
//     * 绘制关键帧角度
//     */
//    private fun drawKeyFrameAngles(canvas: Canvas, totalScale: Float) {
//        if (totalScale <= 0) return
//        val inverseScale = 1f / totalScale
//
//        // 线段在屏幕上长度为5像素，转换到地图坐标系中
//        val lineLength = 10f * inverseScale
//        // 线段在屏幕上宽度设为2像素，转换到地图坐标系中
//        mGreenDrawPaint.strokeWidth = 3f * inverseScale
//
//        for (frame in keyFrames3D.values) {
//            canvas.save()
//            canvas.translate(frame.robotPos[0], frame.robotPos[1])
//            // frame.theta 为弧度，转换为角度（在翻转的Y轴坐标系中，正角度会自动逆时针旋转即向上）
//            canvas.rotate(Math.toDegrees(frame.robotPos[2].toDouble()).toFloat())
//            // 绘制线段，起点为关键帧中心点(0,0)，终点为起点加5像素(lineLength, 0)
//            canvas.drawLine(0f, 0f, lineLength, 0f, mGreenDrawPaint)
//            canvas.restore()
//        }
//
//        // 恢复原有线宽设置（屏幕上8像素），供下一帧 drawKeyFrame 使用
//        mGreenDrawPaint.strokeWidth = 8f / totalScale
//    }
//
//    // 缓存 id 到 String 的映射，避免频繁创建字符串对象
//    private val mKeyFrameIdStrings = ConcurrentHashMap<Int, String>()
//
//    /**
//     * 绘制关键帧ID，防重叠、防镜像，显示在正下方
//     */
//    private fun drawKeyFrameIds(canvas: Canvas, totalMatrix: Matrix) {
//        if (keyFrames3D.isEmpty()) return
//        canvas.save()
//
//        // 逆变换，使画布回到屏幕坐标系，从而保证文字大小恒定且不被镜像
//        totalMatrix.invert(mInverseMatrix)
//        canvas.concat(mInverseMatrix)
//
//        // 提前计算文字 Y 轴偏移量，避免在循环中重复计算
//        val textOffset = -(mTextPaint.descent() + mTextPaint.ascent()) / 2f + 10f
//
//        for (entry in keyFrames3D.entries) {
//            val id = entry.key
//            val frame = entry.value
//            // 使用 totalMatrix.mapPoints 替代 worldToScreen，避免在循环内分配 PointF 和数组对象
//            mTempPts[0] = frame.robotPos[0]
//            mTempPts[1] = frame.robotPos[1]
//            totalMatrix.mapPoints(mTempPts)
//
//            var text = mKeyFrameIdStrings[id]
//            if (text == null) {
//                text = id.toString()
//                mKeyFrameIdStrings[id] = text
//            }
//            canvas.drawText(text, mTempPts[0], mTempPts[1] + textOffset, mTextPaint)
//        }
//
//        canvas.restore()
//    }
//
//    /**
//     * 判断是否已包含该关键帧
//     */
//    fun hasKeyFrame(rad0: Int): Boolean {
//        return keyFrames3D.containsKey(rad0)
//    }
//
//    /**
//     * 空间哈希过滤：判断并记录点云是否已存在于网格中
//     * @return true 表示已存在（重复数据），应当过滤掉
//     */
//    fun filterPoint(worldX: Float, worldY: Float): Boolean {
//        // 使用 0.05m (5cm) 作为网格分辨率 (1 / 0.05 = 20)
//        val gridX = (worldX * 20f).toLong()
//        val gridY = (worldY * 20f).toLong()
//        val key = (gridX shl 32) or (gridY and 0xFFFFFFFFL)
//        // 如果 put 返回 null，说明是新点；如果返回 1，说明该网格已存在点
//        return mPointGridMap.put(key, 1) != null
//    }
//
//    /***
//     * 添加关键帧
//     */
//    fun addKeyFrames(laserData: laser_t, keyPoints: FloatArray?) {
//        val mapView = parent.get() ?: return
//        val rad0 = laserData.rad0.toInt()
//        // rad0等于0的时候添加，其余的时候隔3帧添加(即0, 4, 8...)
//        if (rad0 != -1 && (rad0 == 0 || rad0 % 4 == 0)) {
//            if (!keyFrames3D.containsKey(rad0)) {
////            Log.i(TAG, "关键帧 ID $rad0")
////            Log.i(TAG, "关键帧 x ${robotPose[0]}")
////            Log.i(TAG, "关键帧 y ${robotPose[1]}")
////            Log.i(TAG, "关键帧 t ${robotPose[2]}")
////            Log.i(TAG, "关键帧 z ${robotPose[3]}")
////            Log.i(TAG, "关键帧 roll ${robotPose[4]}")
////            Log.i(TAG, "关键帧 pitch ${robotPose[5]}")
//                //关键帧第一帧 要单独显示
//                if (rad0 == 0) {
//                    mapView.mConstrainNodes?.addConstraintNodes(
//                        ConstraintNode(
//                            rad0,
//                            mapView.robotPose[0].toDouble(),
//                            mapView.robotPose[1].toDouble(),
//                            mapView.robotPose[2].toDouble()
//                        )
//                    )
//                }
//
//                synchronized(keyFrames3D) {
//                    // 累加点数缓存
//                    keyPoints?.size?.let { count ->
//                        if (count > 0) {
//                            mCachedPointCount.addAndGet(count / 4)
//                        }
//                    }
//
//                    keyFrames3D[rad0] = KeyFrame(keyPoints, mapView.robotPose.clone())
//                    mapView.isStartRevSubMaps = true
//                }
//
//                // 在后台触发点云重建
//                rebuildPointArray()
//            }
//        }
//    }
//
//    /**
//     * 外部接口：更新关键帧数据 nav做回环检测 3D
//     */
//    var rangeSize = 0
//    fun parseOptPose(laserData: laser_t) {
//        Log.e(TAG, "3D回环检测关键帧keyFrames3D  ${keyFrames3D.size}")
//
//        if (laserData.ranges.isEmpty()) return
//        if (rangeSize == laserData.ranges.size) return
//        else {
//            rangeSize = laserData.ranges.size
//            Log.d(TAG, "3D回环检测开始 ${laserData.ranges.size / 4}")
//            // 标记脏数据，需要重绘
//            var hasUpdate = false
//
//            // 这里不需要 synchronized(keyFrames3D)，因为 ConcurrentHashMap 支持并发遍历和修改值
//            val size = laserData.ranges.size
//            for (i in 0 until size step 4) {
//                // 关键帧ID
//                val rad0: Int = laserData.ranges[i].toInt()
//
//                // 关键帧位置
//                val radX: Float = laserData.ranges[i + 1]
//                val radY: Float = laserData.ranges[i + 2]
//                val robotTheta: Float = laserData.ranges[i + 3]
//
//                // 获取关键帧数据（非空校验）
//                val keyFrame = keyFrames3D[rad0] ?: continue
//
//                // 更新机器人位置（原子操作，避免中间状态）
//                keyFrame.robotPos[0] = radX
//                keyFrame.robotPos[1] = radY
//                keyFrame.robotPos[2] = robotTheta
//
//                // 优化点：批量更新点云坐标（使用数学运算优化）
//                val cosT = cos(robotTheta)
//                val sinT = sin(robotTheta)
//
//                // 仅更新当前关键帧的点云（避免遍历所有关键帧）
//                val pts = keyFrame.points
//                if (pts != null) {
//                    val sizePts = pts.size
//                    var j = 0
//                    while (j + 3 < sizePts) {
//                        val cloudX = pts[j]
//                        val cloudY = pts[j + 1]
//                        // 复用预计算的三角函数值
//                        pts[j + 2] = cloudX * cosT - cloudY * sinT + radX
//                        pts[j + 3] = cloudX * sinT + cloudY * cosT + radY
//                        j += 4
//                    }
//                }
//
//                hasUpdate = true
//            }
//            if (hasUpdate) {
//                // 在后台触发点云重建
//                rebuildPointArray()
//                postInvalidate()
//            }
//            Log.d(TAG, "3D回环检测结束 更新关键帧数据：处理 ${laserData.ranges.size / 4} 个关键帧")
//        }
//    }
//
//    /**
//     * 清理资源，防止内存泄漏
//     */
//    override fun onDetachedFromWindow() {
//        super.onDetachedFromWindow()
//        // 清理点云数据
//        keyFrames3D.clear()
//        mKeyFrameIdStrings.clear()
//        mPointGridMap.clear()
//        mCachedPointCount.set(0)
//
//        // 释放 DirectBuffer 引用
//        mBufferA = null
//        mBufferB = null
//        mDrawingBuffer = null
//
//        // 清理父引用
//        parent.clear()
//    }
//}
