package com.siasun.dianshi.createMap2D

import VirtualWallNew
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.ImageView
import androidx.annotation.RequiresApi
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.transition.Transition
import com.ngu.lcmtypes.laser_t
import com.ngu.lcmtypes.robot_control_t
import com.siasun.dianshi.R
import com.siasun.dianshi.bean.CleanAreaNew
import com.siasun.dianshi.bean.CmsStation
import com.siasun.dianshi.bean.ElevatorPoint
import com.siasun.dianshi.bean.InitPose
import com.siasun.dianshi.bean.MachineStation
import com.siasun.dianshi.bean.MapData
import com.siasun.dianshi.bean.MergedPoseItem
import com.siasun.dianshi.bean.Point2d
import com.siasun.dianshi.bean.PositingArea
import com.siasun.dianshi.bean.SpArea
import com.siasun.dianshi.bean.TeachPoint
import com.siasun.dianshi.bean.WorkAreasNew
import com.siasun.dianshi.bean.pp.DefPosture
import com.siasun.dianshi.bean.pp.PathPlanResultBean
import com.siasun.dianshi.bean.pp.Posture
import com.siasun.dianshi.bean.pp.world.CLayer
import com.siasun.dianshi.utils.CoordinateConversion
import com.siasun.dianshi.utils.MathUtils
import com.siasun.dianshi.utils.RadianUtil
import com.siasun.dianshi.utils.SlamGestureDetector
import com.siasun.dianshi.utils.YamlNew
import java.io.File
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList
import androidx.core.content.withStyledAttributes
import androidx.core.view.get
import com.hjq.shape.layout.ShapeFrameLayout
import com.siasun.dianshi.view.CrossDoorView
import com.siasun.dianshi.view.DownLaserScanView
import com.siasun.dianshi.view.ElevatorView
import com.siasun.dianshi.view.HomeDockView
import com.siasun.dianshi.view.LegendView
import com.siasun.dianshi.view.MapNameView
import com.siasun.dianshi.view.MixAreaView
import com.siasun.dianshi.view.OnlinePoseView
import com.siasun.dianshi.view.PathView
import com.siasun.dianshi.view.PngMapView
import com.siasun.dianshi.view.PolygonEditView
import com.siasun.dianshi.view.PostingAreasView
import com.siasun.dianshi.view.RemoveNoiseView
import com.siasun.dianshi.view.RobotView
import com.siasun.dianshi.view.SlamWareBaseView
import com.siasun.dianshi.view.SpPolygonEditView
import com.siasun.dianshi.view.StationsView
import com.siasun.dianshi.view.TopViewPathView
import com.siasun.dianshi.view.UpLaserScanView
import com.siasun.dianshi.view.VirtualWallView
import com.siasun.dianshi.view.WorkIngPathView
import com.siasun.dianshi.view.WorldPadView

/**
 * 地图画布
 * 将在此画布中绘制slam的png地图
 */
class CreateMapView2D(context: Context, private val attrs: AttributeSet) :
    ShapeFrameLayout(context, attrs), SlamGestureDetector.OnRPGestureListener {

    // 工作模式枚举
    enum class WorkMode {
        MODE_SHOW_MAP,         // 移动地图模式
        MODE_CREATE_MAP,         // 创建地图模式
    }

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

    private var mMapView: WeakReference<CreateMapView2D> = WeakReference(this)
    private var mapLayers: MutableList<SlamWareBaseView<CreateMapView2D>> = CopyOnWriteArrayList()
    private var mPngMapView: PngMapView? = null //png地图
    private var mUpLaserScanView: UpLaserScanView<CreateMapView2D>? = null//上激光点云
    private var mCreateMapRobotView: RobotView<CreateMapView2D>? = null //机器人图标


    /**
     * *************** 监听器   start ***********************
     */

    private var mSingleTapListener: ISingleTapListener? = null
    private var mGestureDetector: SlamGestureDetector? = null


    /**
     * *************** 监听器   end ***********************
     */

    init {
        // 移除setBackgroundColor调用，让ShapeFrameLayout的shape_solidColor和shape_radius生效
        // 设置clipChildren为true，确保子视图不会超出父视图的圆角区域
        clipChildren = true
        clipToPadding = true
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
        val lp = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
        mPngMapView = PngMapView(context)
        mUpLaserScanView = UpLaserScanView(context, mMapView)
        //底图的View
        addView(mPngMapView, lp)

        //上激光点云
        addMapLayers(mUpLaserScanView)
        //机器人图标
        addMapLayers(mCreateMapRobotView)

        setCentred()
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        val point = screenToWorld(event.x, event.y)

//        // 如果是擦除噪点模式、创建定位区域模式、编辑定位区域模式、删除定位区域模式、编辑清扫区域模式或创建清扫区域模式，或者路径编辑模式，或者创建路径模式
//        if (currentWorkMode == WorkMode.MODE_REMOVE_NOISE || currentWorkMode == WorkMode.MODE_POSITING_AREA_ADD || currentWorkMode == WorkMode.MODE_POSITING_AREA_EDIT || currentWorkMode == WorkMode.MODE_POSITING_AREA_DELETE || currentWorkMode == WorkMode.MODE_CLEAN_AREA_EDIT || currentWorkMode == WorkMode.MODE_CLEAN_AREA_ADD || currentWorkMode == WorkMode.MODE_SP_AREA_EDIT || currentWorkMode == WorkMode.MODE_MIX_AREA_ADD || currentWorkMode == WorkMode.MODE_SP_AREA_EDIT || currentWorkMode == WorkMode.MODE_MIX_AREA_EDIT || currentWorkMode == WorkMode.MODE_PATH_EDIT || currentWorkMode == WorkMode.MODE_PATH_CREATE) {
//            // 让事件传递给子视图（如RemoveNoiseView、PostingAreasView或PathView）处理
//            // 先调用父类的onTouchEvent让事件传递给子视图
//            super.onTouchEvent(event)
//            // 返回true表示事件已处理，禁止手势检测器处理，从而禁止底图拖动
//            return true
//        }

        // 非特殊模式，由手势检测器处理事件
        return mGestureDetector!!.onTouchEvent(event)
    }

    override fun onMapTap(event: MotionEvent) {
        singleTap(event)
    }

    override fun onMapPinch(factor: Float, center: PointF) {
        setScale(factor, center.x, center.y)
    }

    override fun onMapMove(distanceX: Int, distanceY: Int) {
        setTransition(distanceX, distanceY)
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
        setMatrix(mOuterMatrix)
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
        mPngMapView?.setMatrix(matrix)
        for (mapLayer in mapLayers) {
            mapLayer.setMatrix(matrix)
        }
        postInvalidate()
    }

    private fun setMatrixWithScale(matrix: Matrix, scale: Float) {
        mOuterMatrix = matrix
        mMapScale = scale
        mPngMapView?.setMatrix(matrix)
        for (mapLayer in mapLayers) {
            mapLayer.setMatrixWithScale(matrix, scale)
        }
    }

    private fun setMatrixWithScaleAndRotation(matrix: Matrix, scale: Float, rotation: Float) {
        mOuterMatrix = matrix
        mMapScale = scale
        mPngMapView?.setMatrix(matrix)
        for (mapLayer in mapLayers) {
            mapLayer.setMatrixWithScale(matrix, scale)
            mapLayer.mRotation = rotation
        }
    }

    private fun setMatrixWithRotation(matrix: Matrix, rotation: Float) {
        mOuterMatrix = matrix
        mPngMapView?.setMatrix(matrix)
        for (mapLayer in mapLayers) {
            mapLayer.setMatrixWithRotation(matrix, rotation)
        }
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

    /**
     * 世界坐标转屏幕坐标
     */
    fun worldToScreen(x: Float, y: Float): PointF {
        synchronized(mSrf.mapData) {
            return mapPixelCoordinateToMapWidthCoordinateF(mSrf.worldToScreen(x, y))
        }
    }

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
     * 屏幕坐标转世界坐标
     */
    fun screenToWorld(x: Float, y: Float): PointF {
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
            addView(
                mapLayer, LayoutParams(
                    LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT
                )
            )
        }
    }


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

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
    }

    /**
     * 获取当前工作模式
     */
    fun getCurrentWorkMode(): WorkMode {
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
        mSrf.mapData = mapData
        mPngMapView?.setBitmap(bitmap)
        // 设置地图后自动居中显示
        setCentred()
    }


    /***
     * 设置地图显示
     */
    fun setSlamMapViewShow(visibility: Int) {
        mPngMapView!!.visibility = visibility
    }


    /**
     * 设置上激光点云数据源
     */
    fun setUpLaserScan(laser: laser_t) = mUpLaserScanView?.updateUpLaserScan(laser)


    /**
     * 设置AGV 位姿 机器人图标的实时位置
     */
    @SuppressLint("SuspiciousIndentation")
    fun setAgvPose(rt: robot_control_t) {
        val dParams = rt.dparams
        mCreateMapRobotView?.setAgvData(dParams)
    }


    /**
     * 手指抬起监听 回调是世界坐标
     */
    fun setSingleTapListener(listener: ISingleTapListener?) {
        mSingleTapListener = listener
    }

    interface ISingleTapListener {
        fun onSingleTapListener(point: PointF)
    }

}
