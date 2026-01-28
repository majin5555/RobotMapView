package com.siasun.dianshi.createMap2D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import android.os.SystemClock
import android.util.AttributeSet
import android.util.Log
import android.view.Choreographer
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.R
import com.siasun.dianshi.bean.createMap2d.MapEditorConstants
import com.siasun.dianshi.bean.createMap2d.SubMapData
import com.siasun.dianshi.utils.CoordinateConversion
import com.siasun.dianshi.utils.DrawGraphicsNew
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.math3.linear.Array2DRowRealMatrix
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.text.DecimalFormat
import java.util.Collections
import java.util.LinkedList
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.text.toDouble

/**
 *   地图显示与编辑View（支持2D地图显示 ）
 *   工作模式：
 * - 创建地图模式（WORK_MODE_CREATE_MAP）
 * - 显示地图模式（WORK_MODE_SHOW_MAP）
 * - 扩展地图模式（包括预处理和进行中状态）
 * - 虚拟墙模式
 */
class CreateMapView2D : SurfaceView, SurfaceHolder.Callback {
    // 工作模式定义
    companion object {
        const val MODE_SHOW_MAP = 1      // 显示PNG地图模式
        const val MODE_CREATE_MAP2D = 17// 2D建图
        const val MODE_EXPAND_BEFORE2D = 18 // 2D扩展地图预处理模式
        const val MODE_EXPAND_ING2D = 19    // 2D扩展地图进行中模式
    }

    // ======================================
    // 成员变量（按功能模块分组）
    // ======================================
    // 工作模式相关
    private var currentMode = -1

    // 视图渲染相关
    private var surfaceHolder: SurfaceHolder? = null
    private val mDrawGraphicsNew = DrawGraphicsNew()//绘制工具类

    private var mSrf = CoordinateConversion()//坐标转化工具类


    // 机器人相关
    private val robotBitmap: Bitmap? by lazy {
        BitmapFactory.decodeResource(resources, R.mipmap.current_location)
    }

    val robotPose = FloatArray(6) // [x, y, theta(rad),z roll pitch]
    var isMapping = false//是否建图标志
    var isRouteMap = false//是否可以旋转地图

    //是否第一次接收到子图数据，如果没收到子图，直接跳过旋转环境
    var isStartRevSubMaps = false

    //旋转角度
    var mRotateAngle = 0f

    private val keyFrames2d = ConcurrentHashMap<Int, SubMapData>() //绘制地图的数据 建图时 2D
    private val upPointsCloudList = Collections.synchronizedList(mutableListOf<PointF>())//上激光点云
    private val downPointsCloudList = Collections.synchronizedList(mutableListOf<PointF>())//下激光点云

    // 触摸交互相关
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var lastTwoFingerDistance = 0f

    // 坐标转换监听器
    private var worldCoordinateListener: ((PointF) -> Unit)? = null


    // 绘制控制
    private var isRendering = false
    private val frameHandler = Choreographer.getInstance()
    private val frameRenderCallback = Choreographer.FrameCallback { drawAndScheduleNextFrame() }


    // ======================================
    // 构造函数与初始化
    // ======================================
    constructor(context: Context?) : super(context) {
        initView()
    }

    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs) {
        initView()
    }

    private fun initView() {
        surfaceHolder = holder
        surfaceHolder?.addCallback(this)
        isFocusable = false
        setFocusableInTouchMode(false)
        setZOrderOnTop(false) // 如果不需要覆盖RecyclerView

    }

    // ======================================
    // 工作模式设置
    // ======================================
    fun setWorkMode(mode: Int) {
        currentMode = mode
        when (mode) {
            MODE_CREATE_MAP2D -> cleanMappingData() // 创建地图模式
        }
    }

    // ======================================
    // 内部接口
    // ======================================

    /**
     *
     * 清除建图数据（关键帧、点云等）
     */
    private fun cleanMappingData() {
        keyFrames2d.clear()
        upPointsCloudList.clear()
//        mapPathList.clear()
        resetRenderParams()
    }

    /**
     * 更新机器人位置（弧度制）
     */
    private fun updateRobotPose(
        x: Float, y: Float, theta: Float, z: Float = 0f, roll: Float = 0f, pitch: Float = 0f
    ) {
        // 使用辅助方法将可能是科学计数法的float值转换为正常的float值
        robotPose[0] = convertScientificToDecimal(x)
        robotPose[1] = convertScientificToDecimal(y)
        robotPose[2] = convertScientificToDecimal(theta)
        robotPose[3] = convertScientificToDecimal(z)
        robotPose[4] = convertScientificToDecimal(roll)
        robotPose[5] = convertScientificToDecimal(pitch)
    }

    /**
     * 辅助方法：将科学计数法表示的float值转换为普通小数表示的float值
     * 解决激光数据中theta值（laserData.ranges[2]）可能以科学计数法形式存在的问题
     */
    private val df = DecimalFormat("0.000") // 固定小数点后3位
    private fun convertScientificToDecimal(value: Float): Float {
        df.setRoundingMode(RoundingMode.HALF_UP) // 设置四舍五入
        return df.format(value).toFloat()
    }

    // ======================================
    // 触摸事件处理
    // ======================================
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.pointerCount) {
            1 -> handleSingleTouch(event)
            2 -> handleMultiTouch(event)
        }
        return true
    }

    private val ptDown = PointF()
    private val ptMove = PointF()

    private val intervalTime: Long = 500
    private var lastClickTime = 0L

    @OptIn(DelicateCoroutinesApi::class)
    private fun handleSingleTouch(event: MotionEvent) {
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val currentTime = SystemClock.elapsedRealtime()
                if (currentTime - lastClickTime > intervalTime) {
                    lastClickTime = currentTime
                    ptDown.x = event.x
                    ptDown.y = event.y
                    lastTouchX = event.x
                    lastTouchY = event.y
                }
            }

            MotionEvent.ACTION_MOVE -> {
                ptMove.x = event.x
                ptMove.y = event.y

                //当前模式
                if (currentMode !in setOf(
                        MODE_EXPAND_BEFORE2D,
                    )
                ) {
                    val dx = event.x - lastTouchX
                    val dy = event.y - lastTouchY
//                    mSrf.offsetX += dx
//                    mSrf.offsetY += dy
                }

                if (currentMode == MODE_EXPAND_BEFORE2D) {//区域更新
//                    mRegionEdit.moveActionPartialUpdatesArea(ptMove, mSrf)
                } else if (currentMode == MODE_CREATE_MAP2D || currentMode == MODE_EXPAND_ING2D) {//2D建图计算偏移量 单指移动
                    updateKeyFrame2d()
                }

                lastTouchX = event.x
                lastTouchY = event.y
                // 触发坐标转换回调
                GlobalScope.launch(Dispatchers.Default) {
                    val worldPoint = mSrf.screenToWorld(event.x, event.y)
                    withContext(Dispatchers.Main) {
                        worldCoordinateListener?.invoke(worldPoint)
                    }
                }
            }

            MotionEvent.ACTION_UP -> {

            }
        }
    }

    private fun handleMultiTouch(event: MotionEvent) {
        val x0 = event.getX(0)
        val y0 = event.getY(0)
        val x1 = event.getX(1)
        val y1 = event.getY(1)
        val currentDistance = sqrt((x1 - x0).pow(2) + (y1 - y0).pow(2))

        when (event.actionMasked) {
            MotionEvent.ACTION_POINTER_DOWN -> {
                lastTwoFingerDistance = currentDistance
            }

            MotionEvent.ACTION_MOVE -> {
                if (lastTwoFingerDistance > 0) {
                    val scaleDelta = currentDistance / lastTwoFingerDistance
                    val newScale = (mSrf.scale * scaleDelta).coerceIn(0.2f, 5f)
                    val focusX = (x0 + x1) / 2f
                    val focusY = (y0 + y1) / 2f

//                    // 保持缩放中心点不变
//                    mSrf.offsetX = (mSrf.offsetX - focusX) * (newScale / mSrf.scale) + focusX
//                    mSrf.offsetY = (mSrf.offsetY - focusY) * (newScale / mSrf.scale) + focusY
                    mSrf.scale = newScale
                    lastTwoFingerDistance = currentDistance

                    if (currentMode == MODE_CREATE_MAP2D || currentMode == MODE_EXPAND_ING2D) {//2D建图计算缩放比例 手指缩放
                        updateKeyFrame2d()
                    }
                }
            }
        }
    }

    // ======================================
    // 绘制相关方法
    // ======================================
    override fun surfaceCreated(holder: SurfaceHolder) {
        isRendering = true
        frameHandler.postFrameCallback(frameRenderCallback)
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isRendering = false
        frameHandler.removeFrameCallback(frameRenderCallback)
    }

    // 定义间隔值，这里假设间隔为 2
    var interval = 2f

    private var lastDrawTime = 0L
    private val MIN_FRAME_INTERVAL = 16 // 约60fps
    private fun drawAndScheduleNextFrame() {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastDrawTime < MIN_FRAME_INTERVAL) {
            // 跳过本次绘制，保持帧率稳定
            if (isRendering) frameHandler.postFrameCallback(frameRenderCallback)
            return
        }
        lastDrawTime = currentTime
        val holder = surfaceHolder ?: return
        var canvas: Canvas? = null
        try {
            canvas = holder.lockCanvas()
            canvas?.let { it ->
                mDrawGraphicsNew.mCanvas = it
                mDrawGraphicsNew.setCoordinateConversion(mSrf)
                it.drawColor(Color.WHITE)


                when (currentMode) {
                    MODE_CREATE_MAP2D -> {
                        synchronized(keyFrames2d) {
                            canvas.save()
                            // 应用全局旋转（如果有）
                            if (mRotateAngle != 0f) {
                                canvas.rotate(-mRotateAngle, width / 2f, height / 2f)
                            }
                            mDrawGraphicsNew.drawKeyFrames2d(keyFrames2d)
                        }
                    }

                    //区域更新中2D
                    MODE_EXPAND_ING2D -> {
//                        //区域更新中绘制规定的区域
//                        partialUpdateList.forEach {
//                            mDrawGraphicsNew.drawPartialUpdateArea(
//                                it, 2
//                            )
//                        }
//                        //区域更新中绘制导航返回的子图
//                        synchronized(keyFrames2d) {
//                            mDrawGraphicsNew.drawKeyFrames2d(keyFrames2d)
//                        }
                    }
                }
                drawCommon(canvas)
            }
        } finally {
            holder.unlockCanvasAndPost(canvas) // 确保画布解锁
        }
        if (isRendering) frameHandler.postFrameCallback(frameRenderCallback)
    }

    /**
     * 绘制公共的
     */
    private fun drawCommon(canvas: Canvas) {

        mDrawGraphicsNew.drawCurrentPointCloud(upPointsCloudList)

        mDrawGraphicsNew.drawDownCurrentPointCloud(
            downPointsCloudList
        )

        mDrawGraphicsNew.drawRobot(robotBitmap, robotPose)

        //绘制机器人位置 //恢复旋转（只有在建图、扩展地图时使用）
        if (currentMode == MODE_CREATE_MAP2D) canvas.restore()
    }


    /**
     * 绘制扩展地图
     */
    private fun drawExpandMode2D(canvas: Canvas) {
//        mDrawGraphicsNew.drawKeyFrames(keyFrames, mRotateAngle, width, height)
//        mDrawGraphicsNew.drawMapPathPoints(mapPathList)
//        canvas.restore() // 恢复为未旋转状态
    }


    // ======================================
    // 辅助工具方法
    // ======================================

    private fun resetRenderParams() {
        mSrf.scale = 1f
//        mSrf.offsetX = 0f
//        mSrf.offsetY = 0f
        robotPose.fill(0f)
        isRouteMap = false
        isMapping = false
    }


    // ======================================
    // 外部接口（数据更新）
    // ======================================


    /**
     * 外部接口：更新定位时上激光点云
     */
    private val pointPool = ObjectPool<PointF>({ PointF() }, 10) // 预创建 10 个 PointF

    // 更新点云数据时复用对象
    fun setCurPointCloud(laserData: laser_t) {
        updateRobotPose(laserData.ranges[0], laserData.ranges[1], laserData.ranges[2])
        synchronized(upPointsCloudList) {
            upPointsCloudList.clear()
            if (laserData.ranges.size > 3) {
                for (i in 1 until laserData.ranges.size / 3) {
                    val x = laserData.ranges[3 * i]
                    val y = laserData.ranges[3 * i + 1]
                    val point = pointPool.acquire() // 从对象池获取
                    point.set(
                        x * cos(robotPose[2]) - y * sin(robotPose[2]) + robotPose[0],
                        x * sin(robotPose[2]) + y * cos(robotPose[2]) + robotPose[1]
                    )
                    upPointsCloudList.add(point)
                }
            }
        }
    }

    // 对象池实现
    class ObjectPool<T>(private val factory: () -> T, private val initialSize: Int) {
        private val pool = LinkedList<T>().apply { repeat(initialSize) { add(factory()) } }
        fun acquire(): T = pool.poll() ?: factory()
        fun release(obj: T) {
            pool.offer(obj)
        }
    }


    /**
     * 外部接口：更新定位时下激光点云
     */
    @SuppressLint("SuspiciousIndentation")
    fun setDownCurPointCloud(laserData: laser_t) {
        val robotX: Float = laserData.ranges[0]
        val robotY: Float = laserData.ranges[1]
        val theta: Float = laserData.ranges[2]
        downPointsCloudList.clear()
        if (laserData.ranges.isNotEmpty()) {
            for (i in 1 until laserData.ranges.size / 3) {
                val laserX: Float = laserData.ranges[3 * i]
                val laserY: Float = laserData.ranges[3 * i + 1]

                val laserXNew = (laserX * cos(theta) - laserY * sin(theta) + robotX)
                val laserYNew = (laserX * sin(theta) + laserY * cos(theta) + robotY)

                downPointsCloudList.add(PointF(laserXNew, laserYNew))
            }
        }
    }

    // ======================================
    // 数据解析方法（可根据业务需求拆分）
    // ======================================

    /**
     * 外部接口：更新子图数据 2D
     */
    fun parseSubMaps2D(mLaserT: laser_t, type: Int) {
        synchronized(keyFrames2d) {
            val subMapData = SubMapData()
            //子图ID
            subMapData.id = mLaserT.rad0.toInt()
            Log.i("SLAMMapView2D", "子图 radID  子图索引 ${subMapData.id}")

            //子图 x方向格子数量 子图宽度
            subMapData.width = mLaserT.ranges[0]
            Log.i(
                "SLAMMapView2D", "子图 mLaserT.ranges[0] x方向格子数量 子图宽度 ${subMapData.width}"
            )
            //子图 y方向格子数量 子图高度
            subMapData.height = mLaserT.ranges[1]
            Log.i(
                "SLAMMapView2D",
                "子图 mLaserT.ranges[1] y方向格子数量 子图高度 ${subMapData.height}"
            )

            //新增读取子图右上角世界坐标
            subMapData.originX = mLaserT.ranges[2]
            Log.i(
                "SLAMMapView2D",
                "子图 mLaserT.ranges[2] 新增读取子图右上角世界坐标 originX ${subMapData.originX}"
            )
            subMapData.originY = mLaserT.ranges[3]
            Log.i(
                "SLAMMapView2D",
                "子图 mLaserT.ranges[3] 新增读取子图右上角世界坐标 originY ${subMapData.originY}"
            )
            subMapData.originTheta = mLaserT.ranges[4]
            Log.i(
                "SLAMMapView2D",
                "子图 mLaserT.ranges[4] 新增读取子图右上角世界坐标 originTheta ${subMapData.originTheta}"
            )
            subMapData.optMaxTempX = mLaserT.ranges[5]
            Log.i("SLAMMapView2D", "子图 mLaserT.ranges[5] optMaxTempX  ${subMapData.optMaxTempX}")
            subMapData.optMaxTempY = mLaserT.ranges[6]
            Log.i("SLAMMapView2D", "子图 mLaserT.ranges[6] optMaxTempY  ${subMapData.optMaxTempY}")
            subMapData.optMaxTempTheta = mLaserT.ranges[7]
            Log.i(
                "SLAMMapView2D",
                "子图 mLaserT.ranges[7] optMaxTempXTheta   ${subMapData.optMaxTempTheta}"
            )

            // 各格子概率值
            subMapData.indexCount = mLaserT.intensities.size
            //所有概率点的集合
            for (intensity in mLaserT.intensities) {
                subMapData.intensitiesList.add(intensity.toInt())
            }

            //创建子图bitmap对象
            buildSubMapTileLine(subMapData)

            //右上角角物理坐标   (世界坐标系)
            subMapData.rightTop.x = subMapData.originX
            subMapData.rightTop.y = subMapData.originY

            //右下角角物理坐标   (世界坐标系)
            subMapData.rightBottom.x = subMapData.originX
            subMapData.rightBottom.y = subMapData.originY - (subMapData.percent * subMapData.height)

            //左上角物理坐标  (世界坐标系)
            subMapData.leftTop.x = subMapData.rightTop.x - (subMapData.percent * subMapData.width)
            subMapData.leftTop.y = subMapData.originY

            //左下角坐标 (世界坐标系)
            subMapData.leftBottom.x =
                subMapData.rightTop.x - (subMapData.percent * subMapData.width)
            subMapData.leftBottom.y = subMapData.originY - (subMapData.percent * subMapData.height)


            //扩展时
            if (type == 1) {

            } else {
                //新建地图时
                calBinding()
            }


            val mTempMatrix = Matrix()
            mTempMatrix.reset()
            mTempMatrix.setScale(mSrf.scale, mSrf.scale)

            // 计算子图在屏幕上的左上角坐标
            val screenLeftTop = mSrf.worldToScreen(subMapData.leftTop.x, subMapData.leftTop.y)
            mTempMatrix.postTranslate(screenLeftTop.x, screenLeftTop.y)
            subMapData.matrix = mTempMatrix

            keyFrames2d[subMapData.id] = subMapData

            isRouteMap = true
            isStartRevSubMaps = true

            Log.e(
                "SLAMMapView2D", "整张 地图的信息  keyFrames2d keyFrames2d.size ${keyFrames2d.size}"
            )
            Log.w("SLAMMapView2D", "整张 地图的信息  mSrf.mapData ${mSrf.mapData}")
        }
    }

    /**
     * 回环检测2D
     * 输入数据 世界坐标系下的位姿态
     */
    fun updateOptPose2D(mLaserT: laser_t, type: Int) {
//        LogUtil.w("回环检测2D  start")
        val optPose = mLaserT.ranges
//        LogUtil.w("回环检测optPose.size  ${optPose.size}")

        val IDList: MutableList<Int> = mutableListOf()
        //   按采样间隔遍历数据（步长为4*SAMPLE_INTERVAL，每个关键帧占4个Float）
        for (i in optPose.indices step 4) {
            val id = optPose[i].toInt()
            IDList.add(id)
//            LogUtil.w("回环检测id  $id")
            val globalX = optPose[i + 1]
//            LogUtil.w("回环检测globalX  $globalX")
            val globalY = optPose[i + 2]
//            LogUtil.w("回环检测globalY  $globalY")
            val globalTheta = optPose[i + 3]
//            LogUtil.w("回环检测globalT  $globalTheta")

            //关键帧
            // 获取关键帧数据（非空校验）
            val subMapData = keyFrames2d[id] ?: continue

//            LogUtil.d("第 $id 张子图  $subMapData")

            // Extract local pose
            val (localX, localY, localTheta) = Triple(
                subMapData.optMaxTempX.toDouble(),
                subMapData.optMaxTempY.toDouble(),
                subMapData.originTheta.toDouble()
            )

            // Build global transformation matrix [cos(θg) -sin(θg) xg; sin(θg) cos(θg) yg; 0 0 1]
            val globalMatrix = Array2DRowRealMatrix(
                arrayOf(
                    doubleArrayOf(
                        cos(globalTheta).toDouble(),
                        (-sin(globalTheta)).toDouble(),
                        globalX.toDouble()
                    ), doubleArrayOf(
                        sin(globalTheta).toDouble(), cos(globalTheta).toDouble(), globalY.toDouble()
                    ), doubleArrayOf(0.0, 0.0, 1.0)
                )
            )

            // Build local transformation matrix [cos(θl) -sin(θl) xl; sin(θl) cos(θl) yl; 0 0 1]
            val localMatrix = Array2DRowRealMatrix(
                arrayOf(
                    doubleArrayOf(cos(localTheta), -sin(localTheta), localX),
                    doubleArrayOf(sin(localTheta), cos(localTheta), localY),
                    doubleArrayOf(0.0, 0.0, 1.0)
                )
            )

            // Compute final transformation: T_global * T_local
            val resultMatrix = globalMatrix.multiply(localMatrix).data

            // Update submap metadata with global origin and orientation
            subMapData.originX = resultMatrix[0][2].toFloat()
            subMapData.originY = resultMatrix[1][2].toFloat()
            subMapData.originTheta = globalTheta
        }

        updateKeyFrame2d()
        //扩展时
        if (type == 1) {
        } else {
            //新建地图时
            calBinding()
        }
//        LogUtil.w("回环检测2D  end")
    }

    private fun updateKeyFrame2d() {
        // 优化updateKeyFrame2d方法中的矩阵更新逻辑
        for ((matrixKey, mSubMapData) in keyFrames2d.entries) {
            val bitmap = mSubMapData.mBitmap ?: continue

            // 计算屏幕坐标
            val screenLeftTop = mSrf.worldToScreen(mSubMapData.leftTop.x, mSubMapData.leftTop.y)
            val screenRightBottom =
                mSrf.worldToScreen(mSubMapData.rightBottom.x, mSubMapData.rightBottom.y)

            // 计算目标尺寸
            val targetWidth = screenRightBottom.x - screenLeftTop.x
            val targetHeight = screenRightBottom.y - screenLeftTop.y

            // 计算等比缩放
            val originalWidth = bitmap.width.toFloat()
            val originalHeight = bitmap.height.toFloat()
            val scale = min(targetWidth / originalWidth, targetHeight / originalHeight)

            // 创建新矩阵，包含缩放和平移
            mSubMapData.matrix!!.reset()
            mSubMapData.matrix!!.setScale(scale, scale)
            mSubMapData.matrix!!.postTranslate(screenLeftTop.x, screenLeftTop.y)
        }
    }

    /**
     * 创建子图bitmap对象
     */

    private fun buildSubMapTileLine(metaData: SubMapData) {

        val bmpData =
            ByteArray((metaData.width * metaData.height * MapEditorConstants.MAP_PIXEL_SIZE).toInt())
        for (i in 0 until metaData.indexCount) {
            val index: Int = metaData.intensitiesList[i]
            val color = 0
            bmpData[index * MapEditorConstants.MAP_PIXEL_SIZE] = (color and 0x000000FF).toByte()
            bmpData[index * MapEditorConstants.MAP_PIXEL_SIZE + 1] = (color and 0x000000FF).toByte()
            bmpData[index * MapEditorConstants.MAP_PIXEL_SIZE + 2] = (color and 0x000000FF).toByte()
            bmpData[index * MapEditorConstants.MAP_PIXEL_SIZE + 3] = (-0x10000 shr 24).toByte()
        }

        val bitmap = Bitmap.createBitmap(
            (metaData.width).toInt(), (metaData.height).toInt(), Bitmap.Config.ARGB_8888
        )

        bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(bmpData))
        metaData.mBitmap = bitmap
        Log.i("SLAMMapView2D", "子图ID ${metaData.id}  宽 ${bitmap.width} 高 ${bitmap.height}")
    }

    /**
     * 1. 计算所有子图集合的右上角和左下角
     * 2. 计算所有子图集合的长度和宽度
     */
    private var maxTopRight = PointF(-10.0f, -10.0f) // 右上
    private var minBotLeft = PointF(10.0f, 10.0f) // 左下

    private var minTopLeft = PointF(10.0f, 10.0f) // 左上 - 初始化为极大值
    private var maxBottomRight = PointF(-10.0f, -10.0f) // 右下 - 初始化为极小值

    private fun calBinding() {

        for (item in keyFrames2d) {
            val subCreateMap = item.value

            // 更新右上角坐标 - 取所有子图中的最大值
            if (subCreateMap.originX > maxTopRight.x) {
                maxTopRight.x = subCreateMap.originX
            }

            // 更新左下角坐标 - 取所有子图中的最小值
            if (subCreateMap.leftBottom.x < minBotLeft.x) {
                minBotLeft.x = subCreateMap.leftBottom.x
            }

            if (subCreateMap.leftBottom.y < minBotLeft.y) {
                minBotLeft.y = subCreateMap.leftBottom.y
            }

            // 更新左上角坐标 - 取所有子图中的最小值
            if (subCreateMap.leftTop.x < minTopLeft.x) {
                minTopLeft.x = subCreateMap.leftTop.x
            }

            if (subCreateMap.leftTop.y > minTopLeft.y) {
                minTopLeft.y = subCreateMap.leftTop.y
            }

            // 更新右下角坐标 - 取所有子图中的最大值
            if (subCreateMap.rightBottom.x > maxBottomRight.x) {
                maxBottomRight.x = subCreateMap.rightBottom.x
            }

            if (subCreateMap.rightBottom.y < maxBottomRight.y) {
                maxBottomRight.y = subCreateMap.rightBottom.y
            }
        }


        // 计算整张地图的宽度和高度
//        mSrf.mapData.mWidth = abs((maxTopRight.x - minBotLeft.x) / 0.05f)
//        mSrf.mapData.mHeight = abs((maxTopRight.y - minBotLeft.y) / 0.05f)


//        Log.i("SLAMMapView2D","左上 ${minTopLeft}")
//        Log.i("SLAMMapView2D","左下 ${minBotLeft}")
//        Log.i("SLAMMapView2D","右上 ${maxTopRight}")
//        Log.i("SLAMMapView2D","右下 ${maxBottomRight}")
//        Log.i("SLAMMapView2D","整张地图的宽度 ${mSrf.mapData.mWidth}")
//        Log.i("SLAMMapView2D","整张地图的高度 ${mSrf.mapData.mHeight}")
    }

    /**
     * 解析激光点云数据（建图模式） 2D
     */
    fun parseLaserData2D(laserData: laser_t) {
        // 更新机器人位置（始终需要处理，不参与降采样）
        updateRobotPose(laserData.ranges[0], laserData.ranges[1], laserData.ranges[2])

        upPointsCloudList.clear()
        if (laserData.ranges.size <= 3) return // 最少包含机器人位置数据

        // 动态计算采样间隔（根据数据量和缩放比例）
        val totalPoints = (laserData.ranges.size - 3) / 3 // 总激光点数（排除机器人位置）
        val baseSampleInterval = when {
            totalPoints > 600 -> 10  // 数据量极大时，间隔10
            totalPoints > 400 -> 5  // 数据量较大时，间隔5
            else -> 2  // 数据量较小时，间隔2
        }
        val dynamicSampleInterval =
            maxOf(baseSampleInterval, (1f / mSrf.scale).toInt()) // 缩放越小，间隔越大


        // 遍历激光点，按采样间隔降采样
        for (i in 0 until totalPoints step dynamicSampleInterval) {
            val index = 3 + i * 3 // 跳过机器人位置数据（前3个元素）
            if (index + 2 >= laserData.ranges.size) break // 越界保护

            val laserX = laserData.ranges[index]
            val laserY = laserData.ranges[index + 1]

            // 坐标变换（仅计算有效点）
            val cosT = cos(robotPose[2])
            val sinT = sin(robotPose[2])
            val laserXNew = laserX * cosT - laserY * sinT + robotPose[0]
            val laserYNew = laserX * sinT + laserY * cosT + robotPose[1]

            synchronized(upPointsCloudList) {
                upPointsCloudList.add(PointF(laserXNew, laserYNew))
            }
        }
    }


    // ======================================
    // 监听器设置
    // ======================================
    fun setOnWorldCoordinateListener(listener: (PointF) -> Unit) {
        worldCoordinateListener = listener
    }

}

