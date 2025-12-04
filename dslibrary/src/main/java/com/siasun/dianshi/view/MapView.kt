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
import com.siasun.dianshi.bean.CmsStation
import com.siasun.dianshi.bean.InitPose
import com.siasun.dianshi.bean.MachineStation
import com.siasun.dianshi.bean.MapData
import com.siasun.dianshi.bean.MergedPoseItem
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
        MODE_CMS_STATION_EDIT  // 修改避让点模式
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

    var mWallView: VirtualLineView? = null//虚拟墙
    private var mHomeDockView: HomeDockView? = null//充电站
    private var mStationView: StationsView? = null//站点
    private var mOnlinePoseView: OnlinePoseView? = null//上线点
    private var mLegendView: LegendView? = null//图例
    var mUpLaserScanView: UpLaserScanView? = null//上激光点云
    var mDownLaserScanView: DownLaserScanView? = null//下激光点云
    var mTopViewPathView: TopViewPathView? = null//顶视路线

    //    var mAreasView: AreasView? = null//区域
//    var mMixAreasView: MixedAreasView? = null//混行区域
//    var mPathView: PathView? = null//路线PP
    var mRobotView: RobotView? = null //机器人图标
    var mWorkIngPathView: WorkIngPathView? = null //机器人工作路径

    private var mSingleTapListener: ISingleTapListener? = null

    //手势监听
    private var mGestureDetector: SlamGestureDetector? = null


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
        mWallView = VirtualLineView(context, mMapView)
        mHomeDockView = HomeDockView(context, mMapView)
        mStationView = StationsView(context, mMapView)
        mOnlinePoseView = OnlinePoseView(context, mMapView)
        mUpLaserScanView = UpLaserScanView(context, mMapView)
        mDownLaserScanView = DownLaserScanView(context, mMapView)
        mTopViewPathView = TopViewPathView(context, mMapView)
//        mAreasView = AreasView(context, mMapView)
//        mMixAreasView = MixedAreasView(context, mMapView)
        mLegendView = LegendView(context, attrs, mMapView)
        mRobotView = RobotView(context, mMapView)
        mWorkIngPathView = WorkIngPathView(context, mMapView)
//        val mPolygonEditView = PolygonEditView(context, mMapView)
        //底图的View
        addView(mPngMapView, lp)

        //充电站
        addMapLayers(mHomeDockView)
        //显示避让点
        addMapLayers(mStationView)
        //上线点
        addMapLayers(mOnlinePoseView)
        //上激光点云
        addMapLayers(mUpLaserScanView)
        //下激光点云
        addMapLayers(mDownLaserScanView)

        //显示虚拟墙
        addMapLayers(mWallView)
        //顶视路线
        addMapLayers(mTopViewPathView)
        //清扫区域 区域
//        addMapLayers(mAreasView)

        //机器人图标
        addMapLayers(mRobotView)

//        addView(mPolygonEditView)

//        addMapLayers(mMixAreasView)
//        addMapLayers(mPathView)

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
        super.onTouchEvent(event)
        val point = screenToWorld(event.x, event.y)
        mLegendView?.setScreen(point)
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
        if (mSingleTapListener != null) {
            mSingleTapListener!!.onSingleTapListener(event)
        }
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
            getViewTreeObserver().addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    VIEW_HEIGHT = height
                    VIEW_WIDTH = width
                    getViewTreeObserver().removeGlobalOnLayoutListener(this)
                    setCentred()
                }
            })
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

    fun setSingleTapListener(singleTapListener: ISingleTapListener?) {
        mSingleTapListener = singleTapListener
    }

    interface ISingleTapListener {
        fun onSingleTapListener(event: MotionEvent?)
    }

    override fun onDetachedFromWindow() {
        mapLayers.clear()
        super.onDetachedFromWindow()
    }

    /***
     * 设置地图显示
     */
    fun setSlamMapViewShow(visibility: Int) {
        mPngMapView!!.visibility = visibility
    }

    /**
     * ******************************************************
     * *******************      外部接口        **************
     * ******************************************************
     */

    /**
     * 设置地图数据信息
     * 设置地图
     *
     * @param bitmap
     */
    fun setBitmap(mapData: MapData, bitmap: Bitmap) {
        mSrf.mapData = mapData
        mPngMapView!!.setBitmap(bitmap)
        // 设置地图后自动居中显示
        setCentred()
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

    /**
     * 机器人有任务状态下行走的路径
     */
    fun setWorkingPath(array: DoubleArray) {
        num++
        if (num % 3 == 0) {
            val mCarPoint = PointF()
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
    fun setMachineStation(machineStation: MachineStation?) =
        mHomeDockView?.setHomePose(machineStation)


    /**
     * 设置避让点
     */
    fun setCmsStations(list: MutableList<CmsStation>?) = mStationView?.setCmsStations(list)

    /**
     * 设置避让点点击监听器
     */
    fun setOnStationClickListener(listener: StationsView.OnStationClickListener) =
        mStationView?.setOnStationClickListener(listener)

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        currentWorkMode = mode
        // 将工作模式传递给虚拟墙视图
        mWallView?.setWorkMode(mode)
        // 将工作模式传递给避让点视图
        mStationView?.setWorkMode(mode)
    }

    /**
     * 获取当前工作模式
     */
    fun getCurrentWorkMode(): WorkMode {
        return currentWorkMode
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
}
