package com.siasun.dianshi.view

import VirtualWallNew
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
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
import com.hjq.shape.layout.ShapeFrameLayout
import com.siasun.dianshi.bean.CrossDoor
import com.siasun.dianshi.bean.Inspection
import com.siasun.dianshi.bean.RFID
import com.siasun.dianshi.bean.ReflectorMapBean
import com.siasun.dianshi.view.createMap.MapViewInterface
import kotlin.math.atan2

/**
 * 地图画布
 * 将在此画布中绘制slam的png地图
 */
@SuppressLint("ViewConstructor")
class MapView(context: Context, private val attrs: AttributeSet) : ShapeFrameLayout(context, attrs),
    SlamGestureDetector.OnRPGestureListener, MapViewInterface {


    // 当前工作模式
    private var currentWorkMode = WorkMode.MODE_SHOW_MAP

    var mSrf = CoordinateConversion()//坐标转化工具类
    private var mOuterMatrix = Matrix()
    private var VIEW_WIDTH = 0 //视图宽度
    private var VIEW_HEIGHT = 0 //视图高度


    var mMapScale = 1f //地图缩放级别
    var mMapCenterX = 0f//地图真实世界的中线点X
    var mMapCenterY = 0f//地图真实世界的中线点y
    private val mMaxMapScale = 10f //最大缩放级别
    private var mMinMapScale = 0.1f //最小缩放级别

    private var mMapView: WeakReference<MapView> = WeakReference(this)
    private var mapLayers: MutableList<SlamWareBaseView<MapView>> = CopyOnWriteArrayList()
    private var mPngMapView: PngMapView? = null //png地图
    private var mLegendView: LegendView? = null//图例
    private var mMapNameView: MapNameView? = null//图例

    var mWallView: VirtualWallView? = null//虚拟墙
    var mCrossView: CrossDoorView? = null//过门
    var mHomeDockView: HomeDockView? = null//充电站
    var mElevatorView: ElevatorView? = null//乘梯点
    var mStationView: StationsView? = null//站点
    var mRFIDView: RFIDView? = null //RFID
    var mOnlinePoseView: OnlinePoseView? = null//上线点
    var mUpLaserScanView: UpLaserScanView<MapView>? = null//上激光点云
    var mDownLaserScanView: DownLaserScanView? = null//下激光点云
    var mTopViewPathView: TopViewPathView? = null//顶视路线
    var mRemoveNoiseView: RemoveNoiseView? = null//噪点擦出
    var mPostingAreasView: PostingAreasView? = null//定位区域
    var mPolygonEditView: PolygonEditView? = null//区域
    var mSpPolygonEditView: SpPolygonEditView? = null//特殊区域
    var mMixAreaView: MixAreaView? = null//混行区域
    var mWorldPadView: WorldPadView? = null//路线PP
    var mPathView: PathView? = null//路线PP 接收PP返回的路线
    var mRobotView: RobotView? = null //机器人图标
    var mWorkIngPathView: WorkIngPathView? = null //机器人工作路径
    var mDragPositioningView: DragPositioningView? = null //拖拽定位view
    var mReflectMapView: ReflectMapView? = null //反光板地图view
    var mInspectionView: InspectionView? = null //巡检点

    /**
     * 获取地图位图宽度
     */
    fun getPngBitmapWidth(): Int {
        return mPngMapView?.getBitmapWidth() ?: 0
    }

    /**
     * 获取地图位图高度
     */
    fun getPngBitmapHeight(): Int {
        return mPngMapView?.getBitmapHeight() ?: 0
    }

    /**
     * 判断屏幕坐标(x,y)是否在地图图片范围内
     */
    fun isInsideMap(x: Float, y: Float): Boolean {
        val pngMapView = mPngMapView ?: return false
        val width = pngMapView.getBitmapWidth()
        val height = pngMapView.getBitmapHeight()
        if (width <= 0 || height <= 0) return false

        val invertMatrix = Matrix()
        mOuterMatrix.invert(invertMatrix)

        val points = floatArrayOf(x, y)
        invertMatrix.mapPoints(points)
        val mapX = points[0]
        val mapY = points[1]

        return mapX >= 0 && mapX <= width && mapY >= 0 && mapY <= height
    }

    /**
     * *************** 监听器   start ***********************
     */

    private var mSingleTapListener: ISingleTapListener? = null
    private var mGestureDetector: SlamGestureDetector? = null

    //删除噪点
//    private var mRemoveNoiseListener: IRemoveNoiseListener? = null

    /**
     * 旋转弧度
     */
//    override var rotationRadians = 0f

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

    // 机器人位姿 [x, y, theta(rad), z, roll, pitch]
    override val robotPose = FloatArray(6)

    /**
     * 设置各个View显示
     */
    private fun setViewVisibility(attrs: AttributeSet?) {
        attrs?.let {
            context.withStyledAttributes(it, R.styleable.MapView) {
                //显示图例
                mLegendView?.visibility = if (getBoolean(
                        R.styleable.MapView_showLegendView, true
                    )
                ) VISIBLE else GONE
                //显示地图名称呢
                mMapNameView?.visibility = if (getBoolean(
                        R.styleable.MapView_showMapNameView, true
                    )
                ) VISIBLE else GONE

            }
        }
    }

    private fun setDefaultBackground(colorId: Int) = setBackgroundColor(colorId)


    private fun initView() {
        val lp =
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        mPngMapView = PngMapView(context)
        mWallView = VirtualWallView(context, mMapView)
        mCrossView = CrossDoorView(context, mMapView)
        mHomeDockView = HomeDockView(context, mMapView)
        mElevatorView = ElevatorView(context, mMapView)
        mStationView = StationsView(context, mMapView)
        mRFIDView = RFIDView(context, mMapView)
        mOnlinePoseView = OnlinePoseView(context, mMapView)
        mUpLaserScanView = UpLaserScanView(context, mMapView)
        mDownLaserScanView = DownLaserScanView(context, mMapView)
        mTopViewPathView = TopViewPathView(context, mMapView)
        mLegendView = LegendView(context, attrs, mMapView)
        mMapNameView = MapNameView(context, mMapView)
        mRobotView = RobotView(context, mMapView)
        mWorkIngPathView = WorkIngPathView(context, mMapView)
        mRemoveNoiseView = RemoveNoiseView(context, mMapView)
        mPostingAreasView = PostingAreasView(context, mMapView)
        mPolygonEditView = PolygonEditView(context, mMapView)
        mSpPolygonEditView = SpPolygonEditView(context, mMapView)
        mMixAreaView = MixAreaView(context, mMapView)
        mPathView = PathView(context, mMapView)
        mWorldPadView = WorldPadView(context, mMapView)
        mDragPositioningView = DragPositioningView(context, mMapView)
        mReflectMapView = ReflectMapView(context, mMapView)
        mInspectionView = InspectionView(context, mMapView)
        //底图的View
        addView(mPngMapView, lp)

        //清扫区域
        addMapLayers(mPolygonEditView)
        //充电站
        addMapLayers(mHomeDockView)
        //乘梯点
        addMapLayers(mElevatorView)
        //显示避让点
        addMapLayers(mStationView)
        //RFID
        addMapLayers(mRFIDView)
        //上线点
        addMapLayers(mOnlinePoseView)
        //上激光点云
        addMapLayers(mUpLaserScanView)
        //下激光点云
        addMapLayers(mDownLaserScanView)
        //拖拽定位
        addMapLayers(mDragPositioningView)
        //顶视路线
        addMapLayers(mTopViewPathView)
        //显示虚拟墙
        addMapLayers(mWallView)
        //噪点擦除去
        addMapLayers(mRemoveNoiseView)
        //定位区域
        addMapLayers(mPostingAreasView)
        //特殊区域
        addMapLayers(mSpPolygonEditView)
        //混行区域
        addMapLayers(mMixAreaView)
        //显示路线
        addMapLayers(mPathView)
        //显示路线PP
        addMapLayers(mWorldPadView)
        //显示工作路径
        addMapLayers(mWorkIngPathView)
        //新过门
        addMapLayers(mCrossView)
        //反光板地图
        addMapLayers(mReflectMapView)
        //巡检
        addMapLayers(mInspectionView)
        //地图名称
        addView(mMapNameView)
        //机器人图标
        addMapLayers(mRobotView)
        //修改LegendView的布局参数，使其显示在右上角（在地图名称下边）
        addView(
            mLegendView, LayoutParams(
                LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = android.view.Gravity.TOP or android.view.Gravity.END
                setMargins(16, 16, 16, 16)
            })


        setCentred()
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val point = screenToWorld(event.x, event.y)
        mMapNameView?.setScreen(point)

        // 如果是擦除噪点模式、创建定位区域模式、编辑定位区域模式、删除定位区域模式、编辑清扫区域模式或创建清扫区域模式，或者路径编辑模式，或者创建路径模式
        if (currentWorkMode == WorkMode.MODE_REMOVE_NOISE || currentWorkMode == WorkMode.MODE_POSITING_AREA_ADD || currentWorkMode == WorkMode.MODE_POSITING_AREA_EDIT || currentWorkMode == WorkMode.MODE_POSITING_AREA_DELETE || currentWorkMode == WorkMode.MODE_SP_AREA_EDIT || currentWorkMode == WorkMode.MODE_MIX_AREA_ADD || currentWorkMode == WorkMode.MODE_SP_AREA_EDIT || currentWorkMode == WorkMode.MODE_MIX_AREA_EDIT || currentWorkMode == WorkMode.MODE_PATH_EDIT || currentWorkMode == WorkMode.MODE_PATH_CREATE || currentWorkMode == WorkMode.WORK_MODE_ADD_REFLECTOR_AREA || currentWorkMode == WorkMode.WORK_MODE_EDIT_REFLECTOR) {
            // 让事件传递给子视图（如RemoveNoiseView、PostingAreasView或PathView）处理
            // 先调用父类的onTouchEvent让事件传递给子视图
            super.onTouchEvent(event)
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
        val screenToWorld = screenToWorld(event.x, event.y)
        mMapCenterX = screenToWorld.x
        mMapCenterY = screenToWorld.y
        mSingleTapListener?.onSingleTapListener(mMapScale, screenToWorld)
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
        }
    }

    private fun setMatrixWithRotation(matrix: Matrix, rotation: Float) {
        mOuterMatrix = matrix
        mPngMapView?.setMatrix(matrix)
        for (mapLayer in mapLayers) {
            mapLayer.setMatrixWithRotation(matrix, rotation)
        }
    }

    fun setCentred(isCentred: Boolean = true) {
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
                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                            mapView.viewTreeObserver.removeOnGlobalLayoutListener(this)
                        } else {
                            mapView.viewTreeObserver.removeGlobalOnLayoutListener(this)
                        }
                        mapView.setCentred(isCentred)
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
            if (isCentred) {
                mMapScale = scale
                mOuterMatrix = Matrix()
                mOuterMatrix.postScale(mMapScale, mMapScale)
                mOuterMatrix.postTranslate(
                    (VIEW_WIDTH - mMapScale * iWidth) / 2, (VIEW_HEIGHT - mMapScale * iHeight) / 2
                )
                setMatrixWithScaleAndRotation(mOuterMatrix, mMapScale, 0f)
            }
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
    override fun worldToScreen(x: Float, y: Float): PointF {
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
     * 刷新所有层数据
     */
    override fun invalidate() {
        super.postInvalidate()
        mPngMapView?.postInvalidate()
        for (mapLayer in mapLayers) {
            mapLayer.postInvalidate()
        }
    }

    private fun addMapLayers(mapLayer: SlamWareBaseView<MapView>?) {
        if (mapLayer != null && !mapLayers.contains(mapLayer)) {
            mapLayers.add(mapLayer)
            addView(
                mapLayer, LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
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
        mWallView = null
        mHomeDockView = null
        mElevatorView = null
        mStationView = null
        mRFIDView = null
        mOnlinePoseView = null
        mLegendView = null
        mUpLaserScanView = null
        mDownLaserScanView = null
        mTopViewPathView = null
        mRemoveNoiseView = null
        mPostingAreasView = null
        mPolygonEditView = null
        mSpPolygonEditView = null
        mMixAreaView = null
        mPathView = null
        mRobotView = null
        mWorkIngPathView = null
        mMapNameView = null
        mCrossView = null
        mDragPositioningView = null
        mReflectMapView = null

        // 清理监听器
        mSingleTapListener = null
        mGestureDetector = null
//        mRemoveNoiseListener = null

        // 清理矩阵和其他对象
        mOuterMatrix = Matrix()
    }


    /**
     * ******************************************************
     * *******************      外部接口        **************
     * ******************************************************
     */

    /**
     * 手动处理手势事件（供子View调用，以支持特定模式下的地图缩放等）
     */
    fun processMapGestures(event: MotionEvent) {
        mGestureDetector?.onTouchEvent(event, this)
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        currentWorkMode = mode
        // 安全地传递工作模式给各个视图，避免空指针异常
        mWallView?.setWorkMode(mode)
        mStationView?.setWorkMode(mode)
        mRFIDView?.setWorkMode(mode)
        mRemoveNoiseView?.setWorkMode(mode)
        mPostingAreasView?.setEditMode(mode)
        mPolygonEditView?.setWorkMode(mode)
        mSpPolygonEditView?.setWorkMode(mode)
        mMixAreaView?.setWorkMode(mode)
        mElevatorView?.setWorkMode(mode)
        mHomeDockView?.setWorkMode(mode)
        mWorldPadView?.setWorkMode(mode)
        mCrossView?.setWorkMode(mode)
        mDragPositioningView?.setWorkMode(mode)
        mReflectMapView?.setWorkMode(mode)
        mInspectionView?.setWorkMode(mode)
    }

    /**
     * 设置地图状态（固定缩放级别和固定位置）
     * @param scale 缩放级别
     * @param centerX 世界坐标X，将置于视图中心
     * @param centerY 世界坐标Y，将置于视图中心
     * @param rotation 旋转角度（度）
     */
    fun setMapStatus(
        scale: Float = mMapScale,
        centerX: Float = mMapCenterX,
        centerY: Float = mMapCenterY,
        rotation: Float = 0f
    ) {
        if (VIEW_WIDTH == 0 || VIEW_HEIGHT == 0) return

        var finalScale = scale
        // 限制缩放范围
        if (finalScale > mMaxMapScale) finalScale = mMaxMapScale
        if (finalScale < mMinMapScale) finalScale = mMinMapScale

        // 更新成员变量
        mMapScale = finalScale
        mMapCenterX = centerX
        mMapCenterY = centerY

        // 获取地图像素坐标
        val mapPixelPoint = synchronized(mSrf.mapData) {
            mSrf.worldToScreen(mMapCenterX, mMapCenterY)
        }

        val matrix = Matrix()
        // 1. 将目标点移动到原点
        matrix.postTranslate(-mapPixelPoint.x, -mapPixelPoint.y)
        // 2. 缩放
        matrix.postScale(finalScale, finalScale)
        // 3. 旋转
        matrix.postRotate(rotation)
        // 4. 移动到视图中心
        matrix.postTranslate(VIEW_WIDTH / 2f, VIEW_HEIGHT / 2f)

        // 更新矩阵
        setMatrixWithScaleAndRotation(matrix, finalScale, rotation)
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
     * 加载地图
     * pngPath png文件路径
     * yamlPath yaml文件路径
     */
    fun reloadMap(pngPath: String, yamlPath: String) {
        val file = File(pngPath)
        Glide.with(this).asBitmap().load(file).skipMemoryCache(true)
            .diskCacheStrategy(DiskCacheStrategy.NONE).into(object : SimpleTarget<Bitmap?>() {
                override fun onResourceReady(
                    resource: Bitmap, transition: Transition<in Bitmap?>?
                ) {
                    // 1. 保存当前视图状态
                    var currentScale = mMapScale
                    var currentCenterX = mMapCenterX
                    var currentCenterY = mMapCenterY
                    var currentRotation = 0f
                    var hasSavedState = false
                    val oldRes = synchronized(mSrf.mapData) { mSrf.mapData.resolution }

                    if (VIEW_WIDTH > 0 && VIEW_HEIGHT > 0) {
                        try {
                            val center = screenToWorld(VIEW_WIDTH / 2f, VIEW_HEIGHT / 2f)
                            currentCenterX = center.x
                            currentCenterY = center.y

                            val values = FloatArray(9)
                            mOuterMatrix.getValues(values)
                            // 计算旋转角度
                            val skewY = values[Matrix.MSKEW_Y]
                            val scaleX = values[Matrix.MSCALE_X]
                            currentRotation =
                                Math.toDegrees(Math.atan2(skewY.toDouble(), scaleX.toDouble()))
                                    .toFloat()

                            hasSavedState = true
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }

                    val mPngMapData = YamlNew().loadYaml(
                        yamlPath,
                        resource.height.toFloat(),
                        resource.width.toFloat(),
                    )

                    // 2. 设置新地图数据
                    setBitmap(mPngMapData, resource, false)

                    // 3. 恢复视图状态
                    if (hasSavedState) {
                        // 如果分辨率发生变化，调整缩放比例以保持视觉一致性
                        val newRes = mPngMapData.resolution
                        if (oldRes > 0 && newRes > 0 && oldRes != newRes) {
                            currentScale = currentScale * (newRes / oldRes)
                        }
                        setMapStatus(currentScale, currentCenterX, currentCenterY, currentRotation)
                    } else {
                        setMapStatus()
                    }
                }
            })
    }

    /**
     * 设置地图数据信息
     * 设置地图
     *
     * @param bitmap
     */
    private fun setBitmap(mapData: MapData, bitmap: Bitmap, isCentred: Boolean = true) {
        synchronized(mSrf.mapData) {
            mSrf.mapData.width = mapData.width
            mSrf.mapData.height = mapData.height
            mSrf.mapData.originX = mapData.originX
            mSrf.mapData.originY = mapData.originY
            mSrf.mapData.resolution = mapData.resolution
        }

        mMapCenterX = mapData.originX + (mapData.width * mapData.resolution) / 2
        mMapCenterY = mapData.originY + (mapData.height * mapData.resolution) / 2

        mPngMapView?.setBitmap(bitmap)
        // 设置地图后自动居中显示
        setCentred(isCentred)
    }

    /**
     * 设置路径属性编辑回调监听器
     */
    fun setOnPathAttributeEditListener(listener: WorldPadView.OnPathAttributeEditListener) {
        mWorldPadView?.setOnPathAttributeEditListener(listener)
    }

    /**
     * 设置World数据到PathView2
     */
    fun setLayer(cLayer: CLayer) {
        mWorldPadView?.setLayer(cLayer)
    }

    /**
     * 获取当前的CLayer对象，用于保存路径数据
     */
    fun getLayer(): CLayer? {
        return mWorldPadView?.getLayer()
    }


    /***
     * 设置地图显示
     */
    fun setSlamMapViewShow(visibility: Int) {
        mPngMapView!!.visibility = visibility
    }

    /**
     * 设置当前地图名称
     */
    fun setMapName(name: String) {
        mMapNameView?.setMapName(name)
    }


    /**
     * 显示虚拟墙
     * 设置虚拟墙数据
     */
    fun setVirtualWall(virtualWall: VirtualWallNew) {
        mWallView?.setVirtualWall(virtualWall)
    }

    /**
     * 获取虚拟墙数据
     */
    fun getVirtualWall() = mWallView?.getVirtualWall()


    /**
     * 设置上激光点云数据源
     */
    fun setUpLaserScan(laser: laser_t) = mUpLaserScanView?.updateUpLaserScan(laser)

    /**
     * 设置下激光点云数据源
     */
    fun setDownLaserScan(laser: laser_t) = mDownLaserScanView?.updateDownLaserScan(laser)

    /**
     * 设置拖拽定位数据（上激光点云数据源）
     */
    fun setDragPositionData(laser: laser_t) = mDragPositioningView?.updateUpLaserScan(laser)

    /**
     * 获取拖拽定位车体数据y
     */
    fun getDragRobotPose() = mDragPositioningView?.dragRobotPose


    /**
     * 设置反光板地图
     */
    fun setReflectorMap(list: MutableList<ReflectorMapBean>) =
        mReflectMapView?.setReflectorMap(list)

    /**
     * 获取反光板数据
     */
    fun getReflectorMap(): MutableList<ReflectorMapBean> =
        mReflectMapView?.getData() ?: mutableListOf()

    /**
     * 清除反光板地图
     */
    fun cleanReflector() = mReflectMapView?.cleanReflector()

    /**
     * 设置AGV 位姿 机器人图标的实时位置
     */
    @SuppressLint("SuspiciousIndentation")
    fun setAgvPose(rt: robot_control_t) {
        val dParams = rt.dparams
        mMapNameView?.setAgvX(dParams[0])
        mMapNameView?.setAgvY(dParams[1])
        mMapNameView?.setAgvT(dParams[2])
        if (dParams.size > 8) {
            mMapNameView?.setAgvZ(dParams[8])
        }
        mRobotView?.setAgvData(dParams)
    }


    private var num = 0

    // 重用PointF对象，减少内存抖动
    private val mCarPoint = PointF()

    /**
     * 机器人有任务状态下行走的路径
     */
    @SuppressLint("DefaultLocale")
    fun setWorkingPath(array: DoubleArray) {
        num++
        if (num % 3 == 0) {
            // 重用对象，避免频繁创建新对象
            mCarPoint.x = String.format("%.1f", array[0]).toFloat()
            mCarPoint.y = String.format("%.1f", array[1]).toFloat()
            mWorkIngPathView?.setData(mCarPoint)
            num = 0
        }
    }

    /**
     * 设置顶视路线
     */
    fun setTopViewPathDada(data: MutableList<MergedPoseItem>) {
        mTopViewPathView?.setTopViewPath(data)
    }


    /**
     * 设置上线点
     */
    fun setInitPoseList(data: MutableList<InitPose>) {
        mOnlinePoseView?.setInitPoses(data)
    }


    /**
     * 设置充电站
     */
    fun setMachineStation(machineStation: MachineStation?) {
        mHomeDockView?.setHomePose(machineStation)
    }

    /**
     * 获取充电站
     */
    fun getMachineStation(): MachineStation? = mHomeDockView?.getData()


    /**
     * 设置避让点
     */
    fun setCmsStations(list: MutableList<CmsStation>?) {
        mStationView?.setCmsStations(list)
    }

    fun setRFId(list: MutableList<RFID>) {
        mRFIDView?.setRFIds(list)
    }

    /**
     * 设置乘梯点
     */
    fun setElevators(list: MutableList<ElevatorPoint>?) {
        mElevatorView?.setElevators(list)
    }

    /**
     * 获取设置乘梯点
     */
    fun getElevators(): MutableList<ElevatorPoint>? {
        return mElevatorView?.getElevators()
    }


    /**
     * 设置清扫区域数据源
     */
    fun setCleanAreaData(data: MutableList<CleanAreaNew>) {
        mPolygonEditView?.setCleanAreaData(data)
    }

    /**
     * 设置选中的清扫区域
     */
    fun setSelectedArea(area: CleanAreaNew?) {
        mPolygonEditView?.setSelectedArea(area)
    }

    /**
     * 创建清扫区域
     */
    fun createCleanArea(newArea: CleanAreaNew) {
        mPolygonEditView?.createRectangularAreaAtCenter(newArea)
    }

    /**
     * 获取清扫区域
     */
    fun getCleanAreaData(): List<CleanAreaNew> = mPolygonEditView?.getData() ?: mutableListOf()

    /**
     * 设置特殊区域
     */
    fun setSpAreaData(data: MutableList<SpArea>) {
        mSpPolygonEditView?.setSpAreaData(data)
    }

    /**
     * 设置选中的特殊区域
     */
    fun setSelectedSpArea(area: SpArea?) {
        mSpPolygonEditView?.setSelectedArea(area)
    }

    /**
     * 创建特殊区域
     */
    fun createSpArea(newArea: SpArea) {
        mSpPolygonEditView?.createRectangularAreaAtCenter(newArea)
    }

    /**
     * 获取特殊区域
     */
    fun getSpAreaData(): MutableList<SpArea> = mSpPolygonEditView?.getData() ?: mutableListOf()


    /**
     * 设置定位区域
     */
    fun setPositingAreas(list: MutableList<PositingArea>?) =
        mPostingAreasView?.setPositingAreas(list)

    /**
     * 获取定位区域
     */
    fun getPositingAreas(): List<PositingArea> = mPostingAreasView?.getData() ?: mutableListOf()

    /**
     * 设置选中的定位区域
     */
    fun setSelectedPositingArea(area: PositingArea?) {
        mPostingAreasView?.setSelectedArea(area)
    }

    /**
     * 删除指定的定位区域
     */
    fun deletePositingArea(area: PositingArea?) {
        area?.let {
            mPostingAreasView?.deletePositingArea(it)
        }
    }

    /**
     * 设置混行区域
     */
    fun setMixAreaData(data: MutableList<WorkAreasNew>) {
        mMixAreaView?.setMixAreaData(data)
    }


    /**
     * 获取混行区域
     */
    fun getMixAreaData(): List<WorkAreasNew> = mMixAreaView?.getData() ?: mutableListOf()

    /**
     * 创建混行区域
     */
    fun createMixArea(newArea: WorkAreasNew) {
        mMixAreaView?.createRectangularAreaAtCenter(newArea)
    }

    /**
     * 设置选中的混行区域
     */
    fun setSelectedMixArea(area: WorkAreasNew?) {
        mMixAreaView?.setSelectedArea(area)
    }


    /**
     * 设置试教点
     */
    fun setTeachPoint(point: TeachPoint) = mPathView?.setTeachPoint(point)


    /**
     * 外部接口: 创建连续的示教路径
     */
    fun createContinuousPathTeach(pptKeyList: List<Array<Point2d>>, pathParam: Short) {
        var lastEndNodeId = -1
        for (pptKey in pptKeyList) {
            if (pptKey.isNotEmpty()) {
                val m_KeyPst = DefPosture()
                for (point2d in pptKey) {
                    val pst = Posture()
                    pst.x = point2d.x
                    pst.y = point2d.y
                    pst.fThita = 0f
                    m_KeyPst.AddPst(pst)
                }
                // 使用前一条路径的终点作为当前路径的起点
                lastEndNodeId =
                    mWorldPadView?.createTeachPath(pptKey, m_KeyPst, pathParam, lastEndNodeId) ?: -1
            }
        }
    }

    /**
     * 设置清扫路径
     */
    fun setCleanPathPlanResultBean(pathPlanResultBean: PathPlanResultBean?) =
        mPathView?.setCleanPathPlanResultBean(pathPlanResultBean)


    /**
     * 设置全局路径
     */
    fun setGlobalPathPlanResultBean(pathPlanResultBean: PathPlanResultBean?) =
        mPathView?.setGlobalPathPlanResultBean(pathPlanResultBean)


    /**
     * 根据ID删除定位区域
     */
    fun deletePositingAreaById(areaId: Long?) {
        areaId?.let {
            mPostingAreasView?.deletePositingAreaById(it)
        }
    }


    /**
     * 添加虚拟墙
     * @param config 虚拟墙类型：1-重点虚拟墙，2-虚拟门，3-普通虚拟墙
     */
    fun addVirtualWall(config: Int) {
        mWallView?.addVirtualWall(config)
    }

    /**
     * 确认编辑虚拟墙
     */
    fun confirmEditVirtualWall() {
        mWallView?.confirmEditVirtualWall()
    }

    /**
     * 清除噪点擦除视图中绘制的矩形线框
     */
    fun clearRemoveNoiseDrawing() {
        mRemoveNoiseView?.clearDrawing()
    }

    /**
     * 获取所有绘制的噪点区域(世界坐标)
     */
    fun getRemoveNoiseRects(): List<RectF> {
        val rects = mRemoveNoiseView?.getRects() ?: ArrayList()
        return rects
    }

    /**
     * 清除清扫路径
     */
    fun clearCleanPathPlan() = mPathView?.setCleanPathPlanResultBean(null)

    /**
     * 清除全局路径
     */
    fun clearGlobalPathPlan() = mPathView?.setGlobalPathPlanResultBean(null)

    /**
     * 清除所有路径
     */
    fun clearPathPlan() = mPathView?.clearPathPlan()

    /**
     * 清空有任务下的路线
     */
    fun clearCarPath() = mWorkIngPathView?.clearCarPath()


    /**
     * 清除清扫区域
     */
    fun cleanCleanArea() = mPolygonEditView?.cleanData()

    /**
     * 设置过门
     */

    fun addCrossDoor(crossDoor: CrossDoor) = mCrossView?.addCrossDoor(crossDoor)

    /**
     * 设置多个过门
     */
    fun addCrossDoors(crossDoors: List<CrossDoor>) = mCrossView?.addCrossDoors(crossDoors)

    /**
     * 获取车体实时坐标
     */
    fun getAgvData(): DoubleArray? = mRobotView?.getAgvData()

    /**
     * 获取巡检点数据源
     */
    fun setInspectionViewStations(list: MutableList<Inspection>) =
        mInspectionView?.setInspectionViewStations(list)

    /**
     * 获取巡检点数据源
     */
    fun getInspectionViewStations(): MutableList<Inspection> =
        mInspectionView?.getInspectionViewStations()!!

    /**
     * 是否显示下点云
     */
    fun showBottomLaser(isShow: Boolean) {
        mLegendView?.setShowBottomLaser(isShow)
    }

    /**
     * 销毁
     */
    fun destroy() {
    }
    /**
     * ******************************************************
     * *******************      监听接口        **************
     * ******************************************************
     */


    /**
     * 设置过门删除监听器
     */
    fun setOnCrossDoorLineClickListener(listener: CrossDoorView.OnCrossDoorLineClickListener?) {
        mCrossView?.setOnCrossDoorLineClickListener(listener)
    }

    /**
     * 设置过门监听器
     */
    fun setOnCrossDoorDeleteClickListener(listener: CrossDoorView.OnCrossDoorDeleteClickListener?) {
        mCrossView?.setOnCrossDoorDeleteClickListener(listener)
    }

    /**
     * 设置定位区域编辑监听器
     */
    fun setOnPositingAreaEditedListener(listener: PostingAreasView.OnPositingAreaEditedListener?) {
        mPostingAreasView?.setOnPositingAreaEditedListener(listener)

    }

    /**
     * 设置定位区域删除监听器
     */
    fun setOnPositingAreaDeletedListener(listener: PostingAreasView.OnPositingAreaDeletedListener?) {
        mPostingAreasView?.setOnPositingAreaDeletedListener(listener)
    }

    /**
     * 设置定位区域创建监听器
     */
    fun setOnPositingAreaCreatedListener(listener: PostingAreasView.OnPositingAreaCreatedListener?) {
        mPostingAreasView?.setOnPositingAreaCreatedListener(listener)
    }


    /**
     * 设置清扫区域编辑监听器
     */
    fun setOnCleanAreaEditListener(listener: PolygonEditView.OnCleanAreaEditListener?) {
        mPolygonEditView?.setOnCleanAreaEditListener(listener)
    }

    /**
     * 确认删除清扫区域的顶点
     */
    fun performDeleteVertex(area: CleanAreaNew, vertexIndex: Int) {
        mPolygonEditView?.performDeleteVertex(area, vertexIndex)
    }

    /**
     * 设置特殊域编辑监听器
     */
    fun setOnSpAreaEditListener(listener: SpPolygonEditView.OnSpAreaEditListener?) {
        mSpPolygonEditView?.setOnSpAreaEditListener(listener)
    }

    /**
     * 设置混行区域编辑监听器
     */
    fun setOnMixAreaEditListener(listener: MixAreaView.OnMixAreaEditListener?) {
        mMixAreaView?.setOnMixAreaEditListener(listener)
    }

    /**
     * 设置避让点点击监听器
     */
    fun setOnStationClickListener(listener: StationsView.OnStationClickListener) =
        mStationView?.setOnStationClickListener(listener)

    /**
     * 设置避让点删除监听器
     */
    fun setOnStationDeleteListener(listener: StationsView.OnStationDeleteListener) =
        mStationView?.setOnStationDeleteListener(listener)


    /**
     * 设置巡检点点击监听器
     */
    fun setOnInspectionStationClickListener(listener: InspectionView.OnStationClickListener) =
        mInspectionView?.setOnStationClickListener(listener)

    /**
     * 设置巡检点删除监听器
     */
    fun setOnInspectionStationDeleteListener(listener: InspectionView.OnStationDeleteListener) =
        mInspectionView?.setOnStationDeleteListener(listener)

    /**
     * 设置RFId点击监听器
     */
    fun setOnRFIdClickListener(listener: RFIDView.OnRFIdClickListener) =
        mRFIDView?.setOnRFIdClickListener(listener)

    /**
     * 设置RFId删除监听
     */
    fun setOnRFIdDeleteListener(listener: RFIDView.OnRFIdDeleteListener) =
        mRFIDView?.setOnRFIdDeleteClickListener(listener)

    /**
     * 设置乘梯点编辑监听器
     */
    fun setOnElevatorEditListener(listener: ElevatorView.OnElevatorEditListener?) =
        mElevatorView?.setOnElevatorEditListener(listener)

    /**
     * 设置乘梯点删除监听器
     */
    fun setOnElevatorDeleteListener(listener: ElevatorView.OnElevatorDeleteListener?) =
        mElevatorView?.setOnElevatorDeleteListener(listener)

    /**
     * 设置充电站点击监听器
     */
    fun setOnMachineStationClickListener(listener: HomeDockView.OnMachineStationClickListener?) {
        mHomeDockView?.setOnMachineStationClickListener(listener)
    }

    /**
     * 设置充电站删除监听器
     */
    fun setOnMachineStationDeleteListener(listener: HomeDockView.OnMachineStationDeleteListener?) {
        mHomeDockView?.setOnMachineStationDeleteListener(listener)
    }

    /**
     * 设置虚拟墙点击监听器
     */
    fun setOnVirtualWallClickListener(listener: VirtualWallView.OnVirtualWallClickListener) {
        mWallView?.setOnVirtualWallClickListener(listener)
    }

    /**
     * 更新虚拟墙类型
     * @param lineIndex 虚拟墙索引
     * @param newConfig 新的虚拟墙类型配置
     */
    fun updateVirtualWallType(lineIndex: Int, newConfig: Int) {
        mWallView?.updateVirtualWallType(lineIndex, newConfig)
    }

    /**
     * 设置是否是3D模式
     */
    fun set3D(is3D: Boolean) {
        mRemoveNoiseView?.set3D(is3D)
    }

//    /**
//     * 设置擦除噪点监听器
//     */
//    fun setOnRemoveNoiseListener(listener: IRemoveNoiseListener?) {
//        mRemoveNoiseListener = listener
//        mRemoveNoiseView?.setOnRemoveNoiseListener(object : RemoveNoiseView.OnRemoveNoiseListener {
//            override fun onRemoveNoise(leftTop: PointF, rightBottom: PointF) {
//                // 将屏幕坐标转换为世界坐标
//                val worldLeftTop = screenToWorld(leftTop.x, leftTop.y)
//                val worldRightBottom = screenToWorld(rightBottom.x, rightBottom.y)
//                // 使用弱引用的监听器
//                mRemoveNoiseListener?.onRemoveNoise(worldLeftTop, worldRightBottom)
//            }
//
//            override fun onRemoveNoiseDeleted(rect: RectF) {
//                // 将屏幕坐标转换为世界坐标
//                val worldLeftTop = screenToWorld(rect.left, rect.top)
//                val worldRightBottom = screenToWorld(rect.right, rect.bottom)
//                // 创建世界坐标系的矩形
//                val worldRect = RectF(worldLeftTop.x, worldLeftTop.y, worldRightBottom.x, worldRightBottom.y)
//                // 确保 rect 是标准化的（left < right, top < bottom），如果需要的话。
//                // screenToWorld转换后坐标系可能变化，这里直接传递转换后的点构成的矩形
//                mRemoveNoiseListener?.onRemoveNoiseDeleted(worldRect)
//            }
//        })
//    }

    /**
     * 手指抬起监听 回调是世界坐标
     */
    fun setSingleTapListener(listener: ISingleTapListener?) {
        mSingleTapListener = listener
    }


    interface ISingleTapListener {
        fun onSingleTapListener(mMapScale: Float, point: PointF)
    }


//    /**
//     * 擦除噪点监听器接口
//     */
//    interface IRemoveNoiseListener {
//        /**
//         * 当用户完成噪点擦除操作时调用
//         * @param leftTop 矩形左上角的世界坐标
//         * @param rightBottom 矩形右下角的世界坐标
//         */
//        fun onRemoveNoise(leftTop: PointF, rightBottom: PointF)
//
//        /**
//         * 当用户删除已绘制的噪点区域时调用
//         * @param rect 被删除的矩形（世界坐标）
//         */
//        fun onRemoveNoiseDeleted(rect: RectF)
//    }


}
