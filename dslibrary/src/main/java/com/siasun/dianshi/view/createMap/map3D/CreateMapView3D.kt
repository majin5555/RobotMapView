package com.siasun.dianshi.view.createMap.map3D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.opengl.GLSurfaceView
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.R
import com.siasun.dianshi.bean.ConstraintNode
import com.siasun.dianshi.bean.MapData
import com.siasun.dianshi.utils.CoordinateConversion
import com.siasun.dianshi.utils.MathUtils
import com.siasun.dianshi.utils.RadianUtil
import com.siasun.dianshi.utils.SlamGestureDetector
import com.siasun.dianshi.utils.YamlNew
import com.siasun.dianshi.view.PngMapView
import com.siasun.dianshi.view.RobotView
import com.siasun.dianshi.view.SlamWareBaseView
import com.siasun.dianshi.view.UpLaserScanView
import com.siasun.dianshi.view.WorkMode
import com.siasun.dianshi.view.createMap.ExpandAreaView
import com.siasun.dianshi.view.createMap.MapViewInterface
import com.siasun.dianshi.view.createMap.RobotViewCreateMap
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.atan2

/**
 * 地图画布（3D 建图 View）
 * 基于 SurfaceView + Flow 按需刷新，使用 throttleLatest 限制绘制频率并保证最后帧不丢失。
 */
open class CreateMapView3D(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs),
    SlamGestureDetector.OnRPGestureListener, MapViewInterface, SurfaceHolder.Callback {

    private val TAG = this::class.java.simpleName

    // 当前工作模式
    private var currentWorkMode = WorkMode.MODE_SHOW_MAP

    var mSrf = CoordinateConversion()//坐标转化工具类
    private var mOuterMatrix = Matrix()
    private var VIEW_WIDTH = 0 //视图宽度
    private var VIEW_HEIGHT = 0

    //视图高度
    private var mMapScale = 1f //地图缩放级别
    private val mMaxMapScale = 5f //最大缩放级别
    private var mMinMapScale = 0.1f //最小缩放级别

    // 初始地图参数（用于计算扩展地图时的偏移量）
    private var initialOriginX = 0f
    private var initialOriginY = 0f
    private var initialHeight = 0f

    private var mMapView: WeakReference<CreateMapView3D> = WeakReference(this)
    private var mapLayers: MutableList<SlamWareBaseView<CreateMapView3D>> = CopyOnWriteArrayList()
    private var mPngMapView: PngMapView? = null //png地图
    var mMapOutline3D: MapOutline3DGL? = null // OpenGL 轮廓
    private var mCreatingUpLaserScanView: UpLaserScanView3D? = null//上激光点云
    private var mAllKeyFrames: AllKeyFrameView3D? = null//所有关键帧
    private var mUpLaserScanView: UpLaserScanView<CreateMapView3D>? = null//上激光点云（非建图显示）
    var mConstrainNodes: ConstrainNodes? = null//人工约束节点
//    private var mCreateMapRobotView: RobotViewCreateMap<CreateMapView3D>? = null //机器人图标
    private var mExpandAreaView: ExpandAreaView<CreateMapView3D>? = null //地图更新区域

    // 机器人位姿 [x, y, theta(rad), z, roll, pitch]
    override val robotPose = FloatArray(6)

    var isMapping = false//是否建图标志
    //是否第一次接收到子图数据，如果没收到子图，直接跳过旋转环境
    var isStartRevSubMaps = false

    // 监听器
    private var mSingleTapListener: ISingleTapListener? = null
    private var mGestureDetector: SlamGestureDetector? = null

    // ──────────────── Flow 刷新相关 ────────────────
    // 用于按需刷新的触发流
    private val renderTrigger = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    // 协程作用域（与 Surface 生命周期绑定）
    private var renderScope: CoroutineScope? = null
    // 标记 Surface 是否已创建
    private var surfaceCreated = false

    /**
     * 节流发射运算符：保证发射间隔 ≥ periodMs，并总是发射时间窗口内的最新值。
     * 第一个元素立即发射，后续元素会被延迟直到距上一次发射至少 periodMs。
     * 上游结束时，最后一个累积值也会在 periodMs 内发射（尾随保证）。
     * 使用 channelFlow 解决 emit 在不同协程调用的线程安全问题。
     */
    private fun <T> Flow<T>.throttleLatest(periodMs: Long): Flow<T> = channelFlow {
        val scope = CoroutineScope(coroutineContext + SupervisorJob())
        var latestValue: T? = null
        var emissionJob: Job? = null
        var lastEmitTime = 0L
        val lock = Any()

        suspend fun emitLatest() {
            val value = synchronized(lock) { latestValue }
            if (value != null) {
                send(value)
                lastEmitTime = System.currentTimeMillis()
                synchronized(lock) { latestValue = null }
            }
        }

        // 收集上游
        val collectorJob = launch {
            collect { value ->
                synchronized(lock) { latestValue = value }
                val now = System.currentTimeMillis()
                val elapsed = now - lastEmitTime
                if (elapsed >= periodMs) {
                    // 取消之前的延迟任务
                    emissionJob?.cancel()
                    emissionJob = null
                    emitLatest()
                } else {
                    if (emissionJob == null || emissionJob?.isCompleted == true) {
                        emissionJob = launch {
                            delay(periodMs - elapsed)
                            emitLatest()
                        }
                    }
                }
            }
            // 上游完成时处理最后的值
            emissionJob?.join()
            emitLatest()
        }

        // 等待收集完成
        collectorJob.join()
        scope.cancel()
    }

    // ──────────────── 初始化 ────────────────
    init {
        // 初始化 SurfaceHolder 回调
        holder.addCallback(this)

        mOuterMatrix = Matrix()
        mGestureDetector = SlamGestureDetector(this, this)
        initView()
    }

    private fun initView() {
        mPngMapView = PngMapView(context)
        mCreatingUpLaserScanView = UpLaserScanView3D(context, mMapView)
        mAllKeyFrames = AllKeyFrameView3D(context, mMapView)
        mUpLaserScanView = UpLaserScanView(context, mMapView)
        mConstrainNodes = ConstrainNodes(context, mMapView)
//        mCreateMapRobotView = RobotViewCreateMap(context, mMapView)
        mExpandAreaView = ExpandAreaView(context, mMapView)

        // 注意：SurfaceView 模式下不再使用 addView 添加子 View
        // 图层顺序：扩展区域→约束→建图激光→关键帧→非建图激光→机器人
        //扩展区域
        addMapLayers(mExpandAreaView)
        //人工约束节点
        addMapLayers(mConstrainNodes)
        //建图上激光点云
        addMapLayers(mCreatingUpLaserScanView)
        //所有关键帧
        addMapLayers(mAllKeyFrames)
        //非建图上激光点云
        addMapLayers(mUpLaserScanView)
        //机器人图标
//        addMapLayers(mCreateMapRobotView)
    }

    // ──────────────── SurfaceHolder.Callback ────────────────
    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceCreated = true
        // 启动渲染协程
        renderScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        renderScope?.launch {
            renderTrigger
                .throttleLatest(25L)   // 每 25ms 最多绘制一次
                .collect {
                    drawFrame(holder)
                }
        }
        // 触发首次绘制
        requestRender()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        VIEW_WIDTH = width
        VIEW_HEIGHT = height

        // 更新虚拟子 View 的布局尺寸
        mPngMapView?.layout(0, 0, width, height)
        for (layer in mapLayers) {
            layer.layout(0, 0, width, height)
        }

        // 尺寸变化后重新计算居中（如果地图已加载）
        if (mSrf.mapData.width > 0 && mSrf.mapData.height > 0) {
            setCentred()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceCreated = false
        renderScope?.cancel()
        renderScope = null
    }

    /**
     * 执行一帧绘制（在协程中调用）
     */
    private fun drawFrame(holder: SurfaceHolder) {
        var canvas: Canvas? = null
        try {
            canvas = holder.lockCanvas()
            if (canvas != null) {
                synchronized(holder) {
                    // 白色背景
                    canvas.drawColor(Color.WHITE)
                    // 绘制底图
                    mPngMapView?.draw(canvas)
                    // 绘制各图层
                    for (layer in mapLayers) {
                        layer.draw(canvas)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            if (canvas != null) {
                try {
                    holder.unlockCanvasAndPost(canvas)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    /**
     * 请求一次绘制（数据更新时调用）
     */
    fun requestRender() {
        if (surfaceCreated) {
            renderTrigger.tryEmit(Unit)
        }
    }

    // ──────────────── 视图生命周期 ────────────────
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (mMapOutline3D == null && parent is ViewGroup) {
            val parentGroup = parent as ViewGroup

            // 创建 GL 图层
            val glLayer = MapOutline3DGL(context, WeakReference(this)).apply {
                setZOrderOnTop(false)
                setZOrderMediaOverlay(false)
                renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                setBackgroundColor(Color.TRANSPARENT)
            }

            // 设置与地图完全相同的布局参数（支持 ConstraintLayout 和普通布局）
            glLayer.layoutParams = createMatchedLayoutParams()

            val myIndex = parentGroup.indexOfChild(this)
            parentGroup.addView(glLayer, myIndex + 1)

            // 按钮置顶
//            parentGroup.findViewById<View>(R.id.ll_rotate)?.bringToFront()

            // 设置机器人图标（从资源加载）
            val robotBitmap = BitmapFactory.decodeResource(resources, R.mipmap.current_location)
            glLayer.setRobotBitmap(robotBitmap)

            mMapOutline3D = glLayer
        }
    }

    /**
     * 创建与当前视图布局参数完全一致的新 LayoutParams 对象，
     * 确保 GL 图层能与地图视图完美重合。
     */
    private fun createMatchedLayoutParams(): ViewGroup.LayoutParams {
        val myLp = layoutParams
        if (myLp is ConstraintLayout.LayoutParams) {
            // 手动复制 ConstraintLayout 的约束
            val newLp = ConstraintLayout.LayoutParams(0, 0)
            newLp.startToStart = myLp.startToStart
            newLp.startToEnd = myLp.startToEnd
            newLp.endToStart = myLp.endToStart
            newLp.endToEnd = myLp.endToEnd
            newLp.topToTop = myLp.topToTop
            newLp.topToBottom = myLp.topToBottom
            newLp.bottomToTop = myLp.bottomToTop
            newLp.bottomToBottom = myLp.bottomToBottom
            newLp.leftToLeft = myLp.leftToLeft
            newLp.leftToRight = myLp.leftToRight
            newLp.rightToLeft = myLp.rightToLeft
            newLp.rightToRight = myLp.rightToRight
            // 复制 margin
            newLp.setMargins(myLp.leftMargin, myLp.topMargin, myLp.rightMargin, myLp.bottomMargin)
            return newLp
        } else {
            // 其他布局类型（如 FrameLayout、LinearLayout）直接复制宽高和 margin
            return if (myLp is ViewGroup.MarginLayoutParams) {
                ViewGroup.MarginLayoutParams(myLp.width, myLp.height).apply {
                    setMargins(myLp.leftMargin, myLp.topMargin, myLp.rightMargin, myLp.bottomMargin)
                }
            } else {
                ViewGroup.LayoutParams(myLp.width, myLp.height)
            }
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        // 延迟移除 OpenGL 图层，防止在 detach 遍历期间修改子视图列表
        val glLayer = mMapOutline3D
        mMapOutline3D = null
        if (glLayer != null) {
            post {
                if (glLayer.parent is ViewGroup) {
                    (glLayer.parent as ViewGroup).removeView(glLayer)
                }
            }
        }

//        // 移除 OpenGL 图层
//        mMapOutline3D?.let {
//            (it.parent as? ViewGroup)?.removeView(it)
//        }
//        mMapOutline3D = null

        // 清理协程
        renderScope?.cancel()
        renderScope = null

        // 清理资源
        mapLayers.clear()
        mPngMapView = null
        mCreatingUpLaserScanView = null
        mAllKeyFrames = null
        mUpLaserScanView = null
//        mCreateMapRobotView = null
        mSingleTapListener = null
        mGestureDetector = null
        mOuterMatrix = Matrix()
    }

    // ──────────────── 触控与手势 ────────────────
    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isTouchInButtonArea(event)) {
            return false
        }
        if (currentWorkMode == WorkMode.MODE_EXTEND_MAP_ADD_REGION) {
            // SurfaceView 模式下，需要手动分发事件给 ExpandAreaView
            val handled = mExpandAreaView?.onTouchEvent(event) ?: false
            // 返回true表示事件已处理，禁止手势检测器处理，从而禁止底图拖动
            if (handled) return true
        }

        // 非特殊模式或在扩展区域模式下未被处理的事件，由手势检测器处理
        return mGestureDetector!!.onTouchEvent(event, this)
    }

    private fun isTouchInButtonArea(event: MotionEvent): Boolean {
        val container = (parent as? ViewGroup)?.findViewById<View>(R.id.ll_rotate) ?: return false
        val rect = android.graphics.Rect()
        container.getGlobalVisibleRect(rect)
        return rect.contains(event.rawX.toInt(), event.rawY.toInt())
    }

    fun dispatchGestureEvent(event: MotionEvent): Boolean {
        return mGestureDetector?.onTouchEvent(event, this) ?: false
    }

    // ──────────────── 手势回调 ────────────────
    override fun onMapTap(event: MotionEvent) {
        singleTap(event)
    }

    override fun onMapPinch(factor: Float, center: PointF) {
        setScale(factor, center.x, center.y)
    }

    override fun onMapMove(distanceX: Int, distanceY: Int) {
        // 在扩展地图增加区域模式下禁止滑动
        if (currentWorkMode != WorkMode.MODE_EXTEND_MAP_ADD_REGION) {
            setTransition(distanceX, distanceY)
        }
    }

    override fun onMapRotate(factor: Float, center: PointF) {
        setRotation(factor, center.x.toInt(), center.y.toInt())
    }

    private fun singleTap(event: MotionEvent) {
        mSingleTapListener?.onSingleTapListener(screenToWorld(event.x, event.y))
    }

    // ──────────────── 矩阵变换（每次变换后调用 requestRender） ────────────────
    private fun setRotation(factor: Float, cx: Int, cy: Int) {
        mOuterMatrix.postRotate(RadianUtil.toAngel(factor), cx.toFloat(), cy.toFloat())
        setMatrixWithRotation(mOuterMatrix, factor)
        mMapOutline3D?.notifyMatrixChanged()
        requestRender()
    }
    /**
     * 恢复旋转角度
     */
    fun resetRotation() = mGestureDetector?.resetRotation()

    /**
     * 是否是建图模式
     */
    fun isCreateMapMode(): Boolean = currentWorkMode == WorkMode.MODE_CREATE_MAP



    /**
     * 获取当前视图的旋转弧度
     */
    open fun getViewRotation(): Float {
        val values = FloatArray(9)
        mOuterMatrix.getValues(values)
        var angle = atan2(
            values[Matrix.MSKEW_Y].toDouble(), values[Matrix.MSCALE_X].toDouble()
        ).toFloat()
        // 解决精度丢失导致角度不为0的问题
        if (Math.abs(angle) < 0.001f) {
            angle = 0f
        }
        return angle
    }

    private fun setTransition(dx: Int, dy: Int) {
        mOuterMatrix.postTranslate(dx.toFloat(), dy.toFloat())
        if (currentWorkMode == WorkMode.MODE_EXTEND_MAP_ADD_REGION) {
            // 在扩展地图增加区域模式下，只更新子图层的矩阵，不更新 png 地图
            for (mapLayer in mapLayers) {
                mapLayer.setMatrix(mOuterMatrix)
            }
        } else {
            setMatrix(mOuterMatrix)
        }
        mMapOutline3D?.notifyMatrixChanged()
        requestRender()
    }

    private fun setScale(factor: Float, cx: Float, cy: Float) {
        val scale = mMapScale * factor
        if (scale > mMaxMapScale || scale < mMinMapScale) return
        mMapScale = scale
        mSrf.scale = mMapScale
        mOuterMatrix.postScale(factor, factor, cx, cy)
        setMatrixWithScale(mOuterMatrix, mMapScale)
        mMapOutline3D?.notifyMatrixChanged()
        requestRender()
    }


    fun setRotate(boolean: Boolean) {
        mGestureDetector?.isRotate = boolean
    }

    // ──────────────── 矩阵分发（内部使用，不再主动调用 invalidate） ────────────────
    private fun setMatrix(matrix: Matrix) {
        // 复制矩阵以保证渲染线程安全
        val matrixCopy = Matrix(matrix)
        if (currentWorkMode != WorkMode.MODE_EXTEND_MAP_ADD_REGION) {
            mPngMapView?.setMatrix(matrixCopy)
        }
        for (mapLayer in mapLayers) {
            mapLayer.setMatrix(matrixCopy)
        }
    }

    private fun setMatrixWithScale(matrix: Matrix, scale: Float) {
        mOuterMatrix = matrix
        mMapScale = scale
        // 复制矩阵以保证渲染线程安全
        val matrixCopy = Matrix(matrix)
        if (currentWorkMode != WorkMode.MODE_EXTEND_MAP_ADD_REGION) {
            mPngMapView?.setMatrix(matrixCopy)
        }
        for (mapLayer in mapLayers) {
            mapLayer.setMatrixWithScale(matrixCopy, scale)
        }
    }

    private fun setMatrixWithScaleAndRotation(matrix: Matrix, scale: Float, rotation: Float) {
        mOuterMatrix = matrix
        mMapScale = scale
        // 复制矩阵以保证渲染线程安全
        val matrixCopy = Matrix(matrix)
        mPngMapView?.setMatrix(matrixCopy)
        for (mapLayer in mapLayers) {
            mapLayer.setMatrixWithScale(matrixCopy, scale)
        }
    }

    private fun setMatrixWithRotation(matrix: Matrix, rotation: Float) {
        mOuterMatrix = matrix
        // 复制矩阵以保证渲染线程安全
        val matrixCopy = Matrix(matrix)
        if (currentWorkMode != WorkMode.MODE_EXTEND_MAP_ADD_REGION) {
            mPngMapView?.setMatrix(matrixCopy)
        }
        for (mapLayer in mapLayers) {
            mapLayer.setMatrixWithRotation(matrixCopy, rotation)
        }
    }

    // ──────────────── 地图居中 ────────────────
    fun setCentred() {
        // 等待视图有尺寸
        if (VIEW_WIDTH == 0 || VIEW_HEIGHT == 0) {
            post { setCentred() }
            return
        }
        if (mSrf.mapData.width > 0 && mSrf.mapData.height > 0) {
            val iWidth = mSrf.mapData.width
            val iHeight = mSrf.mapData.height
            val scaledRect = RectF()
            MathUtils.calculateScaledRectInContainer(
                RectF(0f, 0f, VIEW_WIDTH.toFloat(), VIEW_HEIGHT.toFloat()),
                iWidth, iHeight, ImageView.ScaleType.FIT_CENTER, scaledRect
            )
            val scale = scaledRect.width() / iWidth
            mMinMapScale = scale / 4
            mMapScale = scale
            mOuterMatrix = Matrix()
            mOuterMatrix.postScale(mMapScale, mMapScale)
            mOuterMatrix.postTranslate(
                (VIEW_WIDTH - mMapScale * iWidth) / 2,
                (VIEW_HEIGHT - mMapScale * iHeight) / 2
            )
            setMatrixWithScaleAndRotation(mOuterMatrix, mMapScale, 0f)
            mMapOutline3D?.notifyMatrixChanged()
            requestRender()
        }
    }

    /**
     * 世界坐标转屏幕坐标
     */
    override fun worldToScreen(x: Float, y: Float): PointF {
        synchronized(mSrf.mapData) {
            return mapPixelCoordinateToMapWidthCoordinateF(mSrf.worldToScreen(x, y))
        }
    }

    /**
     * 屏幕坐标转世界坐标
     */
    override fun screenToWorld(x: Float, y: Float): PointF {
        synchronized(mSrf.mapData) {
            // 首先将屏幕坐标转换为地图像素坐标
            val mapPixelPoint = widgetCoordinateToMapPixelCoordinate(PointF(x, y))
            // 然后使用坐标转换工具将地图像素坐标转换为世界坐标
            return mSrf.screenToWorld(mapPixelPoint.x, mapPixelPoint.y)
        }
    }

    private fun mapPixelCoordinateToMapWidthCoordinateF(mapPixelPointF: PointF): PointF {
        val m = mOuterMatrix
        val points = floatArrayOf(mapPixelPointF.x, mapPixelPointF.y)
        m.mapPoints(points)
        return PointF(points[0], points[1])
    }

    private fun widgetCoordinateToMapPixelCoordinate(screenPointF: PointF): PointF {
        val m = mOuterMatrix
        val points = floatArrayOf(screenPointF.x, screenPointF.y)
        val values = MathUtils.inverseMatrixPoint(m, points)
        return PointF(values[0], values[1])
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        currentWorkMode = mode
        mMapOutline3D?.setWorkMode(mode)
        mCreatingUpLaserScanView?.setWorkMode(mode)
        mAllKeyFrames?.setWorkMode(mode)
//        mCreateMapRobotView?.setWorkMode(mode)
        mExpandAreaView?.setWorkMode(mode)
        requestRender()
    }
    /**
     * 获取当前工作模式
     */
    override fun getCurrentWorkMode() = currentWorkMode
    /**
     * 加载地图
     * pngPath png文件路径
     * yamlPath yaml文件路径
     */
    fun loadMap(pngPath: String, yamlPath: String) {
        val file = File(pngPath)
        Glide.with(this)
            .asBitmap()
            .load(file)
            .skipMemoryCache(true)
            .format(DecodeFormat.PREFER_RGB_565)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .into(object : SimpleTarget<Bitmap?>() {
                override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap?>?) {
                    val mPngMapData = YamlNew().loadYaml(
                        yamlPath,
                        resource.height.toFloat(),
                        resource.width.toFloat()
                    )
                    setBitmap(mPngMapData, resource)
                }
            })
    }
    /**
     * 设置地图数据信息
     * 设置地图
     *
     * @param bitmap
     */
    private fun setBitmap(mapData: MapData, bitmap: Bitmap) {
        synchronized(mSrf.mapData) {
            mSrf.mapData.width = mapData.width
            mSrf.mapData.height = mapData.height
            mSrf.mapData.originX = mapData.originX
            mSrf.mapData.originY = mapData.originY
            mSrf.mapData.resolution = mapData.resolution

            // 记录初始地图参数
            initialOriginX = mapData.originX
            initialOriginY = mapData.originY
            initialHeight = mapData.height
        }
        mPngMapView?.setBitmap(bitmap)
        // 设置地图后自动居中显示
        setCentred()
    }
    /**
     * 外部接口 解析激光点云数据（建图模式） 3D
     *      * type 更新0
     *      * type 扩展1
     *      * type 新建2
     */
    fun parseLaserData(laserData: laser_t, type: Int) {

        // 更新机器人位置（始终需要处理，不参与降采样）
        updateRobotPose(
            laserData.ranges[0],
            laserData.ranges[1],
            laserData.ranges[2],
            laserData.ranges[3],
            laserData.ranges[4],
            laserData.ranges[5]
        )
        if (laserData.ranges.size <= 6) return // 最少包含机器人位置数据

        //保持居中
        if (currentWorkMode == WorkMode.MODE_CREATE_MAP) {
            keepRobotCentered()
        }
        calBinding(laserData, type)
        //更新点云数据
        mCreatingUpLaserScanView?.updateUpLaserScan(laserData)
        mMapOutline3D?.updateRobotPose(robotPose[0], robotPose[1], robotPose[2])
        requestRender()
    }

    fun loadCurPointCloud(laserData: laser_t) {
        mUpLaserScanView?.updateUpLaserScan(laserData)
        requestRender()
    }

    fun parseOptPose(laserData: laser_t) {
        mMapOutline3D?.parseOptPose(laserData)
        mMapOutline3D?.requestRender()
        requestRender()
    }

    fun showKeyFrames(boolean: Boolean) {
        mMapOutline3D?.setDrawingEnabled(boolean)
        mAllKeyFrames?.setDrawingEnabled(boolean)
        requestRender()
    }

    fun addConstraintNodes(constraintNode: ConstraintNode) {
        mConstrainNodes?.addConstraintNodes(constraintNode)
        requestRender()
    }

    fun parseKeyFramePose(mLaserT: laser_t) {
        mAllKeyFrames?.parseKeyFramePose(mLaserT)
        requestRender()
    }

    /**
     * 计算新建地图宽高
     */
    private fun calBinding(laserData: laser_t, type: Int) {
//        Log.d(TAG, "calBinding mSrf.mapData.width ${laserData.intensities[0]}")
//        Log.d(TAG, "calBinding mSrf.mapData.height ${laserData.intensities[1]}")
//        Log.d(TAG, "calBinding originX ${laserData.intensities[2]}")
//        Log.d(TAG, "calBinding originY ${laserData.intensities[3]}")

        synchronized(mSrf.mapData) {
            if (type == 0) {//更新 使用地图PNG原有的宽高


            } else if (type == 1) {//扩展 （1地图内的时候使用地图宽高、2地图外的时候使用子图计算的宽高）
                // 更新地图元数据
                mSrf.mapData.height = laserData.intensities[0]
                mSrf.mapData.width = laserData.intensities[1]
                mSrf.mapData.originX = laserData.intensities[2]
                mSrf.mapData.originY = laserData.intensities[3]

                // 计算并设置PngMapView的偏移量
                val res = mSrf.mapData.resolution
                if (res > 0.0001f) {
                    val offX = (initialOriginX - mSrf.mapData.originX) / res
                    val offY =
                        (mSrf.mapData.height - initialHeight) + (mSrf.mapData.originY - initialOriginY) / res
                    mPngMapView?.setOffset(offX, offY)
                }

            } else {//新建
                // 解析地图元数据（关键帧或非关键帧均需要）
                mSrf.mapData.height = laserData.intensities[0]
                mSrf.mapData.width = laserData.intensities[1]
                mSrf.mapData.originX = laserData.intensities[2]
                mSrf.mapData.originY = laserData.intensities[3]
                mSrf.mapData.resolution = laserData.intensities[4]
            }
        }
    }
    /**
     * 更新机器人位置（弧度制）
     */
    fun updateRobotPose(
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
    private fun convertScientificToDecimal(value: Float): Float {
        // 优化：移除DecimalFormat，使用数学运算保留3位小数
        // 避免 String.format 和 parseFloat 带来的大量GC和CPU消耗
        return kotlin.math.round(value * 1000f) / 1000f
    }

    /**
     * 保持车体居中显示
     */
    private fun keepRobotCentered() {
        if (VIEW_WIDTH == 0 || VIEW_HEIGHT == 0) return

        // 将机器人当前位置转换为屏幕坐标
        val robotScreenPos = worldToScreen(robotPose[0], robotPose[1])

        // 计算屏幕中心
        val centerX = VIEW_WIDTH / 2f
        val centerY = VIEW_HEIGHT / 2f

        // 计算需要移动的距离
        val dx = centerX - robotScreenPos.x
        val dy = centerY - robotScreenPos.y

        // 移动地图使机器人居中
        setTransition(dx.toInt(), dy.toInt())
//        Log.d("LogUtil", "移动地图使机器人居中")
    }

    fun resetExpandAreaView() {
        mExpandAreaView?.resetCreateState()
        requestRender()
    }


    /**
     * 手指抬起监听 回调是世界坐标
     */
    fun setSingleTapListener(listener: ISingleTapListener?) {
        mSingleTapListener = listener
    }
    /**
     * 获取扩展区域视图实例
     */
    fun getExpandAreaView(): ExpandAreaView<CreateMapView3D>? = mExpandAreaView



    // 兼容旧版 invalidate 调用，现统一转为按需请求
    override fun invalidate() {
        requestRender()
    }

    val outerMatrix: Matrix get() = mOuterMatrix
    val viewWidth: Int get() = VIEW_WIDTH
    val viewHeight: Int get() = VIEW_HEIGHT

    private fun addMapLayers(mapLayer: SlamWareBaseView<CreateMapView3D>?) {
        if (mapLayer != null && !mapLayers.contains(mapLayer)) {
            mapLayers.add(mapLayer)
        }
    }

    interface ISingleTapListener {
        fun onSingleTapListener(point: PointF)
    }
}