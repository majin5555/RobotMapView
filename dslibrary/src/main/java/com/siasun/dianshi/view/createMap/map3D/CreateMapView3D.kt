package com.siasun.dianshi.view.createMap.map3D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
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
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.hypot

/**
 * 地图画布（3D 建图 View）
 * 基于 SurfaceView + Flow 按需刷新，使用 throttleLatest 限制绘制频率并保证最后帧不丢失。
 */
open class CreateMapView3D(context: Context, attrs: AttributeSet) : SurfaceView(context, attrs),
    SlamGestureDetector.OnRPGestureListener, MapViewInterface, SurfaceHolder.Callback {

    private val TAG = this::class.java.simpleName

    companion object {
        /**
         * png 解码后的最大边长（像素）。
         * 原始 png/yaml 文件不做任何改动，只在解码阶段按 2 的整数倍采样压缩，
         * 避免 5000x5000 这类大图（RGB_565 约 50MB）带来的内存与逐帧采样开销；
         * 同时保证纹理上传不超过绝大多数设备的 GL_MAX_TEXTURE_SIZE(4096)。
         */
        private const val MAX_MAP_EDGE = 3000
    }

    /** 解码结果：位图 + 原始像素尺寸 + 补偿缩放比 */
    private class DecodedMap(
        val bitmap: Bitmap,
        val width: Int,
        val height: Int,
        val drawScale: Float
    )

    // 当前工作模式
    private var currentWorkMode = WorkMode.MODE_SHOW_MAP

    var mSrf = CoordinateConversion()//坐标转化工具类
    private var mOuterMatrix = Matrix()

    // 方案C：给 GL 线程提供"不可变矩阵快照"。
    // mOuterMatrix 在 UI 线程被 postScale/postTranslate 就地修改，GL 线程直接读会读到半更新状态，
    // 表现为某些图层错位一帧。这里每次变更后整体替换一个新对象（volatile 引用赋值是原子的），
    // GL 侧拿到的始终是一个稳定快照。
    @Volatile
    private var mMatrixSnapshot = Matrix()

    // 双缓冲：手势期每秒几十次 requestRender，每次都 new Matrix 会造成明显 GC 抖动
    private val snapshotBuffers = arrayOf(Matrix(), Matrix())
    private var snapshotIndex = 0

    private fun publishMatrixSnapshot() {
        val next = 1 - snapshotIndex
        snapshotBuffers[next].set(mOuterMatrix)
        mMatrixSnapshot = snapshotBuffers[next]
        snapshotIndex = next
    }

    internal fun snapshotOuterMatrix(out: Matrix) {
        out.set(mMatrixSnapshot)
    }
    private var VIEW_WIDTH = 0 //视图宽度
    private var VIEW_HEIGHT = 0

    //视图高度
    private var mMapScale = 1f //地图缩放级别
    private val mMaxMapScale = 5f //最大缩放级别
    private var mMinMapScale = 0.1f //最小缩放级别

    /**
     * 视图是否已经完成过"首次居中 + 缩放初始化"。
     *
     * SurfaceView 的 Surface 在页面切后台、跳到其它 Activity（如设置页）时会销毁，
     * 返回时重新创建并回调 [surfaceChanged]。原实现在该回调里无条件 [setCentred]，
     * 而建图过程中 [mSrf.mapData] 的宽高/分辨率每帧都在被激光数据改写，
     * 用这份"实时变化的地图尺寸"重算 FIT 缩放，会把用户当前的缩放与平移整体冲掉：
     * 车体图标大小随 mMapScale 突变（放大/缩小），并且 mMinMapScale 被抬高后
     * 再也缩不回原来的比例。因此只在首次初始化时居中，之后一律保留现场。
     */
    @Volatile
    private var mViewInited = false

    // 初始地图参数（用于计算扩展地图时的偏移量）
    private var initialOriginX = 0f
    private var initialOriginY = 0f
    private var initialHeight = 0f

    private var mMapView: WeakReference<CreateMapView3D> = WeakReference(this)
    private var mapLayers: MutableList<SlamWareBaseView<CreateMapView3D>> = CopyOnWriteArrayList()

    private var mPngMapView: PngMapView? = null //png地图
    var mMapOutline3D: MapOutline3DGL? = null // OpenGL 轮廓
//    private var mCreatingUpLaserScanView: UpLaserScanView3D? = null//上激光点云
    private var mAllKeyFrames: AllKeyFrameView3D? = null//所有关键帧
    private var mUpLaserScanView: UpLaserScanView<CreateMapView3D>? = null//上激光点云（非建图显示）
    var mConstrainNodes: ConstrainNodes? = null//人工约束节点

    //    private var mCreateMapRobotView: RobotViewCreateMap<CreateMapView3D>? = null //机器人图标
    private var mExpandAreaView: ExpandAreaView<CreateMapView3D>? = null //地图更新区域

    // 机器人位姿 [x, y, theta(rad), z, roll, pitch]
    override val robotPose = FloatArray(6)

    //是否建图标志（由页面依据导航心跳 1=定位 / 2=开始建图 置位）
    var isMapping = false

    //是否第一次接收到子图数据，如果没收到子图，直接跳过旋转环境
    var isStartRevSubMaps = false

    // 监听器
    private var mSingleTapListener: ISingleTapListener? = null
    private var mGestureDetector: SlamGestureDetector? = null

    // ──────────────── Flow 刷新相关 ────────────────
    // 用于按需刷新的触发流
    /**
     * 手势进行中（按下~抬起）。
     * 用于：① GL 侧降级渲染（跳过文字/网格、点径 1px）；② 暂停自动居中，避免和拖动打架
     */
    @Volatile
    private var isInteracting = false

    /**
     * 是否显示"实时关键帧"的详细内容（方向线 + 编号）。
     * showKeyFrames() 可能在 GL 图层创建之前被调用（Activity 在 initView 里就会调用），
     * 因此必须把状态记下来，等 GL 图层 attach 时再下发，否则开关会被丢掉。
     */
    @Volatile
    private var showKeyFrameDetail = false

    private val renderTrigger = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    // 协程作用域（与 Surface 生命周期绑定）
    private var renderScope: CoroutineScope? = null

    // 标记 Surface 是否已创建
    private var surfaceCreated = false

    // 方案A：GL 图层是否已输出过至少一帧。
    // 为 true 时本 SurfaceView 不再产画面（避免两张 Surface 之间的提交时差），
    // GL 图层还未就绪时仍按原逻辑在 Canvas 上绘制作为兜底，防止黑屏。
    @Volatile
    private var glFrameReady = false

    // png 解码协程
    private var loadJob: Job? = null

    // ──────────────── 底图格子（固定 40px 屏幕网格） ────────────────
    // 屏幕正交网格：线始终水平/竖直，锚定世界原点、不跟随机器人，格距不随缩放变化。
    // 绘制位置在白色背景之后、底图位图之前，处于底图与 GL 点云之下，不影响地图显示。
    private val GRID_CELL_PX = 40f          // 格距固定 40px（屏幕像素，不随缩放变化；与 GL 路径一致）
    private val mGridPaint = Paint().apply {
        color = Color.rgb(210, 210, 210)     // 浅灰细线
        style = Paint.Style.STROKE
        strokeWidth = 1f
        isAntiAlias = false
    }

    // 复用对象，避免每帧分配
    private val mGridWorldToPixelMatrix = Matrix()
    private val mGridTotalMatrix = Matrix()
    private val mGridMatrixValues = FloatArray(9)
    private val mGridAnchor = FloatArray(2)  // 世界原点在屏幕上的位置（格子锚点）

    // 临时诊断：格子绘制关键参数仅打印一次，便于定位"看不到格子"
    private var mGridDiagLogged = false

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
        // 移除 PngMapView 自带的白色背景：drawFrame 已用 canvas.drawColor 铺白，
        // 而 View.draw() 会先把这个不透明白色背景铺满整个视图，
        // 会把先于底图绘制的格子盖掉（表现为扫描时看不到格子线）。
        mPngMapView?.setBackground(null)
//        建图实时上激光点云绘制已迁移至 MapOutline3DGL(GLES 直接绘制)，
//        原 UpLaserScanView3D 控件保留，但不再作为绘制图层使用。
//        mCreatingUpLaserScanView = UpLaserScanView3D(context, mMapView)
        mAllKeyFrames = AllKeyFrameView3D(context, mMapView)
        mUpLaserScanView = UpLaserScanView(context, mMapView)
        mConstrainNodes = ConstrainNodes(context, mMapView)
//        mCreateMapRobotView = RobotViewCreateMap(context, mMapView)
        mExpandAreaView = ExpandAreaView(context, mMapView)

        // 注意：SurfaceView 模式下不再使用 addView 添加子 View
        // 图层顺序：扩展区域→约束→建图激光→关键帧→非建图激光→机器人
        //
        // 方案C：约束节点 / 所有关键帧 / 非建图激光 已改为在 MapOutline3DGL 中用 VBO 绘制，
        // 不再进入 canvas 图层（离屏快照）通道 —— 否则缩放时会因为快照滞后与底图脱节。
        // 这里只保留扩展区域（交互中且仅在扩展模式）。
        addMapLayers(mExpandAreaView)
    }

    // ──────────────── SurfaceHolder.Callback ────────────────
    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceCreated = true
        // 启动渲染协程
        renderScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
        renderScope?.launch {
            // 订阅建立后，先绘制首帧，再开始收集后续刷新事件。
            // 若在 launch 之后立刻调用 requestRender()，此时 MutableSharedFlow(replay=0)
            // 尚无订阅者，tryEmit 会丢弃事件，导致首帧 drawFrame 不执行、Surface 保持黑色。
            drawFrame(holder)
            renderTrigger
                .throttleLatest(25L)   // 每 25ms 最多绘制一次
                .collect {
                    drawFrame(holder)
                }
        }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        VIEW_WIDTH = width
        VIEW_HEIGHT = height

        // 更新虚拟子 View 的布局尺寸
        mPngMapView?.layout(0, 0, width, height)
        for (layer in mapLayers) {
            layer.layout(0, 0, width, height)
        }

        if (mViewInited) {
            // 从设置页/后台返回、或 Surface 被重建：保留用户当前的缩放与平移，
            // 只把现有矩阵重新分发给各图层并刷新一帧。
            // 这里绝不能再调用 setCentred()：建图中 mapData 的宽高每帧都在变，
            // 重新 FIT 会让车体箭头忽大忽小、且 mMinMapScale 被抬高后缩不回原比例。
            setMatrixWithScale(mOuterMatrix, mMapScale)
            mMapOutline3D?.notifyMatrixChanged()
            requestRender()
            return
        }

        if (mSrf.mapData.width > 0 && mSrf.mapData.height > 0) {
            setCentred()
        } else {
            // 无底图（新建建图）：以世界原点为屏幕中心初始化视图，
            // 机器人位于起点(0,0)时显示在屏幕中心而不是左上角
            mOuterMatrix = Matrix()
            mOuterMatrix.postTranslate(VIEW_WIDTH / 2f, VIEW_HEIGHT / 2f)
            setMatrixWithScaleAndRotation(mOuterMatrix, mMapScale, 0f)
            mViewInited = true
            mMapOutline3D?.notifyMatrixChanged()
            requestRender()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceCreated = false
        renderScope?.cancel()
        renderScope = null
    }

    /**
     * 绘制底图格子（50cm 正方形）。
     *
     * 原理：地图 resolution 为米/像素（正常 0.05，即 5cm/像素），故 50cm 一格
     * 对应 0.5 / resolution 个地图像素；屏幕上的格距 = 世界格距 × 缩放比例。
     *
     * 变换：不随地图旋转。只从 世界->地图像素->屏幕 的组合矩阵中取两样东西：
     *   1) 比例 totalScale（每 1 世界米对应的屏幕像素，旋转不影响该值）→ 决定格距；
     *   2) 世界原点(0,0)的屏幕位置 → 作为网格锚点。
     * 据此在屏幕坐标系画水平/竖直正交网格。旋转时锚点会移动、格距不变、线方向恒定。
     */
    private fun drawGrid(canvas: Canvas) {
        if (VIEW_WIDTH == 0 || VIEW_HEIGHT == 0) return

        var res: Float
        var originX: Float
        var originY: Float
        var mapHeight: Float
        synchronized(mSrf.mapData) {
            val mapData = mSrf.mapData
            res = mapData.resolution
            originX = mapData.originX
            originY = mapData.originY
            mapHeight = mapData.height
        }
        if (res <= 0.0001f) res = 0.05f
        // 地图元数据未就绪（尚未收到首帧激光数据）时不绘制
        if (mapHeight <= 0f) return

        // 世界坐标 -> 地图像素坐标：px=(wx-originX)/res, py=height-(wy-originY)/res
        mGridWorldToPixelMatrix.reset()
        mGridWorldToPixelMatrix.postTranslate(-originX, -originY)
        mGridWorldToPixelMatrix.postScale(1f / res, -1f / res)
        mGridWorldToPixelMatrix.postTranslate(0f, mapHeight)

        // 组合矩阵：屏幕 = outerMatrix ∘ worldToPixel（仅用于取比例与世界原点位置）
        mGridTotalMatrix.set(mOuterMatrix)
        mGridTotalMatrix.preConcat(mGridWorldToPixelMatrix)

        // 矩阵比例 = 每 1 世界米对应的屏幕像素数（旋转不改变该值）
        mGridTotalMatrix.getValues(mGridMatrixValues)
        val totalScale = hypot(
            mGridMatrixValues[Matrix.MSCALE_X], mGridMatrixValues[Matrix.MSKEW_Y]
        )
        if (totalScale <= 0f) return

        // 临时诊断：打印一次关键参数，便于定位"看不到格子"
        if (!mGridDiagLogged) {
            mGridDiagLogged = true
            Log.d(
                TAG,
                "grid diag: res=$res mapHeight=$mapHeight originX=$originX originY=$originY " +
                    "totalScale=$totalScale cellPx=$GRID_CELL_PX anchorWorld=(0,0)"
            )
        }

        // 与 GL 路径保持一致：格距固定 40px，不随缩放变化
        val cellPx = GRID_CELL_PX

        // 网格属于"世界"：锚点固定在世界原点，不跟随机器人。
        // 相机跟随时网格会在屏幕上相对滑动，便于判断车体轨迹是否笔直。
        mGridAnchor[0] = 0f
        mGridAnchor[1] = 0f
        mGridTotalMatrix.mapPoints(mGridAnchor)
        var anchorX = mGridAnchor[0]
        var anchorY = mGridAnchor[1]
        if (!anchorX.isFinite() || !anchorY.isFinite()) return
        // 归一化到 (-cellPx, 0]：保持网格相位，避免世界原点远离视野时的浮点误差
        anchorX %= cellPx
        if (anchorX > 0f) anchorX -= cellPx
        anchorY %= cellPx
        if (anchorY > 0f) anchorY -= cellPx

        // 屏幕正交网格：线宽恒为 1px
        mGridPaint.strokeWidth = 1f

        // 竖线：x = anchorX + n * cellPx
        var n = ceil((0f - anchorX) / cellPx).toInt()
        val nEnd = floor((VIEW_WIDTH - anchorX) / cellPx).toInt()
        while (n <= nEnd) {
            val x = anchorX + n * cellPx
            canvas.drawLine(x, 0f, x, VIEW_HEIGHT.toFloat(), mGridPaint)
            n++
        }

        // 横线：y = anchorY + j * cellPx
        var j = ceil((0f - anchorY) / cellPx).toInt()
        val jEnd = floor((VIEW_HEIGHT - anchorY) / cellPx).toInt()
        while (j <= jEnd) {
            val y = anchorY + j * cellPx
            canvas.drawLine(0f, y, VIEW_WIDTH.toFloat(), y, mGridPaint)
            j++
        }
    }

    /**
     * 执行一帧绘制（在协程中调用）
     *
     * 方案A：底图 png、50cm 网格、canvas 图层（约束节点/关键帧/扩展区/非建图激光）、点云、机器人
     * 已全部合并到 MapOutline3DGL 这**同一个 GL Surface** 上输出，本 SurfaceView 不再出图，
     * 彻底消除"两张 Surface 各自排队 + 各自与 vsync 对齐"带来的拖动脱节。
     *
     * 仅在 GL 图层尚未产出首帧时（如 GLSurfaceView 创建失败、上下文未就绪）才回退到原 Canvas 绘制，
     * 保证不出现黑屏，原有绘制逻辑一行未改。
     */
    private fun drawFrame(holder: SurfaceHolder) {
        if (mMapOutline3D != null && glFrameReady) return
        var canvas: Canvas? = null
        try {
            canvas = holder.lockCanvas()
            if (canvas != null) {
                synchronized(holder) {
                    // 白色背景
                    canvas.drawColor(Color.WHITE)
                    // 底图格子（画在底图下方，不影响底图与点云显示）
                    drawGrid(canvas)
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
        // 方案C：先把当前矩阵固化成快照，再触发渲染，保证本帧所有图层用同一份矩阵
        publishMatrixSnapshot()
        // 方案A：canvas 图层（扩展区域）仍由 GL 侧栅格化成纹理
        mMapOutline3D?.requestOverlayRefresh()
        if (surfaceCreated) {
            renderTrigger.tryEmit(Unit)
        }
    }

    // ──────────────── 视图生命周期 ────────────────
    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        if (parent is ViewGroup) {
            val parentGroup = parent as ViewGroup

            // 去重：同一个父容器里可能已经存在上一次 attach 创建的 GL 图层
            // （onDetachedFromWindow 里的 post 移除在 detach 后不会执行）。
            // 若重复挂载，会出现两套画面叠加（两个机器人、两套关键帧），
            // 且两者矩阵更新时机不同，看起来就像"图层与底图脱节"。
            for (i in 0 until parentGroup.childCount) {
                val child = parentGroup.getChildAt(i)
                if (child is MapOutline3DGL) {
                    mMapOutline3D = child
                    return
                }
            }
            if (mMapOutline3D != null) return

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

            // 设置机器人图标（从资源加载）
            val robotBitmap =
                BitmapFactory.decodeResource(resources, R.mipmap.create_current_location)
            glLayer.setRobotBitmap(robotBitmap)

            // 把创建前记录的"显示关键帧"状态补下发给新图层
            glLayer.setDrawingEnabled(showKeyFrameDetail)

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

        // 清理 png 解码协程
        loadJob?.cancel()
        loadJob = null
        glFrameReady = false

        // 清理资源
        mapLayers.clear()
        mPngMapView = null
//        mCreatingUpLaserScanView = null
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
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> beginInteract()
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> endInteract()
        }
        if (currentWorkMode == WorkMode.MODE_EXTEND_MAP_ADD_REGION) {
            // SurfaceView 模式下，需要手动分发事件给 ExpandAreaView
            val handled = mExpandAreaView?.onTouchEvent(event) ?: false
            // 返回true表示事件已处理，禁止手势检测器处理，从而禁止底图拖动
            if (handled) {
                // 扩展区域开始编辑：只有这个场景才需要 canvas 图层快照通道
                overlayNeeded = true
                // ExpandAreaView 内部只用 postInvalidate() 请求刷新，
                // 而该 View 未挂到窗口上（postInvalidate 无效），必须由这里显式请求一帧，
                // 否则"正在拖拽的扩展区域矩形"不会跟随手指刷新（原 Canvas 路径依赖激光帧兜底）
                requestRender()
                return true
            }
        }

        // 非特殊模式或在扩展区域模式下未被处理的事件，由手势检测器处理
        return mGestureDetector!!.onTouchEvent(event, this)
    }

    private fun beginInteract() {
        if (isInteracting) return
        isInteracting = true
        mMapOutline3D?.setInteracting(true)
    }

    private fun endInteract() {
        if (!isInteracting) return
        isInteracting = false
        mMapOutline3D?.setInteracting(false)
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
            // 其他模式下正常更新所有图层
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
        // 用户已经手动改过视图：此后 Surface 重建必须保留用户的比例
        mViewInited = true
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

    /**
     * 同步缩放状态：mMapScale 与 mSrf.scale 必须始终一致。
     *
     * mSrf.scale 被下游当作"屏幕像素/地图像素"使用（机器人图标尺寸、关键帧刻度线宽都读它）。
     * 此前只有捏合手势 setScale() 会写 mSrf.scale，而 setCentred() / setMatrixWithScale*()
     * 只改 mMapScale，导致扩展地图页加载完地图后 mSrf.scale 一直停在旧值（通常是 1），
     * 上述换算全部失真。统一在这里同步，避免再漏。
     */
    private fun syncScale(scale: Float) {
        mMapScale = scale
        if (scale > 0f) mSrf.scale = scale
    }

    private fun setMatrixWithScale(matrix: Matrix, scale: Float) {
        mOuterMatrix = matrix
        syncScale(scale)
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
        syncScale(scale)
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
            mViewInited = true
            mMapOutline3D?.notifyMatrixChanged()
            requestRender()
        }
    }

    /**
     * 调试 / 自动化压测用：直接设置视图缩放（不经过手势），并把世界原点置于屏幕中心。
     * 供无手势环境的建图测试使用，正常业务流程不调用。
     */
    fun setDebugScale(scale: Float) {
        if (VIEW_WIDTH == 0 || VIEW_HEIGHT == 0) {
            post { setDebugScale(scale) }
            return
        }
        val target = scale.coerceIn(mMinMapScale, mMaxMapScale)
        val current = mMapScale
        // 以"当前视野中心"为锚点缩放，保留原有平移量：
        // 若直接重置矩阵到世界原点，画面会整体跳一下（表现为闪屏）。
        val m = Matrix()
        if (current > 0f) {
            m.set(mOuterMatrix)
            m.postScale(target / current, target / current, VIEW_WIDTH / 2f, VIEW_HEIGHT / 2f)
        } else {
            m.postScale(target, target)
            m.postTranslate(VIEW_WIDTH / 2f, VIEW_HEIGHT / 2f)
        }
        setMatrixWithScaleAndRotation(m, target, 0f)
        // 压测/调试已显式指定缩放，后续 Surface 重建同样不要再做 FIT 居中
        mViewInited = true
        mMapOutline3D?.notifyMatrixChanged()
        requestRender()
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
//        建图激光点云由 MapOutline3DGL 直接绘制，不再向下发。
//        mCreatingUpLaserScanView?.setWorkMode(mode)
        mAllKeyFrames?.setWorkMode(mode)
//        mCreateMapRobotView?.setWorkMode(mode)
        mExpandAreaView?.setWorkMode(mode)
        // 注意：这里**不做**点云清理。setWorkMode 只是界面状态切换，
        // 页面初始化时就会调 MODE_SHOW_MAP，把它当"数据事件"用会导致误清空。
        // 残留红点的清理由真正的建图数据到达时处理（parseLaserData / parseOptPose）。
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
     *
     * 解码压缩说明（原始 png / yaml 文件不做任何修改）：
     * 1. 先只读尺寸，再按 2 的整数倍采样率解码，保证解码后最大边 ≤ [MAX_MAP_EDGE]；
     * 2. yaml 仍按**原始像素尺寸**解析，因此 mSrf.mapData.width/height/resolution 的语义原样保留，
     *    世界坐标↔像素坐标的换算、GL 的 MVP、激光点、机器人全部不受影响；
     * 3. 解码缩放比交给 PngMapView / GL 在绘制阶段补偿（drawScale），屏幕显示位置与尺寸完全不变。
     */
    fun loadMap(pngPath: String, yamlPath: String) {
        val file = File(pngPath)
        if (!file.exists()) {
            Log.w(TAG, "loadMap: png not exists -> $pngPath")
            return
        }
        loadJob?.cancel()
        loadJob = CoroutineScope(Dispatchers.Main.immediate + SupervisorJob()).launch {
            val decoded = withContext(Dispatchers.IO) { decodeMapBitmap(pngPath) }
            if (decoded == null) {
                Log.w(TAG, "loadMap: decode failed -> $pngPath")
                return@launch
            }
            // 用原始像素尺寸解析 yaml，保持世界<->像素映射语义不变
            val mPngMapData = YamlNew().loadYaml(
                yamlPath,
                decoded.height.toFloat(),
                decoded.width.toFloat()
            )
            setBitmap(mPngMapData, decoded.bitmap, decoded.drawScale)
        }
    }

    /**
     * 解码 png 地图位图。
     * 采用 inSampleSize（2 的整数倍）先降采样，既省内存又比 createScaledBitmap 少一次大图分配；
     * 采样比取整数还能让 drawScale 恰好是 1/2、1/4…，绘制时不会出现半像素采样抖动。
     */
    private fun decodeMapBitmap(pngPath: String): DecodedMap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(pngPath, bounds)
        val srcWidth = bounds.outWidth
        val srcHeight = bounds.outHeight
        if (srcWidth <= 0 || srcHeight <= 0) return null

        var sample = 1
        while (maxOf(srcWidth, srcHeight) / sample > MAX_MAP_EDGE) {
            sample *= 2
        }

        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.RGB_565
            inScaled = false
        }
        val bitmap = try {
            BitmapFactory.decodeFile(pngPath, opts)
        } catch (e: Exception) {
            Log.e(TAG, "decodeMapBitmap: ${e.message}")
            null
        } ?: return null
        if (bitmap.width <= 0 || bitmap.height <= 0) return null

        val drawScale = srcWidth.toFloat() / bitmap.width.toFloat()
        Log.d(
            TAG,
            "decodeMapBitmap: ${srcWidth}x$srcHeight sample=$sample " +
                "-> ${bitmap.width}x${bitmap.height} drawScale=$drawScale"
        )
        return DecodedMap(bitmap, srcWidth, srcHeight, drawScale)
    }

    /**
     * 设置地图数据信息
     * 设置地图
     *
     * @param bitmap 实际参与绘制的位图
     * @param drawScale 地图原始像素 / 位图像素，绘制时按该比例补偿
     */
    private fun setBitmap(mapData: MapData, bitmap: Bitmap, drawScale: Float = 1f) {
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
        mPngMapView?.setBitmap(bitmap, drawScale)
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
        // 建图激光（KEY_UPDATE_POS）一到位，说明建图已经在跑：非建图的上激光点云
        // （KEY_CURRENT_POINT_CLOUD，此时已停发）不能再留在画面上。
        // 判断 upLaserCount > 0 才清：清空后计数归零，后续每一帧只是一次 int 比较，零开销。
        clearUselessUpLaser()

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

        //保持居中（手势进行中不强行拉回，否则拖动会被"拽回去"）
        if (currentWorkMode == WorkMode.MODE_CREATE_MAP && !isInteracting) {
            keepRobotCentered()
        }
        calBinding(laserData, type)
        // 建图实时激光点云由 MapOutline3DGL 直接计算并绘制（GLES），
        // 原 UpLaserScanView3D 控件的计算职责已迁移至 MapOutline3DGL#updateLiveScan。
        mUpLaserScanView?.clear()
        mMapOutline3D?.updateLiveScan(laserData)
        mMapOutline3D?.updateRobotPose(robotPose[0], robotPose[1], robotPose[2])
        requestRender()
    }

    fun loadCurPointCloud(laserData: laser_t) {
        // 方案C：非建图激光改为在 GL 侧用 VBO 绘制，这里只缓存世界坐标点。
        // UpLaserScanView 为 2D/主地图共用控件，不做任何修改，仅不再作为 3D 的绘制图层。
        //
        // 建图中（导航心跳已置 isMapping）这路点云不参与显示：
        // 建图红点由 parseLaserData → MapOutline3DGL.updateLiveScan 单独绘制。
        // 这里只做"忽略"，不做清空——清空统一由建图数据到达时触发（见 clearUselessUpLaser）。
        if (isMapping) return
        updateUpLaserPoints(laserData)
        requestRender()
    }

    /**
     * 清掉"非建图的上激光点云"留下的残影（一次有效，之后是零开销）。
     *
     * KEY_CURRENT_POINT_CLOUD 这条非建图通道的点云被换算成世界坐标缓存在 [upLaserPoints]，
     * GL 每帧照画。开始建图后该通道停发，缓存却没人数它过期，于是最后一帧红点
     * 永久钉在地图上（车开出去很远也不消失）。
     *
     * 调用时机：真正的建图数据到达时（[parseLaserData] / [parseOptPose]），
     * 而不是 setWorkMode —— 后者只是界面状态切换，页面初始化就会调，不能当数据事件用。
     */
    private fun clearUselessUpLaser() {
        // 没有残留就直接返回：清空之后每帧只做这一次判断，不会重复触发清空
        if (!synchronized(upLaserLock) { upLaserCount > 0 }) return
        clearCurPointCloud()
    }

    /**
     * 清空"非建图"上激光点云缓存。
     *
     * 世界坐标缓存由 GL 每帧绘制，数据源停发（开始建图后）时不会自动消失，
     * 会在地图上留下永远擦不掉的红点。这里置空并推进版本号，让 GL 侧同步清空 VBO 绘制。
     */
    fun clearCurPointCloud() {
        synchronized(upLaserLock) {
            upLaserPoints = FloatArray(0)
            upLaserCount = 0
        }
        upLaserVersion++
        requestRender()
    }

    /**
     * 把上激光点云转成世界坐标，供 GL 侧绘制。
     *
     * 位姿必须取"这一帧自带"的 ranges[0..2]（与 1.1.69 的 UpLaserScanView 完全一致）：
     * 激光包里的位姿是该帧扫描时刻的导航位姿，与点云严格配对；
     * 若改用控件里的 [robotPose]（来自车体上报 KEY_AGV_COORDINATE，另外的包、另外的频率），
     * 转弯/行驶时每帧都会被"最新的车体位姿"重新刚体变换一次，红点会跟着车头甩；
     * 两者来源不同时还会整体偏离墙体；robotPose 尚未写入时更会全部堆到原点。
     */
    private fun updateUpLaserPoints(laserData: laser_t) {
        val ranges = laserData.ranges
        val groups = ranges.size / 3
        if (groups <= 1) {
            synchronized(upLaserLock) {
                upLaserPoints = FloatArray(0)
                upLaserCount = 0
            }
            upLaserVersion++
            return
        }
        // 帧内位姿：[x, y, theta]，点云从第 2 组三元组开始
        val robotX = ranges[0]
        val robotY = ranges[1]
        val theta = ranges[2]
        val cosT = kotlin.math.cos(theta)
        val sinT = kotlin.math.sin(theta)
        val count = groups - 1
        // 写入与 GL 线程的 snapshotUpLaser() 互斥：否则可能拷到"半新半旧"的一帧，
        // 表现为偶发的错位点（1.1.69 的 canvas 版同样有这把锁）
        synchronized(upLaserLock) {
            if (upLaserPoints.size < count * 2) upLaserPoints = FloatArray(count * 2)
            val out = upLaserPoints
            var idx = 0
            for (i in 1 until groups) {
                val lx = ranges[3 * i]
                val ly = ranges[3 * i + 1]
                out[idx++] = lx * cosT - ly * sinT + robotX
                out[idx++] = lx * sinT + ly * cosT + robotY
            }
            upLaserCount = count
        }
        upLaserVersion++
    }

    // ──────────────── GL 侧数据接口（VBO 绘制用） ────────────────
    private var upLaserPoints = FloatArray(0)
    private var upLaserCount = 0
    private val upLaserLock = Any()
    @Volatile
    internal var upLaserVersion = 0
        private set

    internal fun snapshotUpLaser(): FloatArray = synchronized(upLaserLock) {
        upLaserPoints.copyOf(upLaserCount * 2)
    }

    internal fun oldKeyFramesVersion(): Int = mAllKeyFrames?.dataVersion ?: -1
    internal fun snapshotOldKeyFrames(): FloatArray? =
        mAllKeyFrames?.takeIf { it.isDrawing() }?.snapshotKeyFrames()

    internal fun constrainNodesVersion(): Int = mConstrainNodes?.dataVersion ?: -1
    internal fun snapshotConstraintNodes(): FloatArray? = mConstrainNodes?.snapshotNodes()

    /**
     * 只有扩展区域（canvas 交互图层）还需要离屏快照通道。
     * 未进入扩展区域编辑时关闭 overlay，彻底避免"快照滞后"导致的缩放脱节。
     */
    @Volatile
    internal var overlayNeeded = false
        private set

    fun parseOptPose(laserData: laser_t) {
        // KEY_OPT_POSE（导航回环/子图优化）只在建图中才下发，是"确实在扫图"的可靠信号：
        // 到这里如果还残留着非建图上激光点云的世界坐标，就地清一次（清完即 0，后续零开销）。
        // 注意它通常是稀疏的，所以主清理由 parseLaserData（每一帧建图激光）负责，这里是兜底。
        clearUselessUpLaser()
        mMapOutline3D?.parseOptPose(laserData)
        mMapOutline3D?.requestRender()
        requestRender()
    }

    fun showKeyFrames(boolean: Boolean) {
        showKeyFrameDetail = boolean
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
        mAllKeyFrames?.setDrawingEnabled(true)
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

        // 同步到 GL 层（机器人图标由 MapOutline3DGL 绘制，使用其内部 robotPose）。
        // 非建图模式下（如扩展地图页重新定位后）仅调用本方法，若不推送 GL，
        // 机器人图标会停留在旧位置不更新
        mMapOutline3D?.updateRobotPose(robotPose[0], robotPose[1], robotPose[2])
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

        // 位移小于 1px 不必重设矩阵（避免空闲时反复触发渲染与快照分配）
        if (kotlin.math.abs(dx) < 1f && kotlin.math.abs(dy) < 1f) return
        // 移动地图使机器人居中
        setTransition(dx.toInt(), dy.toInt())
    }

    /**
     * 清空当前建图状态（关键帧/点云/约束节点/非建图激光），供"开始模拟"重开一轮使用。
     * 只影响 3D 建图链路，不动 2D 共用控件。
     */
    fun resetMappingState() {
        mMapOutline3D?.resetPointCloud()
        mConstrainNodes?.clearNodes()
        mAllKeyFrames?.setDrawingEnabled(false)
        upLaserPoints = FloatArray(0)
        upLaserCount = 0
        upLaserVersion++
        isStartRevSubMaps = false
        requestRender()
    }

    fun resetExpandAreaView() {
        mExpandAreaView?.resetCreateState()
        overlayNeeded = false
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

    // ──────────────── 方案A：单 Surface 渲染（供 MapOutline3DGL 读取） ────────────────

    /** 底图位图（可能已按 MAX_MAP_EDGE 解码压缩） */
    internal fun getPngBitmap(): Bitmap? = mPngMapView?.pngBitmap

    /** 底图位图版本号，GL 侧据此判断是否需要重传纹理 */
    internal fun getPngBitmapVersion(): Int = mPngMapView?.pngBitmapVersion ?: -1

    /** 底图解码缩放比（原始像素 / 位图像素） */
    internal fun getPngDrawScale(): Float = mPngMapView?.pngDrawScale ?: 1f

    /** 底图偏移（单位：地图原始像素），扩展地图时使用 */
    internal fun getPngOffsetX(): Float = mPngMapView?.pngOffsetX ?: 0f

    internal fun getPngOffsetY(): Float = mPngMapView?.pngOffsetY ?: 0f

    /**
     * 拷贝底图矩阵快照到 out。
     * 这里刻意使用 PngMapView 自己的矩阵（而不是 mOuterMatrix）：
     * 扩展地图"增加区域"模式下原逻辑就是只更新子图层、不更新底图，
     * 合并到 GL 后必须完整保持该行为，因此底图矩阵只能从 PngMapView 取。
     */
    internal fun copyPngMatrixInto(out: Matrix): Boolean {
        val png = mPngMapView ?: return false
        if (png.pngBitmap == null) return false
        png.copyMatrixInto(out)
        return true
    }

    /**
     * 把 canvas 图层（扩展区域→约束节点→关键帧→非建图激光）画到 GL 侧的离屏位图上，
     * 绘制顺序与 [drawFrame] 中的 mapLayers 遍历顺序完全一致。
     */
    internal fun drawOverlayLayers(canvas: Canvas) {
        for (layer in mapLayers) {
            layer.draw(canvas)
        }
    }

    /**
     * GL 图层已输出首帧：本 SurfaceView 停止出图（避免两张 Surface 的提交时差）
     */
    internal fun onGlFrameReady() {
        glFrameReady = true
    }

    private fun addMapLayers(mapLayer: SlamWareBaseView<CreateMapView3D>?) {
        if (mapLayer != null && !mapLayers.contains(mapLayer)) {
            mapLayers.add(mapLayer)
        }
    }

    /**
     * 设置扩展区域是否显示坐标文本
     */
    fun setShowCoordinates(show: Boolean) {
        mExpandAreaView?.setShowCoordinates(show)
    }

    interface ISingleTapListener {
        fun onSingleTapListener(point: PointF)
    }
}