package com.siasun.dianshi.view

import VirtualWallNew
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.FrameLayout
import android.widget.ImageView
import com.ngu.lcmtypes.laser_t
import com.ngu.lcmtypes.robot_control_t
import com.siasun.dianshi.bean.CleanAreaNew
import com.siasun.dianshi.bean.CmsStation
import com.siasun.dianshi.bean.ElevatorPoint
import com.siasun.dianshi.bean.InitPose
import com.siasun.dianshi.bean.MachineStation
import com.siasun.dianshi.bean.MapData
import com.siasun.dianshi.bean.MergedPoseItem
import com.siasun.dianshi.bean.Point2d
import com.siasun.dianshi.bean.pp.PathPlanResultBean
import com.siasun.dianshi.bean.PositingArea
import com.siasun.dianshi.bean.SpArea
import com.siasun.dianshi.bean.TeachPoint
import com.siasun.dianshi.bean.WorkAreasNew
import com.siasun.dianshi.bean.pp.DefPosture
import com.siasun.dianshi.bean.pp.Posture
import com.siasun.dianshi.utils.CoordinateConversion
import com.siasun.dianshi.utils.MathUtils
import com.siasun.dianshi.utils.RadianUtil
import com.siasun.dianshi.utils.SlamGestureDetector
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 地图画布
 * 将在此画布中绘制slam的png地图
 */
class MapView(context: Context, private val attrs: AttributeSet) : FrameLayout(context, attrs),
    SlamGestureDetector.OnRPGestureListener {

    // 工作模式枚举
    enum class WorkMode {
        MODE_SHOW_MAP,         // 移动地图模式
        MODE_VIRTUAL_WALL_ADD, // 创建虚拟墙模式
        MODE_VIRTUAL_WALL_EDIT,// 编辑虚拟墙模式
        MODE_VIRTUAL_WALL_DELETE, // 删除虚拟墙模式
        MODE_CMS_STATION_EDIT,  // 修改避让点模式
        MODE_CMS_STATION_DELETE, // 删除避让点模式
        MODE_ELEVATOR_EDIT,    // 编辑乘梯点模式
        MODE_ELEVATOR_DELETE,  // 删除乘梯点模式
        MODE_MACHINE_STATION_EDIT,  // 编辑充电站模式
        MODE_MACHINE_STATION_DELETE, // 删除充电站模式
        MODE_REMOVE_NOISE,      // 擦除噪点模式
        MODE_POSITING_AREA_ADD, // 创建定位区域模式
        MODE_POSITING_AREA_EDIT, // 编辑定位区域模式
        MODE_POSITING_AREA_DELETE, // 删除定位区域模式
        MODE_CLEAN_AREA_EDIT, // 编辑清扫区域模式
        MODE_CLEAN_AREA_ADD, // 创建清扫区域模式
        MODE_SP_AREA_EDIT, // 编辑特殊区域模式
        MODE_SP_AREA_ADD, // 创建特殊区域模式
        MODE_MIX_AREA_ADD, // 创建混行区域模式
        MODE_MIX_AREA_EDIT, // 编辑混行区域模式
    }

    // 当前工作模式
    private var currentWorkMode = WorkMode.MODE_SHOW_MAP

    var mSrf = CoordinateConversion()//坐标转化工具类
    private var mOuterMatrix = Matrix()
    private var VIEW_WIDTH = 0 //视图宽度
    private var VIEW_HEIGHT = 0

    //视图高度
    private val defaultBackGroundColor = 0xC0C0C0 //默认背景
    private var mMapScale = 1f //地图缩放级别
    private val mMaxMapScale = 5f //最大缩放级别
    private var mMinMapScale = 0.1f //最小缩放级别

    private var mMapView: WeakReference<MapView> = WeakReference(this)
    private var mapLayers: MutableList<SlamWareBaseView> = CopyOnWriteArrayList()
    private var mPngMapView: PngMapView? = null //png地图
    private var mLegendView: LegendView? = null//图例

    var mWallView: VirtualWallView? = null//虚拟墙
    var mHomeDockView: HomeDockView? = null//充电站
    var mElevatorView: ElevatorView? = null//乘梯点
    var mStationView: StationsView? = null//站点
    var mOnlinePoseView: OnlinePoseView? = null//上线点
    var mUpLaserScanView: UpLaserScanView? = null//上激光点云
    var mDownLaserScanView: DownLaserScanView? = null//下激光点云
    var mTopViewPathView: TopViewPathView? = null//顶视路线
    var mRemoveNoiseView: RemoveNoiseView? = null//噪点擦出
    var mPostingAreasView: PostingAreasView? = null//定位区域
    var mPolygonEditView: PolygonEditView? = null//区域
    var mSpPolygonEditView: SpPolygonEditView? = null//特殊区域
    var mMixAreaView: MixAreaView? = null//混行区域
    var mPathView: PathView? = null//路线PP
    var mRobotView: RobotView? = null //机器人图标
    var mWorkIngPathView: WorkIngPathView? = null //机器人工作路径

    /**
     * *************** 监听器   start ***********************
     */

    // 使用弱引用存储监听器，避免内存泄漏
    private var mSingleTapListener: WeakReference<ISingleTapListener?>? = null
    private var mGestureDetector: SlamGestureDetector? = null

    //删除噪点
    private var mRemoveNoiseListener: IRemoveNoiseListener? = null


    /**
     * *************** 监听器   end ***********************
     */

    init {
        setDefaultBackground(defaultBackGroundColor)
        mOuterMatrix = Matrix()
        mGestureDetector = SlamGestureDetector(this, this)
        initView()
    }


    private fun setDefaultBackground(colorId: Int) = setBackgroundColor(colorId)


    private fun initView() {
        val lp =
            LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        mPngMapView = PngMapView(context)
        mWallView = VirtualWallView(context, mMapView)
        mHomeDockView = HomeDockView(context, mMapView)
        mElevatorView = ElevatorView(context, mMapView)
        mStationView = StationsView(context, mMapView)
        mOnlinePoseView = OnlinePoseView(context, mMapView)
        mUpLaserScanView = UpLaserScanView(context, mMapView)
        mDownLaserScanView = DownLaserScanView(context, mMapView)
        mTopViewPathView = TopViewPathView(context, mMapView)

        mLegendView = LegendView(context, attrs, mMapView)
        mRobotView = RobotView(context, mMapView)
        mWorkIngPathView = WorkIngPathView(context, mMapView)
        mRemoveNoiseView = RemoveNoiseView(context, mMapView)
        mPostingAreasView = PostingAreasView(context, mMapView)
        mPolygonEditView = PolygonEditView(context, mMapView)
        mSpPolygonEditView = SpPolygonEditView(context, mMapView)
        mMixAreaView = MixAreaView(context, mMapView)
        mPathView = PathView(context, mMapView)
        //底图的View
        addView(mPngMapView, lp)

        //充电站
        addMapLayers(mHomeDockView)
        //乘梯点
        addMapLayers(mElevatorView)
        //显示避让点
        addMapLayers(mStationView)
        //上线点
        addMapLayers(mOnlinePoseView)
        //上激光点云
        addMapLayers(mUpLaserScanView)
        //下激光点云
        addMapLayers(mDownLaserScanView)
        //顶视路线
        addMapLayers(mTopViewPathView)
        //机器人图标
        addMapLayers(mRobotView)
        //显示虚拟墙
        addMapLayers(mWallView)
        //噪点擦除去
        addMapLayers(mRemoveNoiseView)
        //定位区域
        addMapLayers(mPostingAreasView)
        //清扫区域
        addMapLayers(mPolygonEditView)
        //特殊区域
        addMapLayers(mSpPolygonEditView)
        //混行区域
        addMapLayers(mMixAreaView)
        //显示路线
        addMapLayers(mPathView)

        //  修改LegendView的布局参数，使其显示在右上角
        addView(mLegendView, LayoutParams(
            LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = android.view.Gravity.TOP or android.view.Gravity.END
            setMargins(16, 16, 16, 16)
        })

        setCentred()
    }


    override fun onTouchEvent(event: MotionEvent): Boolean {
        val point = screenToWorld(event.x, event.y)
        mLegendView?.setScreen(point)

        // 如果是擦除噪点模式、创建定位区域模式、编辑定位区域模式、删除定位区域模式、编辑清扫区域模式或创建清扫区域模式
        if (currentWorkMode == WorkMode.MODE_REMOVE_NOISE || currentWorkMode == WorkMode.MODE_POSITING_AREA_ADD || currentWorkMode == WorkMode.MODE_POSITING_AREA_EDIT || currentWorkMode == WorkMode.MODE_POSITING_AREA_DELETE || currentWorkMode == WorkMode.MODE_CLEAN_AREA_EDIT || currentWorkMode == WorkMode.MODE_CLEAN_AREA_ADD || currentWorkMode == WorkMode.MODE_SP_AREA_EDIT || currentWorkMode == WorkMode.MODE_MIX_AREA_ADD || currentWorkMode == WorkMode.MODE_SP_AREA_EDIT || currentWorkMode == WorkMode.MODE_MIX_AREA_EDIT) {
            // 让事件传递给子视图（如RemoveNoiseView或PostingAreasView）处理
            // 先调用父类的onTouchEvent让事件传递给子视图
            super.onTouchEvent(event)
            // 返回true表示事件已处理，禁止手势检测器处理，从而禁止底图拖动
            return true
        }

        // 非擦除噪点模式和非编辑定位区域模式，由手势检测器处理事件
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
        mOuterMatrix.postScale(factor, factor, cx, cy)
        setMatrixWithScale(mOuterMatrix, mMapScale)
    }

    private fun singleTap(event: MotionEvent) {
        mSingleTapListener?.get()?.onSingleTapListener(event)
    }

    private fun setMatrix(matrix: Matrix) {
        mPngMapView!!.setMatrix(matrix)
        for (mapLayer in mapLayers) {
            mapLayer.setMatrix(matrix)
        }
        postInvalidate()
    }

    private fun setMatrixWithScale(matrix: Matrix, scale: Float) {
        mOuterMatrix = matrix
        mMapScale = scale
        mPngMapView!!.setMatrix(matrix)
        for (mapLayer in mapLayers) {
            mapLayer.setMatrixWithScale(matrix, scale)
        }
    }

    private fun setMatrixWithScaleAndRotation(matrix: Matrix, scale: Float, rotation: Float) {
        mOuterMatrix = matrix
        mMapScale = scale
        mPngMapView!!.setMatrix(matrix)
        for (mapLayer in mapLayers) {
            mapLayer.setMatrixWithScale(matrix, scale)
            mapLayer.mRotation = rotation
        }
    }

    private fun setMatrixWithRotation(matrix: Matrix, rotation: Float) {
        mOuterMatrix = matrix
        mPngMapView!!.setMatrix(matrix)
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

    private fun addMapLayers(mapLayer: SlamWareBaseView?) {
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

        // 清理监听器
        mSingleTapListener = null
        mGestureDetector = null
        mRemoveNoiseListener = null

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
        // 安全地传递工作模式给各个视图，避免空指针异常
        mWallView?.setWorkMode(mode)
        mStationView?.setWorkMode(mode)
        mRemoveNoiseView?.setWorkMode(mode)
        mPostingAreasView?.setEditMode(mode)
        mPolygonEditView?.setWorkMode(mode)
        mSpPolygonEditView?.setWorkMode(mode)
        mMixAreaView?.setWorkMode(mode)
        mElevatorView?.setWorkMode(mode)
        mHomeDockView?.setWorkMode(mode)
    }

    /**
     * 获取当前工作模式
     */
    fun getCurrentWorkMode(): WorkMode {
        return currentWorkMode
    }

    /**
     * 设置地图数据信息
     * 设置地图
     *
     * @param bitmap
     */
    fun setBitmap(mapData: MapData, bitmap: Bitmap) {
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
     * 设置当前地图名称
     */
    fun setMapName(name: String) {
        mLegendView?.setMapName(name)
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
     * 设置AGV 位姿 机器人图标的实时位置
     */
    @SuppressLint("SuspiciousIndentation")
    fun setAgvPose(rt: robot_control_t) {
        val dParams = rt.dparams
        mLegendView?.setAgvX(dParams[0])
        mLegendView?.setAgvY(dParams[1])
        mLegendView?.setAgvT(dParams[2])
        if (dParams.size > 8) {
            mLegendView?.setAgvZ(dParams[8])
        }
        mRobotView?.setAgvData(dParams)
    }


    private var num = 0

    // 重用PointF对象，减少内存抖动
    private val mCarPoint = PointF()

    /**
     * 机器人有任务状态下行走的路径
     */
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
     * 设置充电站
     */
    fun getMachineStation(): MachineStation? = mHomeDockView?.getData()


    /**
     * 设置避让点
     */
    fun setCmsStations(list: MutableList<CmsStation>?) {
        mStationView?.setCmsStations(list)
    }

    /**
     * 设置乘梯点
     */
    fun setElevators(list: MutableList<ElevatorPoint>?) {
        mElevatorView?.setElevators(list)
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
     * 外部接口: 创建示教路径
     */
    fun createPathTeach(pptKey: Array<Point2d>?, pathParam: Short) {
        if (!pptKey.isNullOrEmpty()) {
            val m_KeyPst = DefPosture()
            for (point2d in pptKey) {
                val pst = Posture()
                pst.x = point2d.x
                pst.y = point2d.y
                pst.fThita = 0f
                m_KeyPst.AddPst(pst)
            }
//             CreateTeachPath(mWorld, pptKey, m_KeyPst, pathParam)
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
     * ******************************************************
     * *******************      监听接口        **************
     * ******************************************************
     */


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
     * 设置擦除噪点监听器
     */
    fun setOnRemoveNoiseListener(listener: IRemoveNoiseListener?) {
        mRemoveNoiseListener = listener
        mRemoveNoiseView?.setOnRemoveNoiseListener(object : RemoveNoiseView.OnRemoveNoiseListener {
            override fun onRemoveNoise(leftTop: PointF, rightBottom: PointF) {
                // 将屏幕坐标转换为世界坐标
                val worldLeftTop = screenToWorld(leftTop.x, leftTop.y)
                val worldRightBottom = screenToWorld(rightBottom.x, rightBottom.y)
                // 使用弱引用的监听器
                mRemoveNoiseListener?.onRemoveNoise(worldLeftTop, worldRightBottom)
            }
        })
    }

    /**
     * 手指抬起监听
     */
    fun setSingleTapListener(singleTapListener: ISingleTapListener?) {
        mSingleTapListener = if (singleTapListener != null) {
            WeakReference(singleTapListener)
        } else {
            null
        }
    }

    interface ISingleTapListener {
        fun onSingleTapListener(event: MotionEvent?)
    }

    /**
     * 擦除噪点监听器接口
     */
    interface IRemoveNoiseListener {
        /**
         * 当用户完成噪点擦除操作时调用
         * @param leftTop 矩形左上角的世界坐标
         * @param rightBottom 矩形右下角的世界坐标
         */
        fun onRemoveNoise(leftTop: PointF, rightBottom: PointF)
    }


}
