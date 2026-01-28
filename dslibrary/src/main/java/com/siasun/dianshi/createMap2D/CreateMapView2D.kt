package com.siasun.dianshi.createMap2D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
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
import com.siasun.dianshi.bean.MapData
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
import com.siasun.dianshi.bean.createMap2d.MapEditorConstants
import com.siasun.dianshi.bean.createMap2d.SubMapData
import com.siasun.dianshi.view.PngMapView
import com.siasun.dianshi.view.SlamWareBaseView
import java.math.RoundingMode
import java.nio.ByteBuffer
import java.text.DecimalFormat
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.set
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

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
    private var mUpLaserScanView: UpLaserScanView2D? = null//上激光点云
    private var mCreateMapRobotView: RobotView2D? = null //机器人图标


    private val keyFrames2d = ConcurrentHashMap<Int, SubMapData>() //绘制地图的数据 建图时 2D
    val robotPose = FloatArray(6) // [x, y, theta(rad),z roll pitch]
    var isMapping = false//是否建图标志
    var isRouteMap = false//是否可以旋转地图

    //是否第一次接收到子图数据，如果没收到子图，直接跳过旋转环境
    var isStartRevSubMaps = false

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
        mUpLaserScanView = UpLaserScanView2D(context, mMapView)
        mCreateMapRobotView = RobotView2D(context, mMapView)
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

    /**
     * 计算新建地图宽高
     */
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
        mSrf.mapData.width = abs((maxTopRight.x - minBotLeft.x) / 0.05f)
        mSrf.mapData.height = abs((maxTopRight.y - minBotLeft.y) / 0.05f)

//        Log.i("SLAMMapView2D","左上 ${minTopLeft}")
//        Log.i("SLAMMapView2D","左下 ${minBotLeft}")
//        Log.i("SLAMMapView2D","右上 ${maxTopRight}")
//        Log.i("SLAMMapView2D","右下 ${maxBottomRight}")
//        Log.i("SLAMMapView2D","整张地图的宽度 ${mSrf.mapData.mWidth}")
//        Log.i("SLAMMapView2D","整张地图的高度 ${mSrf.mapData.mHeight}")
    }


    /**
     * 外部接口 解析激光点云数据（建图模式） 2D
     */
    fun parseLaserData2D(laserData: laser_t) {
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
     * 设置AGV 位姿 机器人图标的实时位置
     */
    @SuppressLint("SuspiciousIndentation")
    fun setAgvPose(rt: robot_control_t) {
        val dParams = rt.dparams
        mCreateMapRobotView?.setAgvData(dParams)
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
