package com.siasun.dianshi.view.createMap.map2D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.R
import com.siasun.dianshi.bean.MapData
import com.siasun.dianshi.utils.CoordinateConversion
import com.siasun.dianshi.utils.MathUtils
import com.siasun.dianshi.utils.RadianUtil
import com.siasun.dianshi.utils.SlamGestureDetector
import com.siasun.dianshi.utils.YamlNew
import com.siasun.dianshi.view.WorkMode
import com.siasun.dianshi.view.createMap.MapViewInterface
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import androidx.core.content.withStyledAttributes
import com.siasun.dianshi.view.createMap.ExpandAreaView
import com.siasun.dianshi.view.PngMapView
import com.siasun.dianshi.view.SlamWareBaseView
import com.siasun.dianshi.view.createMap.RobotViewCreateMap

/**
 * 地图画布
 * 2D 建图View
 */
class CreateMapView2D(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs),
    SlamGestureDetector.OnRPGestureListener, MapViewInterface, SurfaceHolder.Callback {

    // 渲染线程
    private var mRenderThread: RenderThread? = null

    // 当前工作模式
    private var currentWorkMode = WorkMode.MODE_SHOW_MAP

    var mSrf = CoordinateConversion()//坐标转化工具类
    private var mOuterMatrix = Matrix()
    private var VIEW_WIDTH = 0 //视图宽度
    private var VIEW_HEIGHT = 0//视图高度

    //视图高度
    var mMapScale = 1f //地图缩放级别

    private val mMaxMapScale = 5f //最大缩放级别
    private var mMinMapScale = 0.1f //最小缩放级别

    private var mMapView: WeakReference<CreateMapView2D> = WeakReference(this)
    private var mapLayers: MutableList<SlamWareBaseView<CreateMapView2D>> = CopyOnWriteArrayList()
    private var mPngMapView: PngMapView? = null //png地图
    private var mMapOutline2D: MapOutline2D? = null //地图轮廓
    private var mExpandAreaView: ExpandAreaView<CreateMapView2D>? = null //地图更新区域
    private var mUpLaserScanView: UpLaserScanView2D? = null//上激光点云
    private var mCreateMapRobotView: RobotViewCreateMap<CreateMapView2D>? = null //机器人图标

    // 机器人位姿 [x, y, theta(rad), z, roll, pitch]
    override val robotPose = FloatArray(6)

    var isMapping = false//是否建图标志

    //是否第一次接收到子图数据，如果没收到子图，直接跳过旋转环境
    var isStartRevSubMaps = false

    /**
     * 旋转弧度
     */
    var rotationRadians = 0f

    /**
     * *************** 监听器   start ***********************
     */

    private var mSingleTapListener: ISingleTapListener? = null
    private var mGestureDetector: SlamGestureDetector? = null


    /**
     * *************** 监听器   end ***********************
     */

    init {
        // 初始化 SurfaceHolder 回调
        holder.addCallback(this)

        mOuterMatrix = Matrix()
        mGestureDetector = SlamGestureDetector(this, this)
        initView()
        setViewVisibility(attrs)
    }

    /**
     * 设置各个View显示
     */
    private fun setViewVisibility(attrs: AttributeSet?) {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.MapView) {}
        }
    }


    private fun initView() {
        // SurfaceView 模式下不再使用 addView 添加子 View
        // 而是通过 RenderThread 手动绘制这些 View
        mPngMapView = PngMapView(context)
        mUpLaserScanView = UpLaserScanView2D(context, mMapView)
        mMapOutline2D = MapOutline2D(context, mMapView)
        mCreateMapRobotView = RobotViewCreateMap(context, mMapView)
        mExpandAreaView = ExpandAreaView(context, mMapView)

        //扩展区域
        addMapLayers(mExpandAreaView)
        //地图轮廓
        addMapLayers(mMapOutline2D)
        //上激光点云
        addMapLayers(mUpLaserScanView)
        //机器人图标
        addMapLayers(mCreateMapRobotView)

        setCentred()
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (currentWorkMode == WorkMode.MODE_EXTEND_MAP_ADD_REGION) {
            // SurfaceView 模式下，需要手动分发事件给 ExpandAreaView
            mExpandAreaView?.onTouchEvent(event)
            // 返回true表示事件已处理，禁止手势检测器处理，从而禁止底图拖动
            return true
        }

        // 非特殊模式，由手势检测器处理事件
        return mGestureDetector!!.onTouchEvent(event, this)
    }

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

    private fun setRotation(factor: Float, cx: Int, cy: Int) {
        mOuterMatrix.postRotate(RadianUtil.toAngel(factor), cx.toFloat(), cy.toFloat())
        setMatrixWithRotation(mOuterMatrix, factor)
    }

    private fun setTransition(dx: Int, dy: Int) {
        mOuterMatrix.postTranslate(dx.toFloat(), dy.toFloat())
        if (currentWorkMode == WorkMode.MODE_EXTEND_MAP_ADD_REGION) {
            // 在扩展地图增加区域模式下，只更新子图层的矩阵，不更新 png 地图
            for (mapLayer in mapLayers) {
                mapLayer.setMatrix(mOuterMatrix)
            }
            // postInvalidate() // RenderThread handles this
        } else {
            setMatrix(mOuterMatrix)
        }
    }

    private fun setScale(factor: Float, cx: Float, cy: Float) {
        val scale = mMapScale * factor
        if (scale > mMaxMapScale || scale < mMinMapScale) {
            return
        }
        mMapScale = scale
        mSrf.scale = mMapScale
        mOuterMatrix.postScale(factor, factor, cx, cy)
        setMatrixWithScale(mOuterMatrix, mMapScale)
    }

    private fun singleTap(event: MotionEvent) {
        mSingleTapListener?.onSingleTapListener(screenToWorld(event.x, event.y))
    }

    private fun setMatrix(matrix: Matrix) {
        // 复制矩阵以保证渲染线程安全
        val matrixCopy = Matrix(matrix)
        if (currentWorkMode != WorkMode.MODE_EXTEND_MAP_ADD_REGION) {
            mPngMapView?.setMatrix(matrixCopy)
        }
        for (mapLayer in mapLayers) {
            mapLayer.setMatrix(matrixCopy)
        }
        // postInvalidate() // RenderThread 自动循环渲染，不需要 invalidate
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
            mapLayer.mRotation = rotation
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
        rotationRadians = RadianUtil.toRadians(mMapOutline2D!!.mRotation)
    }

    fun setCentred() {
        val scaledRect = RectF()
        if (VIEW_WIDTH == 0 || VIEW_HEIGHT == 0) {
            // 使用弱引用避免内存泄漏
            val weakRef = WeakReference(this)
            val listener = object : OnGlobalLayoutListener {
                @RequiresApi(Build.VERSION_CODES.KITKAT)
                override fun onGlobalLayout() {
                    val mapView = weakRef.get()
                    if (mapView != null && mapView.isAttachedToWindow) {
                        mapView.VIEW_HEIGHT = mapView.height
                        mapView.VIEW_WIDTH = mapView.width
                        // 使用兼容版本的移除方法
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                            mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        } else {
                            mapView.viewTreeObserver.removeGlobalOnLayoutListener(this)
                        }
                        mapView.setCentred()
                    }
                }
            }
            getViewTreeObserver().addOnGlobalLayoutListener(listener)
            return
        }
        if (mSrf.mapData.width > 0 && mSrf.mapData.height > 0) {
            val iWidth = mSrf.mapData.width
            val iHeight = mSrf.mapData.height

            MathUtils.calculateScaledRectInContainer(
                RectF(
                    0f, 0f, VIEW_WIDTH.toFloat(), VIEW_HEIGHT.toFloat()
                ), iWidth, iHeight, ImageView.ScaleType.FIT_CENTER, scaledRect
            )
            val scale = scaledRect.width() / iWidth
            mMinMapScale = scale / 4
            mMapScale = scale
            mOuterMatrix = Matrix()
            mOuterMatrix.postScale(mMapScale, mMapScale)
            mOuterMatrix.postTranslate(
                (VIEW_WIDTH - mMapScale * iWidth) / 2, (VIEW_HEIGHT - mMapScale * iHeight) / 2
            )
            setMatrixWithScaleAndRotation(mOuterMatrix, mMapScale, 0f)
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        VIEW_WIDTH = MeasureSpec.getSize(widthMeasureSpec)
        VIEW_HEIGHT = MeasureSpec.getSize(heightMeasureSpec)
    }

    val outerMatrix: Matrix
        get() = mOuterMatrix

    /**
     * 获取视图宽度
     */
    val viewWidth: Int
        get() = VIEW_WIDTH

    /**
     * 获取视图高度
     */
    val viewHeight: Int
        get() = VIEW_HEIGHT


    /**
     * 世界坐标转屏幕坐标
     */
    override fun worldToScreen(x: Float, y: Float): PointF {
        synchronized(mSrf.mapData) {
            return mapPixelCoordinateToMapWidthCoordinateF(mSrf.worldToScreen(x, y))
        }
    }

    private fun mapPixelCoordinateToMapWidthCoordinateF(mapPixelPointF: PointF): PointF {
        val m = mOuterMatrix
        val points = floatArrayOf(mapPixelPointF.x, mapPixelPointF.y)
        m.mapPoints(points)
        return PointF(points[0], points[1])
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

    private fun widgetCoordinateToMapPixelCoordinate(screenPointF: PointF): PointF {
        val m = mOuterMatrix
        val points = floatArrayOf(screenPointF.x, screenPointF.y)
        val values = MathUtils.inverseMatrixPoint(m, points)
        return PointF(values[0], values[1])
    }

    /**
     * 刷新所有层数据
     */
    override fun invalidate() {
        super.postInvalidate()
        mPngMapView?.postInvalidate()
        for (mapLayer in mapLayers) {
            mapLayer.postInvalidate()
        }
    }

    private fun addMapLayers(mapLayer: SlamWareBaseView<CreateMapView2D>?) {
        if (mapLayer != null && !mapLayers.contains(mapLayer)) {
            mapLayers.add(mapLayer)
            // SurfaceView 不添加子 View
        }
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        // 停止渲染线程
        mRenderThread?.setRunning(false)
        try {
            mRenderThread?.join()
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
        mRenderThread = null

        // 清理所有资源，避免内存泄漏
        mapLayers.clear()

        // 清理视图引用
        mPngMapView = null
        mUpLaserScanView = null
        mCreateMapRobotView = null

        // 清理监听器
        mSingleTapListener = null
        mGestureDetector = null

        // 清理矩阵和其他对象
        mOuterMatrix = Matrix()
    }


    /**
     * ******************************************************
     * *******************      外部接口        **************
     * ******************************************************
     */

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        currentWorkMode = mode
        mMapOutline2D?.setWorkMode(mode)
        mUpLaserScanView?.setWorkMode(mode)
        mCreateMapRobotView?.setWorkMode(mode)
        mExpandAreaView?.setWorkMode(mode)
    }

    /**
     * 获取当前工作模式
     */
    override fun getCurrentWorkMode(): WorkMode {
        return currentWorkMode
    }

    /**
     * 加载地图
     * pngPath png文件路径
     * yamlPath yaml文件路径
     */
    fun loadMap(pngPath: String, yamlPath: String) {
        val file = File(pngPath)
        Glide.with(this).asBitmap().load(file).skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE).into(object : SimpleTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap, transition: Transition<in Bitmap?>?
                ) {
                    val mPngMapData = YamlNew().loadYaml(
                        yamlPath,
                        resource.height.toFloat(),
                        resource.width.toFloat(),
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
        mSrf.mapData.width = mapData.width
        mSrf.mapData.height = mapData.height
        mSrf.mapData.originX = mapData.originX
        mSrf.mapData.originY = mapData.originY
        mSrf.mapData.resolution = mapData.resolution
        mPngMapView?.setBitmap(bitmap)
        // 设置地图后自动居中显示
        setCentred()
    }


    /**
     * 外部接口：更新子图数据 （建图模式） 2D
     * type 扩展1
     * type 新建2
     */
    fun parseSubMaps2D(mLaserT: laser_t, type: Int) {
        // 建图模式下，保持车体居中显示
        if (currentWorkMode == WorkMode.MODE_CREATE_MAP) {
            keepRobotCentered()
        }
        mMapOutline2D?.parseSubMaps2D(mLaserT, type)

    }


    /**
     * 外部接口 解析激光点云数据（建图模式） 2D
     */
    fun parseLaserData(laserData: laser_t) {
        // 更新机器人位置（始终需要处理，不参与降采样）
        updateRobotPose(laserData.ranges[0], laserData.ranges[1], laserData.ranges[2])
        mUpLaserScanView?.updateUpLaserScan(laserData)
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
    private fun convertScientificToDecimal(value: Float): Float {
        // 优化：移除DecimalFormat，使用数学运算保留3位小数
        // 避免 String.format 和 parseFloat 带来的大量GC和CPU消耗
        return kotlin.math.round(value * 1000f) / 1000f
    }

    /**
     * 回环检测2D
     * 输入数据 世界坐标系下的位姿态
     */
    fun updateOptPose2D(mLaserT: laser_t, type: Int) = mMapOutline2D?.updateOptPose2D(mLaserT, type)

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

    fun resetExpandAreaView() = mExpandAreaView?.resetCreateState()


    /**
     * 手指抬起监听 回调是世界坐标
     */
    fun setSingleTapListener(listener: ISingleTapListener?) {
        mSingleTapListener = listener
    }

    /**
     * 获取扩展区域视图实例
     */
    fun getExpandAreaView(): ExpandAreaView<CreateMapView2D>? {
        return mExpandAreaView
    }

    interface ISingleTapListener {
        fun onSingleTapListener(point: PointF)
    }

    // SurfaceHolder.Callback 实现
    override fun surfaceCreated(holder: SurfaceHolder) {
        mRenderThread = RenderThread(holder)
        mRenderThread?.setRunning(true)
        mRenderThread?.start()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        VIEW_WIDTH = width
        VIEW_HEIGHT = height

        // 更新虚拟子 View 的布局大小
        mPngMapView?.layout(0, 0, width, height)
        for (layer in mapLayers) {
            layer.layout(0, 0, width, height)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        var retry = true
        mRenderThread?.setRunning(false)
        while (retry) {
            try {
                mRenderThread?.join()
                retry = false
            } catch (e: InterruptedException) {
                e.printStackTrace()
            }
        }
        mRenderThread = null
    }

    // 渲染线程
    inner class RenderThread(private val surfaceHolder: SurfaceHolder) : Thread() {
        private var running = false

        fun setRunning(isRunning: Boolean) {
            running = isRunning
        }

        override fun run() {
            while (running) {
                var canvas: Canvas? = null
                try {
                    canvas = surfaceHolder.lockCanvas()
                    if (canvas != null) {
                        synchronized(surfaceHolder) {
                            // 绘制背景
                            canvas.drawColor(android.graphics.Color.WHITE)

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
                            surfaceHolder.unlockCanvasAndPost(canvas)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        }
    }
}