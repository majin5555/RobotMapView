package com.siasun.dianshi.view.createMap.map3D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Matrix
import android.graphics.PorterDuff
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix as GMatrix
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.MotionEvent
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.bean.ConstraintNode
import com.siasun.dianshi.bean.KeyFrame
import com.siasun.dianshi.view.WorkMode
import java.lang.ref.WeakReference
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin

@SuppressLint("ViewConstructor")
class MapOutline3DGL(
    context: Context,
    private val parent: WeakReference<CreateMapView3D>
) : GLSurfaceView(context), GLSurfaceView.Renderer {

    companion object {
        private val TAG = "MapOutline3DGL"

        private val COLOR_POINT_CLOUD = floatArrayOf(0f, 0f, 0f, 1f)
        private val COLOR_KEYFRAME = floatArrayOf(0f, 1f, 0f, 1f)
        private val COLOR_LINE = floatArrayOf(1f, 0f, 0f, 1f)
        private val COLOR_LIVE_POINT = floatArrayOf(1f, 0f, 0f, 1f) // 实时上激光点云（红）
        // 点云点径：保持 3px 原观感（降到 2px 后覆盖率下降，画面会明显"变稀"）。
        // 填充压力主要靠"分块剔除 + LOD + 手势期 1px"解决，而不是牺牲平时的观感
        private const val POINT_SIZE_CLOUD = 3f
        // 手势进行中的点径：交互期只求跟手，1px 几乎无填充压力
        private const val POINT_SIZE_CLOUD_INTERACT = 1f
        private const val POINT_SIZE_KEYFRAME = 8f
        private const val POINT_SIZE_LIVE = 3f
        // 方案C：旧关键帧/约束节点/非建图激光（原 canvas 图层）的屏幕尺寸与颜色
        private const val OLD_KF_POINT_SIZE = 10f
        // 朝向线：长度 14px、线宽 3px。
        // 关键帧圆点直径正好 10px，线若也取 10px 会被圆点完全盖住（看起来"没有线"），
        // 因此比点半径多出 9px，保证缩小时也能看出朝向。
        // 注意 glLineWidth 在部分 GPU（Mali 等）被钳到 1px，所以线宽改用三角形实现
        private const val OLD_KF_LINE_LEN_PX = 14f
        private const val OLD_KF_LINE_WIDTH_PX = 3f
        private const val NODE_POINT_SIZE = 15f
        private const val UP_LASER_POINT_SIZE = 2f
        private val COLOR_OLD_KF = floatArrayOf(0.502f, 0f, 0.502f, 1f) // #800080
        private val COLOR_NODE = floatArrayOf(0f, 0f, 1f, 1f)           // Color.BLUE
        private val COLOR_UP_LASER = floatArrayOf(1f, 0f, 0f, 1f)       // Color.RED
        private val COLOR_WHITE = floatArrayOf(1f, 1f, 1f, 1f)
        private val COLOR_LIVE_TEXT = floatArrayOf(0.25f, 1f, 0.25f, 1f)  // 实时编号：亮绿（压在黑色点云上也要清晰）
        private const val DIR_LINE_LENGTH = 0.5f

        // 点云分块：每块拥有独立 VBO，约 400×1500 点 ≈ 4.8MB，用于视野剔除 + 增量追加
    private const val CHUNK_KEYFRAMES = 400
    // 单块点数上限，防止异常数据把单块撑到几百 MB（超限就提前封口）
    private const val CHUNK_MAX_POINTS = 300_000
    // ── 体素 LOD（单级）──
    // 缩到 1m 约 8px 时启用：用 0.3m 体素网格抽稀（每格留 1 点），点数约降到 1/3~1/8
    private const val LOD_ENABLED = true
    private const val LOD_VOXEL_METERS = 0.5f
    // 只在"缩得很小"时启用（1m ≤ 5px），正常建图/查看保持全量，避免点云变稀的观感
    private const val LOD_WORLD_PER_PX = 0.2f
    // 分帧预算：每帧最多处理这么多点（约 2ms），避免生成 LOD 时卡顿
    private const val LOD_POINTS_PER_FRAME = 40_000

    // 只保留最近 N 个关键帧的"原始激光点"（回环重算位姿用），更早的释放掉。
    // 每个关键帧的 cloudPoints 与 worldPoints 各占一份，释放后 Java 堆接近减半。
    private const val KEEP_CLOUD_POINT_FRAMES = 3000

    /**
     * 点云分块：同一块内的点在 VBO 中是连续的一段，
     * 记录世界 AABB 后即可按视野剔除，把"每帧画全部点"改成"只画看得见的块"。
     */
    /**
     * 点云分块。
     *
     * 每块拥有**独立的 GL VBO**（约 400 个关键帧 ≈ 4.8MB），不再共用一个"能长到几百 MB"的大缓冲：
     * - 新点只往最后一块追加，块满即新建，永不整块扩容拷贝（消除每几千步一次的长卡顿）；
     * - 单块尺寸远低于 GPU 单次分配上限，不会出现 glBufferData 静默失败导致"前面点云丢失"。
     *
     * 注：原先的 LOD 已移除（按数组索引抽稀会丢掉一半方向的射线，视觉上就是点云丢失），
     * 分块剔除已把绘制量压到足够低，无需 LOD。
     */
    private class CloudChunk(
        var kfStartIndex: Int,      // 在 keyFrameOrder 中的起始下标（扩容时整块重传用）
        var pointCount: Int,
        var kfCount: Int,
        var minX: Float,
        var minY: Float,
        var maxX: Float,
        var maxY: Float
    ) {
        var vboId: Int = 0
        var capacityPoints: Int = 0
        // 体素抽稀版本（缩小时使用）
        var lodVboId: Int = 0
        var lodPointCount: Int = 0
    }

    /** 分帧生成 LOD 的任务：每帧只处理一段点，避免一次性卡顿 */
    private class LodJob(
        val chunk: CloudChunk,
        val points: FloatArray,
        var cursor: Int,
        val seen: HashSet<Long>,
        val out: FloatArray,
        var outCount: Int
    )

    private val cloudChunks = ArrayList<CloudChunk>()
    private var buildingChunk: CloudChunk? = null
    private val lodJobs = ArrayDeque<LodJob>()
    private var lastDrawnCloudPoints = 0
    private var lastVisibleChunks = 0

    // 上传复用的 direct buffer（单帧小数据与整块重传共用，按需扩容）
    private var uploadScratch: FloatBuffer? = null
    // 实时编号实例上传复用缓冲（每帧重建，绝不能每帧新分配）
    private var textUploadScratch: FloatBuffer? = null

    // 关键帧黑点采样间隔（每 N 个原始点存 1 个）。
        // 固定常量，不跟随视图缩放：黑点是持久数据，密度必须可复现。
        private const val KEYFRAME_SAMPLE_INTERVAL = 3

        // 渲染节流最小帧间隔（毫秒）：GLSurfaceView 是 RENDERMODE_WHEN_DIRTY，
        // 每个激光数据包都会触发一次全场景重绘，需要限流避免 GL 线程被打满
        private const val RENDER_MIN_INTERVAL_MS = 40L

        // ── 文字统一缩放规则（实时关键帧与旧关键帧共用，保证观感一致）──
        // 屏幕字号 = BASE_PX * scale / REF_SCALE，再钳制到 [MIN, MAX]
        // 效果：放大时字变大、缩小时字变小，但不会小到看不清、也不会大到糊屏
        private const val TEXT_BASE_PX = 16f
        private const val TEXT_REF_SCALE = 3f
        private const val TEXT_MIN_PX = 9f
        private const val TEXT_MAX_PX = 26f
        // 编号之间的最小屏幕间距（避让）：小于该值只画点/线，不画编号。
        // 16px 字高时 3 位数字宽约 26px、4 位约 34px，所以取 48px（字宽 + 间隙），
        // 否则 100 以后编号会连成一片
        private const val TEXT_MIN_GAP_PX = 60f
        // 文字与关键帧点的屏幕间距
        private const val TEXT_GAP_PX = 12f
        // 视野裁剪外扩（像素），避免边缘文字突然出现/消失
        private const val TEXT_CLIP_MARGIN_PX = 48f
        // 字模图集高 64px、48px 字号，数字字形实际只占图集约 0.55，
        // 所以"想要的屏幕字高"要除以该比例才是 u_CharWorldHeight
        private const val GLYPH_HEIGHT_RATIO = 0.55f
        // 兼容旧代码：实时关键帧文字原世界高度（仅在未取到有效缩放时兜底）
        private const val CHAR_WORLD_HEIGHT = 0.2f
        private const val TEXT_OFFSET_WORLD_Y = 0.2f

        // 机器人世界尺寸（与图标视觉大小匹配，可根据需要调整）
        private const val ROBOT_SIZE = 0.5f

        // 在 companion object 外新增成员变量
        private var cachedMapScale = 1f
        private var cachedResolution = 0.05f
        private const val ROBOT_BASE_SIZE = 0.5f   // 基础世界尺寸（米）
        private const val MIN_SCREEN_PX = 20f       // 最小屏幕像素

        // 点/线着色器
        private const val VERTEX_SHADER_POINT = """
            uniform mat4 u_MVPMatrix;
            uniform float u_PointSize;
            uniform float u_Round;
            attribute vec4 a_Position;
            varying float v_Round;
            void main() {
                gl_Position = u_MVPMatrix * a_Position;
                gl_PointSize = u_PointSize;
                v_Round = u_Round;
            }
        """
        private const val FRAGMENT_SHADER_POINT = """
            precision mediump float;
            uniform vec4 u_Color;
            varying float v_Round;
            void main() {
                // 关键帧/约束节点需要圆点；点云保持方点，避免额外分支影响性能
                if (v_Round > 0.5) {
                    vec2 d = gl_PointCoord - vec2(0.5);
                    if (dot(d, d) > 0.25) discard;
                }
                gl_FragColor = u_Color;
            }
        """

        // 文字着色器
        private const val VERTEX_SHADER_TEXT = """
            uniform mat4 u_MVPMatrix;
            uniform float u_CharAspectScale;
            uniform float u_CharWorldHeight;
            attribute vec2 a_Vertex;
            attribute vec2 a_TexOffset;
            attribute vec2 a_WorldPos;
            attribute float a_Umin;
            attribute float a_Umax;
            varying vec2 v_TexCoord;
            void main() {
                float charWorldWidth = u_CharWorldHeight * (a_Umax - a_Umin) * u_CharAspectScale;
                vec2 worldPos = a_WorldPos + a_Vertex * vec2(charWorldWidth, u_CharWorldHeight);
                gl_Position = u_MVPMatrix * vec4(worldPos, 0.0, 1.0);
                v_TexCoord = vec2(a_Umin + a_TexOffset.x * (a_Umax - a_Umin), 1.0 - a_TexOffset.y);
            }
        """
        private const val FRAGMENT_SHADER_TEXT = """
            precision mediump float;
            uniform sampler2D u_Texture;
            uniform vec4 u_TextColor;
            varying vec2 v_TexCoord;
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord) * u_TextColor;
            }
        """

        // 机器人纹理着色器
        private const val VERTEX_SHADER_ROBOT = """
            uniform mat4 u_MVPMatrix;
            uniform mat4 u_ModelMatrix;
            attribute vec2 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = u_MVPMatrix * u_ModelMatrix * vec4(a_Position, 0.0, 1.0);
                v_TexCoord = a_TexCoord;
            }
        """
        private const val FRAGMENT_SHADER_ROBOT = """
            precision mediump float;
            uniform sampler2D u_Texture;
            varying vec2 v_TexCoord;
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
            }
        """

        // ─────────── 方案A：底图 / 网格 / canvas 图层（同一个 GL Surface） ───────────

        // 网格参数：必须与 CreateMapView3D.drawGrid 完全一致
        // 网格：固定屏幕格距（像素），与 CreateMapView3D.drawGrid 保持一致。
        // 不再按 0.5m 世界格距换算 —— 大地图缩小时 0.5m 只有零点几像素，
        // 会被"过小跳过"直接吞掉，表现为网格线消失。
        private const val GRID_CELL_PX = 40f
        private val COLOR_GRID = floatArrayOf(0.8235f, 0.8235f, 0.8235f, 1f) // Color.rgb(210,210,210)

        // canvas 图层纹理刷新的最小间隔（毫秒）。
        // 图层（约束节点/关键帧/扩展区/非建图激光）必须是屏幕空间栅格化才能保持"固定屏幕尺寸"的原逻辑，
        // 因此矩阵变化时需要重新栅格化 + 重传纹理；这里限到 ~30fps，避免每帧上传整屏 ARGB 纹理。
        private const val OVERLAY_MIN_INTERVAL_MS = 33L

        // 纹理四边形着色器（底图 png 与 canvas 图层共用）
        private const val VERTEX_SHADER_TEX = """
            uniform mat4 u_MVPMatrix;
            attribute vec2 a_Position;
            attribute vec2 a_TexCoord;
            varying vec2 v_TexCoord;
            void main() {
                gl_Position = u_MVPMatrix * vec4(a_Position, 0.0, 1.0);
                v_TexCoord = a_TexCoord;
            }
        """
    }

    // 状态标志
    private var isDetailedEnabled = false
    // 跨线程：激光线程(主线程)写、GLThread 读，必须 volatile
    @Volatile
    private var pointCloudDirty = true
    // 需要全量重建累积点云（回环改写位姿 / 数据清空后）
    @Volatile
    private var pointCloudFullDirty = true
    private var keyframeGeometryDirty = true
    private var drawWhiteBackground = false           // 默认透明

    private val keyFrames3D = ConcurrentHashMap<Int, KeyFrame>()

    // 点/线着色器
    private var programPoint = 0
    private var aPosPoint = 0
    private var uColorPoint = 0
    private var uMVPPoint = 0
    private var uPointSize = 0
    private var uRoundPoint = 0
    private var uCharHeightText = 0
    private var uTextColorText = 0

    // 文字着色器
    private var programText = 0
    private var aVertexText = 0
    private var aTexOffsetText = 0
    private var aWorldPosText = 0
    private var aUminText = 0
    private var aUmaxText = 0
    private var uMVPTex = 0
    private var uCharAspectScaleTex = 0
    private var uTextureTex = 0

    // 机器人着色器
    private var programRobot = 0
    private var aPosRobot = 0
    private var aTexRobot = 0
    private var uMVPRobot = 0
    private var uModelMatrixRobot = 0
    private var uTextureRobot = 0

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    // VBO
    private var vboLivePoint = intArrayOf(0) // 实时上激光点云 VBO
    private var vboKeyframePoints = intArrayOf(0)
    private var vboKeyframeLines = intArrayOf(0)
    private var vboTextGeometry = intArrayOf(0)
    private var vboTextInstance = intArrayOf(0)
    private var vboRobot = intArrayOf(0)

    private var pointCloudVertexCount = 0
    private var keyframePointCount = 0
    private var keyframeLineVertexCount = 0
    private var textInstanceCount = 0
    private var liveVertexCount = 0 // 实时上激光点云顶点数
    private var liveDirty = false // 实时点云是否需要重建 VBO

    private var keyframePointsLastLen = 0
    private var keyframeLinesLastLen = 0
    private var lastLenLive = 0   // 实时点云上次长度

    // ──────────────── 累积点云 VBO 增量上传状态 ────────────────
    // 设计：关键帧只会按 id 追加（addKeyFrames 里 containsKey 已挡掉重复 id），
    // 因此点云可以"只追加不重建"。staging 是 Java 侧镜像，VBO 按容量分配，
    // 新数据用 glBufferSubData 追加到尾部，避免每个关键帧全量重拷 + glBufferData 重分配。
    // 线程约定：keyFrameOrder 与 keyFrames3D 共用一把锁；其余字段只在 GL 线程读写。
    private val keyFrameOrder = ArrayList<KeyFrame>()   // 关键帧追加顺序（受 keyFrames3D 锁保护）
    private val keyFrameIds = ArrayList<Int>()          // 与 keyFrameOrder 平行的 id（文字按顺序避让用）
    private var cloudPointTrimIndex = 0                 // 已释放 cloudPoints 的帧数
    private var pcUploadedKeyframes = 0                 // 已并入 staging 的关键帧数

    // 最小 staging 容量（float）：避免重建后从 0 开始频繁扩容
    private val PC_MIN_CAP = 4096
    private var liveBuffer: FloatBuffer? = null
    private var liveBufferCapacity = 0

    // 实时点云跨线程交换锁：写入方为激光数据回调线程(updateLiveScan)，读取方为GLThread
    private val liveLock = Any()

    // GLThread专用：当前实时点云VBO中实际顶点数（与VBO内容严格一致；
    // 不能直接用跨线程的liveVertexCount绘制，否则count可能与VBO数据不匹配）
    private var liveDrawCount = 0

    // 实时扫描关键帧复用缓存（grow-only，避免每次关键帧重新分配）
    private var liveKeyframeCloudBuf: FloatArray? = null
    private var liveKeyframeWorldBuf: FloatArray? = null

    private var keyframePointsArray: FloatArray? = null
    private var keyframeLinesArray: FloatArray? = null

    private var uploadBuffer: FloatBuffer? = null
    private var uploadBufferCapacity = 0

    private val worldToPixelMatrix = Matrix()
    private val totalMatrix = Matrix()
    private val matrixValues = FloatArray(9)
    private var lastRangeSize = 0

    private var textTexture = 0
    private var charUmin = FloatArray(11)
    private var charUmax = FloatArray(11)
    private var texAspectScale = 1f

    // 机器人相关
    private var robotPose = FloatArray(3)
    private var robotBitmap: Bitmap? = null
    private var robotTexture = 0

    // ─────────── 方案A：底图 / 网格 / canvas 图层（同一个 GL Surface） ───────────

    // 纹理四边形着色器
    private var programTex = 0
    private var aPosTex = 0
    private var aTexCoordTex = 0
    private var uMVPTexQuad = 0
    private var uTextureQuad = 0

    // 单位四边形（0..1）+ 纹理坐标，底图与 overlay 共用
    private var vboQuad = intArrayOf(0)
    private val quadModelGl = FloatArray(16)
    private val quadMvp = FloatArray(16)

    // 底图（png）纹理
    private var pngTexture = 0
    private var pngTextureVersion = -1
    private var pngBitmapRef: Bitmap? = null   // 已上传纹理对应的位图实例（防止跨线程读到版本/位图不一致）
    private var pngQuadWidth = 0f      // 底图在"地图原始像素"坐标下的宽
    private var pngQuadHeight = 0f
    private val pngOuterMatrix = Matrix()   // PngMapView 的矩阵快照
    private val pngModelMatrix = Matrix()
    private var maxTextureSize = 4096

    // 网格线
    private var vboGrid = intArrayOf(0)
    // 方案C：world-space 图层 VBO
    private var vboOldKfPoints = intArrayOf(0)
    private var vboOldKfTicks = intArrayOf(0)
    private var vboOldKfText = intArrayOf(0)
    private var vboNodePoints = intArrayOf(0)
    private var vboNodeText = intArrayOf(0)
    private var vboUpLaser = intArrayOf(0)

    private var oldKfPointCount = 0
    private var oldKfTickCount = 0
    private var oldKfTextCount = 0
    private var oldKfVersion = -1
    private var oldKfTickScale = Float.NaN
    private var oldKfSnapshot = FloatArray(0)

    private var nodePointCount = 0
    private var nodeTextCount = 0
    private var nodeVersion = -1

    private var upLaserCount = 0
    private var upLaserVersion = -1

    private var nodeSnapshot = FloatArray(0)
    private var oldKfTextHeight = 0f
    private var liveTextHeight = 0f
    private var nodeTextHeight = 0f
    private var oldKfTextDirty = true
    private var nodeTextDirty = true

    // 上一次生成实时编号时的视野范围，用于判断"平移后是否需要重建"
    private var textVisMinX = Float.NaN
    private var textVisMinY = Float.NaN
    private var textVisMaxX = Float.NaN
    private var textVisMaxY = Float.NaN

    // 一帧内所有图层共用的矩阵/参数快照（避免读到 UI 线程正在改的中间状态）
    private val frameMatrix = Matrix()
    private var frameResolution = 0.05f
    private var frameScale = 1f
    /** 手势进行中：降级渲染（跳过文字/网格、点径降到 1px），保证跟手 */
    @Volatile
    private var interacting = false

    // 视野裁剪：屏幕四角反变换到世界，得到当前可见世界 AABB
    private val viewCorners = FloatArray(8)
    private val invFrameMatrix = Matrix()
    private var visMinX = 0f
    private var visMinY = 0f
    private var visMaxX = 0f
    private var visMaxY = 0f
    private var visValid = false

    private var gridVertexCount = 0
    private var gridLastLen = 0
    private var gridArray: FloatArray? = null
    private val gridWorldToPixel = Matrix()
    private val gridTotal = Matrix()
    private val gridValues = FloatArray(9)
    private val gridAnchor = FloatArray(2)

    // canvas 图层（约束节点/关键帧/扩展区/非建图激光）离屏位图 + 纹理
    private var overlayTexture = 0
    private var overlayBitmap: Bitmap? = null
    private var overlayCanvas: Canvas? = null
    private var overlayTextureUploaded = false
    @Volatile
    private var overlayDirty = true
    private var overlayLastRefreshMs = 0L
    private val overlayModelGl = FloatArray(16)
    private val overlayModelMatrix = Matrix()

    private var surfaceW = 0
    private var surfaceH = 0

    // ---------- 渲染节流 ----------
    // 每收到一个激光数据包就会 requestRender()（updateLiveScan / updateRobotPose），
    // 而每一帧都是全场景重绘：累积黑点云 + 关键帧点 + 关键帧线 + 文字 + 实时红点 + 机器人。
    // 黑点云规模随建图时长线性增长，若不限流，GL 线程会被数据包率直接打满，
    // 结果反而是最需要实时性的红色点云滞后于机器人。这里统一限到 ~25fps。
    //
    // 采用 leading + trailing 策略：窗口内的首个请求会安排一个补偿帧，
    // 保证最后一次数据更新一定会被画出来（否则机器人停下后红点云会停在旧位置）。
    //
    // 注意：拖动 / 缩放 / 旋转走 [requestRenderImmediate]，不参与节流。
    private val renderMinIntervalMs = RENDER_MIN_INTERVAL_MS
    private val lastRenderTimeMs = AtomicLong(0L)
    private val renderTrailingScheduled = AtomicBoolean(false)
    private val renderHandler = Handler(Looper.getMainLooper())

    /**
     * 立即请求渲染，跳过节流窗口。
     *
     * 只给交互路径用（拖动 / 缩放 / 旋转）：手势过程中若被限流，GL 层会明显跟不上手指。
     * 数据刷新路径（激光点云、关键帧、位姿、回环）一律走节流的 [requestRender]。
     *
     * 这里刻意不更新 [lastRenderTimeMs]：交互帧与数据帧各自独立计时，
     * 否则持续拖动会不断推后节流基准，把实时红点云一起拖慢。
     */
    private fun requestRenderImmediate() {
        super.requestRender()
    }

    override fun requestRender() {
        // 任何一次重绘请求都意味着"图层内容/矩阵可能已变化"，标记 overlay 纹理失效
        overlayDirty = true
        val now = SystemClock.elapsedRealtime()
        val last = lastRenderTimeMs.get()
        val elapsed = now - last
        // leading：距上一帧已足够久，立即渲染
        if (elapsed >= renderMinIntervalMs && lastRenderTimeMs.compareAndSet(last, now)) {
            requestRenderImmediate()
            return
        }
        // trailing：窗口内已有请求在排队补帧则跳过（补帧时读的是最新数据，不会丢更新）
        if (renderTrailingScheduled.compareAndSet(false, true)) {
            val delay = (renderMinIntervalMs - elapsed).coerceIn(0L, renderMinIntervalMs)
            renderHandler.postDelayed({
                renderTrailingScheduled.set(false)
                lastRenderTimeMs.set(SystemClock.elapsedRealtime())
                requestRenderImmediate()
            }, delay)
        }
    }

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        // 方案A：底图/网格/图层/点云/机器人全部在本 Surface 输出，自身铺满整块区域并绘制白色背景。
        // 因此不再调用 holder.setFormat(PixelFormat.TRANSLUCENT)，
        // 沿用 SurfaceView 默认的不透明格式（省掉与下层地图 Surface 的透明混合，HWC 可直接走 overlay）。
        setZOrderOnTop(false)
        setZOrderMediaOverlay(false)
        setRenderer(this)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun setDrawingEnabled(enabled: Boolean) {
        if (isDetailedEnabled != enabled) {
            isDetailedEnabled = enabled
            if (enabled) {
                keyframeGeometryDirty = true   // 强制重建
            }
            requestRender()
        }
    }

    fun setWorkMode(mode: WorkMode) {}

    /** 清空累积点云与关键帧（供"重新开始建图"使用） */
    fun resetPointCloud() {
        synchronized(keyFrames3D) {
            keyFrames3D.clear()
            keyFrameOrder.clear()
            keyFrameIds.clear()
            cloudPointTrimIndex = 0
            pcUploadedKeyframes = 0
            // 这里在 UI 线程，不能做 glDeleteBuffers；
            // 置 fullDirty 后由 GL 线程在 rebuildPointCloudVBO 里统一 releaseAllChunks
        }
        pointCloudVertexCount = 0
        pointCloudFullDirty = true
        pointCloudDirty = true
        keyframeGeometryDirty = true
        keyframePointCount = 0
        keyframeLineVertexCount = 0
        textInstanceCount = 0
        requestRender()
    }

    /**
     * 调试自校验用：点云分块统计。
     * total 是累积点数，drawn 是本帧实际绘制点数（视野剔除后），chunks 是"总数/可见数"。
     */
    fun debugPointCloudStats(): String = synchronized(keyFrames3D) {
        var capPoints = 0
        var maxChunkBytes = 0
        for (c in cloudChunks) {
            capPoints += c.capacityPoints
            val b = c.capacityPoints * 8
            if (b > maxChunkBytes) maxChunkBytes = b
        }
        buildingChunk?.let {
            capPoints += it.capacityPoints
            val b = it.capacityPoints * 8
            if (b > maxChunkBytes) maxChunkBytes = b
        }
        var lodPoints = 0
        for (c in cloudChunks) lodPoints += c.lodPointCount
        "kf=${keyFrameOrder.size} total=$pointCloudVertexCount drawn=$lastDrawnCloudPoints " +
                "lod=$lodPoints " +
                "liveTxt=$textInstanceCount oldTxt=$oldKfTextCount " +
                "chunks=${cloudChunks.size}/${lastVisibleChunks} " +
                "capMB=${capPoints * 8 / 1048576} maxChunkKB=${maxChunkBytes / 1024} " +
                "vis=[${visMinX.toInt()},${visMinY.toInt()}," +
                "${visMaxX.toInt()},${visMaxY.toInt()}]"
    }

    fun addKeyFrames(
        laserData: laser_t,
        cloudBuf: FloatArray?,
        worldBuf: FloatArray?,
        pointCount: Int
    ) {
        val mapView = parent.get() ?: return
        val rad0 = laserData.rad0.toInt()
        if (rad0 != -1 && rad0 >= 0) {
            if (!keyFrames3D.containsKey(rad0)) {
                if (rad0 == 0) {
                    mapView.mConstrainNodes?.addConstraintNodes(
                        ConstraintNode(
                            rad0,
                            mapView.robotPose[0].toDouble(),
                            mapView.robotPose[1].toDouble(),
                            mapView.robotPose[2].toDouble()
                        )
                    )
                }
                val cloudCopy = cloudBuf?.copyOf(pointCount * 2)
                val worldCopy = worldBuf?.copyOf(pointCount * 2)
                val kf = KeyFrame(
                    cloudPoints = cloudCopy,
                    worldPoints = worldCopy,
                    robotPos = mapView.robotPose.clone()
                )
                synchronized(keyFrames3D) {
                    keyFrames3D[rad0] = kf
                    // 追加顺序索引：增量上传依赖它，不能靠 ConcurrentHashMap 的遍历顺序
                    keyFrameOrder.add(kf)
                    keyFrameIds.add(rad0)
                    // 释放过老关键帧的原始激光点（parseOptPose 遇到 null 会跳过，该帧保持旧位姿）
                    while (cloudPointTrimIndex < keyFrameOrder.size - KEEP_CLOUD_POINT_FRAMES) {
                        keyFrameOrder[cloudPointTrimIndex].cloudPoints = null
                        cloudPointTrimIndex++
                    }
                    mapView.isStartRevSubMaps = true
                    pointCloudDirty = true
                    keyframeGeometryDirty = true
                }
                requestRender()
            }
        }
    }

    /**
     * 建图实时上激光点云计算（由 CreateMapView3D 直接调用，替代原 UpLaserScanView3D 的计算职责）。
     * 内部同步完成：实时红点世界坐标计算 + 关键帧收集。
     */
    fun updateLiveScan(laserData: laser_t) {
        if (laserData.ranges.size <= 6) {
            synchronized(liveLock) { liveVertexCount = 0 }
            liveDirty = true
            requestRender()
            return // 最少包含机器人位置数据
        }
        val mapView = parent.get() ?: return

        val isKeyframe = laserData.rad0.toInt() != -1
        val totalPoints = (laserData.ranges.size - 6) / 3
        // 关键帧黑点采样间隔：固定常量。
        // 注意：这里刻意不再跟随视图缩放（原实现是 max(5, 1/scale)）——
        // 存下来的黑点是持久数据，与"当前缩放"无关，跟着缩放变会导致同一段路
        // 缩得远一点走一遍、凑近走一遍，落库的点密度不同，数据不可复现。
        val keyframeSampleInterval = KEYFRAME_SAMPLE_INTERVAL

        // 实时红点：完全显示，不过滤（下一帧整体替换，无需压缩）
        val required = totalPoints * 2

        // 关键帧缓存数组（按采样间隔容量申请，复用、仅扩容），黑点存储需控量
        var localCloudBuf: FloatArray? = null
        var localWorldBuf: FloatArray? = null
        if (isKeyframe) {
            val keyframeRequired = (totalPoints / keyframeSampleInterval + 1) * 2
            if (liveKeyframeCloudBuf == null || liveKeyframeCloudBuf!!.size < keyframeRequired) {
                liveKeyframeCloudBuf = FloatArray(keyframeRequired)
            }
            if (liveKeyframeWorldBuf == null || liveKeyframeWorldBuf!!.size < keyframeRequired) {
                liveKeyframeWorldBuf = FloatArray(keyframeRequired)
            }
            localCloudBuf = liveKeyframeCloudBuf
            localWorldBuf = liveKeyframeWorldBuf
        }

        val robotX = mapView.robotPose[0]
        val robotY = mapView.robotPose[1]
        val robotTheta = mapView.robotPose[2]
        val cosT = cos(robotTheta)
        val sinT = sin(robotTheta)

        var pointCount = 0
        var keyIdx = 0
        // 实时红点缓冲区写入全程持锁（GLThread在锁内拷贝快照后再上传）：
        // 修复崩溃——此前GLThread直接读取liveBuffer，若恰好读到clear()与flip()
        // 之间写了一半的状态（position>0且缓冲区通常恰好装满），glBufferSubData
        // 的JNI层检查remaining()<size会抛IllegalArgumentException使GLThread崩溃
        synchronized(liveLock) {
            if (liveBuffer == null || liveBufferCapacity < required) {
                liveBuffer = ByteBuffer.allocateDirect(required * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer()
                liveBufferCapacity = required
            } else {
                liveBuffer!!.clear()
            }
            val buf = liveBuffer!!
            // 实时红点：全量遍历（step=1，完全显示，不降采样）
            for (i in 0 until totalPoints) {
                val index = 6 + i * 6 // 跳过机器人位置数据（前6个元素）
                if (index + 2 >= laserData.ranges.size) break // 越界保护

                val laserX = laserData.ranges[index]
                val laserY = laserData.ranges[index + 1]
                val worldX = laserX * cosT - laserY * sinT + robotX
                val worldY = laserX * sinT + laserY * cosT + robotY

                // 实时红点：全量写入
                if (pointCount * 2 + 1 < required) {
                    buf.put(worldX)
                    buf.put(worldY)
                    pointCount++
                }

                // 关键帧黑点：按采样间隔收集，控制存储量
                if (isKeyframe && localCloudBuf != null && localWorldBuf != null && i % keyframeSampleInterval == 0) {
                    if (keyIdx + 1 < localCloudBuf.size) {
                        localCloudBuf[keyIdx] = laserX
                        localCloudBuf[keyIdx + 1] = laserY
                        localWorldBuf[keyIdx] = worldX
                        localWorldBuf[keyIdx + 1] = worldY
                        keyIdx += 2
                    }
                }
            }
            buf.flip()
            liveVertexCount = pointCount
        }
        liveDirty = true

        if (isKeyframe && keyIdx > 0 && localCloudBuf != null && localWorldBuf != null) {
            addKeyFrames(laserData, localCloudBuf, localWorldBuf, keyIdx / 2)
        }
        requestRender()
    }

    fun parseOptPose(laserData: laser_t) {
        if (laserData.ranges.isEmpty() || lastRangeSize == laserData.ranges.size) return
        lastRangeSize = laserData.ranges.size
        var hasUpdate = false
        synchronized(keyFrames3D) {
            val size = laserData.ranges.size
            for (i in 0 until size step 4) {
                val id = laserData.ranges[i].toInt()
                val x = laserData.ranges[i + 1]
                val y = laserData.ranges[i + 2]
                val theta = laserData.ranges[i + 3]
                val kf = keyFrames3D[id] ?: continue
                kf.robotPos[0] = x
                kf.robotPos[1] = y
                kf.robotPos[2] = theta

                val cloud = kf.cloudPoints ?: continue
                val world = kf.worldPoints ?: continue
                val cosT = cos(theta)
                val sinT = sin(theta)
                val n = kf.pointCount
                for (j in 0 until n) {
                    val cx = cloud[2 * j]
                    val cy = cloud[2 * j + 1]
                    world[2 * j] = cx * cosT - cy * sinT + x
                    world[2 * j + 1] = cx * sinT + cy * cosT + y
                }
                hasUpdate = true
            }
            if (hasUpdate) {
                // worldPoints 是就地改写，增量上传感知不到位姿变化，必须全量重建
                pointCloudFullDirty = true
                pointCloudDirty = true
                keyframeGeometryDirty = true
                requestRender()
            }
        }
    }

    /**
     * 地图矩阵变化（拖动 / 缩放 / 旋转）时调用。
     * 属交互路径，不参与节流，直接渲染以保证 GL 层跟手。
     */
    fun notifyMatrixChanged() {
        overlayDirty = true
        requestRenderImmediate()
    }

    /**
     * 请求刷新 canvas 图层纹理（约束节点/关键帧/扩展区/非建图激光）。
     * 由 CreateMapView3D 在"需要刷新画面"的时机调用，语义等价于原 Canvas 路径的 requestRender。
     */
    fun requestOverlayRefresh() {
        overlayDirty = true
        requestRender()
    }

    /** 设置机器人位姿（弧度），由 CreateMapView3D 调用 */
    fun updateRobotPose(x: Float, y: Float, theta: Float) {
        robotPose[0] = x
        robotPose[1] = y
        robotPose[2] = theta
        requestRender()
    }

    /** 设置机器人图标，需在 OpenGL 初始化前调用 */
    fun setRobotBitmap(bitmap: Bitmap) {
        robotBitmap = bitmap
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        // 方案A：每条帧先铺白底（等价于原 canvas.drawColor(Color.WHITE)）
        GLES20.glClearColor(1f, 1f, 1f, 1f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // 点/线
        programPoint = createProgram(VERTEX_SHADER_POINT, FRAGMENT_SHADER_POINT)
        aPosPoint = GLES20.glGetAttribLocation(programPoint, "a_Position")
        uColorPoint = GLES20.glGetUniformLocation(programPoint, "u_Color")
        uMVPPoint = GLES20.glGetUniformLocation(programPoint, "u_MVPMatrix")
        uPointSize = GLES20.glGetUniformLocation(programPoint, "u_PointSize")
        uRoundPoint = GLES20.glGetUniformLocation(programPoint, "u_Round")

        // 文字
        programText = createProgram(VERTEX_SHADER_TEXT, FRAGMENT_SHADER_TEXT)
        aVertexText = GLES20.glGetAttribLocation(programText, "a_Vertex")
        aTexOffsetText = GLES20.glGetAttribLocation(programText, "a_TexOffset")
        aWorldPosText = GLES20.glGetAttribLocation(programText, "a_WorldPos")
        aUminText = GLES20.glGetAttribLocation(programText, "a_Umin")
        aUmaxText = GLES20.glGetAttribLocation(programText, "a_Umax")
        uMVPTex = GLES20.glGetUniformLocation(programText, "u_MVPMatrix")
        uCharAspectScaleTex = GLES20.glGetUniformLocation(programText, "u_CharAspectScale")
        uCharHeightText = GLES20.glGetUniformLocation(programText, "u_CharWorldHeight")
        uTextColorText = GLES20.glGetUniformLocation(programText, "u_TextColor")
        uTextureTex = GLES20.glGetUniformLocation(programText, "u_Texture")

        // 机器人
        programRobot = createProgram(VERTEX_SHADER_ROBOT, FRAGMENT_SHADER_ROBOT)
        aPosRobot = GLES20.glGetAttribLocation(programRobot, "a_Position")
        aTexRobot = GLES20.glGetAttribLocation(programRobot, "a_TexCoord")
        uMVPRobot = GLES20.glGetUniformLocation(programRobot, "u_MVPMatrix")
        uModelMatrixRobot = GLES20.glGetUniformLocation(programRobot, "u_ModelMatrix")
        uTextureRobot = GLES20.glGetUniformLocation(programRobot, "u_Texture")

        // 纹理四边形（底图 png / canvas 图层）
        programTex = createProgram(VERTEX_SHADER_TEX, FRAGMENT_SHADER_ROBOT)
        aPosTex = GLES20.glGetAttribLocation(programTex, "a_Position")
        aTexCoordTex = GLES20.glGetAttribLocation(programTex, "a_TexCoord")
        uMVPTexQuad = GLES20.glGetUniformLocation(programTex, "u_MVPMatrix")
        uTextureQuad = GLES20.glGetUniformLocation(programTex, "u_Texture")

        // 固定几何体 VBO（文字四边形）
        val quadVertices = floatArrayOf(
            -0.5f, -0.5f, 0f, 0f,
            0.5f, -0.5f, 1f, 0f,
            -0.5f, 0.5f, 0f, 1f,
            0.5f, 0.5f, 1f, 1f
        )
        val quadBuffer = ByteBuffer.allocateDirect(quadVertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        quadBuffer.put(quadVertices).position(0)
        GLES20.glGenBuffers(1, vboTextGeometry, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboTextGeometry[0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            quadVertices.size * 4,
            quadBuffer,
            GLES20.GL_STATIC_DRAW
        )

        // 实例 VBO
        GLES20.glGenBuffers(1, vboTextInstance, 0)

        // 机器人几何体（四边形，顶点坐标+纹理坐标）
        val half = ROBOT_SIZE / 2f
//        val robotVertices = floatArrayOf(
//            -half, -half, 0f, 0f,
//            half, -half, 1f, 0f,
//            -half,  half, 0f, 1f,
//            half,  half, 1f, 1f
//        )

        // 在 onSurfaceCreated 中创建机器人 VBO 时：
        val robotVertices = floatArrayOf(
            -0.5f, -0.5f, 0f, 0f,
            0.5f, -0.5f, 1f, 0f,
            -0.5f, 0.5f, 0f, 1f,
            0.5f, 0.5f, 1f, 1f
        )
        val robotBuffer = ByteBuffer.allocateDirect(robotVertices.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        robotBuffer.put(robotVertices).position(0)
        GLES20.glGenBuffers(1, vboRobot, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboRobot[0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            robotVertices.size * 4,
            robotBuffer,
            GLES20.GL_STATIC_DRAW
        )

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        // 单位四边形（0..1），底图/图层纹理都用它 + 模型矩阵缩放
        val unitQuad = floatArrayOf(
            0f, 0f, 0f, 0f,
            1f, 0f, 1f, 0f,
            0f, 1f, 0f, 1f,
            1f, 1f, 1f, 1f
        )
        val unitBuffer = ByteBuffer.allocateDirect(unitQuad.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        unitBuffer.put(unitQuad).position(0)
        GLES20.glGenBuffers(1, vboQuad, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboQuad[0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            unitQuad.size * 4,
            unitBuffer,
            GLES20.GL_STATIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        // 纹理
        textTexture = createDigitTexture()
        // 机器人纹理在 setRobotBitmap 后上传，这里先创建占位
        robotTexture = 0
        uploadRobotTexture()

        // 上下文可能重建：底图/图层纹理需要重新上传
        pngTexture = 0
        pngTextureVersion = -1
        pngBitmapRef = null
        overlayTexture = 0
        overlayTextureUploaded = false
        overlayDirty = true

        // 单张纹理最大尺寸（底图解码已限制在 3000 以内，这里只是防御性校验）
        val sizeArr = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, sizeArr, 0)
        if (sizeArr[0] > 0) maxTextureSize = sizeArr[0]
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        GMatrix.orthoM(projectionMatrix, 0, 0f, width.toFloat(), height.toFloat(), 0f, -1f, 1f)
        surfaceW = width
        surfaceH = height
        // canvas 图层纹理与 Surface 同尺寸（1:1 屏幕像素），尺寸变化时重建
        ensureOverlayBitmap(width, height)
        overlayDirty = true
    }

    override fun onDrawFrame(gl: GL10?) {
        val mapView = parent.get() ?: return
        updateMVPMatrix(mapView)

        // 白底（等价于原 canvas.drawColor(Color.WHITE)）
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        // 每帧算一次可见世界 AABB，供文字裁剪 / 点云分块剔除使用
        updateVisibleWorldRect()

        // ① 50cm 网格线（屏幕正交网格，画在底图之前/之下，与原绘制顺序一致）
        // 手势期间跳过：网格每帧重建 + 上百条线，交互期优先保证跟手
        if (!interacting) {
            rebuildGridVertices(mapView)
            if (gridVertexCount > 0) {
                drawLines(vboGrid[0], gridVertexCount, COLOR_GRID, projectionMatrix)
            }
        }

        // ② 底图 png（原逻辑：白底之后、图层之前，处于网格之上、点云之下）
        uploadPngIfNeeded(mapView)
        drawPng(mapView)

        // ③ 仅扩展区域还需要离屏快照（canvas 交互图层）；其余图层已改为下面的 world-space VBO
        if (mapView.overlayNeeded) {
            refreshOverlayIfNeeded(mapView)
            drawOverlay()
        }

        // 网格用了屏幕正交投影，这里把点/线着色器的 MVP 恢复成世界->屏幕矩阵
        GLES20.glUseProgram(programPoint)
        GLES20.glUniformMatrix4fv(uMVPPoint, 1, false, mvpMatrix, 0)

        // ③.5 方案C：约束节点 / 旧关键帧 / 非建图激光
        // 与底图共用同一帧矩阵快照，直接从 VBO 绘制，不再经过位图快照，因此缩放恒不错位
        // 手势期间只画点/线、跳过文字：拖动时文字不是关注点，却能省掉大量实例绘制
        syncWorldLayers(mapView)
        // 文字已经按"视野 + 屏幕间距避让"稀疏到几十个，成本很低，手势期间不再跳过，
        // 否则拖动时编号整体消失，看起来像"数据没了"
        drawWorldLayers(includeText = true)

        if (pointCloudDirty) {
            rebuildPointCloudVBO()
            pointCloudDirty = false
        }
        // 点云是纯色不透明，关掉混合可省一大截带宽（20000 步时是最大填充源）
        val cloudPointSize = if (interacting) POINT_SIZE_CLOUD_INTERACT else POINT_SIZE_CLOUD
        // 分帧生成体素 LOD：每帧最多 2 万点，摊到多帧完成，避免块封口时的顿挫
        processLodQueue()

        if (pointCloudVertexCount > 0) {
            GLES20.glDisable(GLES20.GL_BLEND)
            drawCloudChunks(COLOR_POINT_CLOUD, cloudPointSize)
            GLES20.glEnable(GLES20.GL_BLEND)
        }

        if (keyframeGeometryDirty) rebuildKeyframePointsVBO()
        if (keyframePointCount > 0) {
            drawPoints(
                vboKeyframePoints[0],
                keyframePointCount,
                COLOR_KEYFRAME,
                POINT_SIZE_KEYFRAME
            )
        }

        if (isDetailedEnabled) {
            // 平移/缩放结束后视野变了，但数据没更新 -> 实时编号不会自动重建，
            // 于是"只停在上一屏"。这里在非手势状态下检测视野变化，补建一次。
            if (!interacting && viewRectChangedSinceText()) {
                keyframeGeometryDirty = true
            }
            if (keyframeGeometryDirty) {
                rebuildKeyframeLinesVBO()
                rebuildTextVBO()
            }
            if (keyframeLineVertexCount > 0) {
                drawLines(vboKeyframeLines[0], keyframeLineVertexCount, COLOR_LINE)
            }
            if (textInstanceCount > 0) {
                // 与旧关键帧统一走同一个管线，保证字号/缩放逻辑完全一致
                drawTextGroup(vboTextInstance[0], textInstanceCount, liveTextHeight, COLOR_LIVE_TEXT)
            }
        }

        // 绘制实时上激光点云（红，始终在黑色关键帧点云之上）
        if (liveDirty) {
            rebuildLivePointVBO()
            liveDirty = false
        }
        if (liveDrawCount > 0) {
            drawPoints(vboLivePoint[0], liveDrawCount, COLOR_LIVE_POINT, POINT_SIZE_LIVE)
        }

        // 最后绘制机器人（在所有元素之上）
        drawRobot()

        if (keyframeGeometryDirty) keyframeGeometryDirty = false

        // 方案A：通知地图视图"GL 已产出首帧"，此后 CreateMapView3D 的 SurfaceView 停止出图
        mapView.onGlFrameReady()
    }

    // ---------- 方案A：底图 / 网格 / canvas 图层 ----------

    /**
     * 生成 50cm 屏幕正交网格线顶点。
     *
     * 算法与 CreateMapView3D.drawGrid 完全一致（同样的锚点、格距、跳过条件、绘制顺序），
     * 仅把 canvas.drawLine 改成 GL_LINES 顶点；网格线始终水平/竖直，只跟随缩放与平移。
     */
    private fun rebuildGridVertices(mapView: CreateMapView3D) {
        gridVertexCount = 0
        val w = surfaceW
        val h = surfaceH
        if (w <= 0 || h <= 0) return

        // 给默认值：synchronized 块内的赋值编译器不保证一定执行（部分 Kotlin/IDE 版本会报未初始化）
        var res = 0.05f
        var originX = 0f
        var originY = 0f
        var mapHeight = 0f
        synchronized(mapView.mSrf.mapData) {
            val mapData = mapView.mSrf.mapData
            res = mapData.resolution
            originX = mapData.originX
            originY = mapData.originY
            mapHeight = mapData.height
        }
        if (res <= 0.0001f) res = 0.05f
        // 地图元数据未就绪（尚未收到首帧激光数据）时不绘制
        if (mapHeight <= 0f) return

        // 世界坐标 -> 地图像素坐标：px=(wx-originX)/res, py=height-(wy-originY)/res
        gridWorldToPixel.reset()
        gridWorldToPixel.postTranslate(-originX, -originY)
        gridWorldToPixel.postScale(1f / res, -1f / res)
        gridWorldToPixel.postTranslate(0f, mapHeight)

        gridTotal.set(mapView.outerMatrix)
        gridTotal.preConcat(gridWorldToPixel)
        gridTotal.getValues(gridValues)

        // 与 CreateMapView3D.drawGrid 一致：格距固定 40px（不随缩放变化），
        // 锚点取机器人位置的屏幕坐标，使机器人始终落在网格交叉点上。
        gridAnchor[0] = mapView.robotPose[0]
        gridAnchor[1] = mapView.robotPose[1]
        gridTotal.mapPoints(gridAnchor)
        val anchorX = gridAnchor[0]
        val anchorY = gridAnchor[1]
        if (!anchorX.isFinite() || !anchorY.isFinite()) return

        val cellPx = GRID_CELL_PX
        if (cellPx <= 0f) return

        val needFloats =
            (((w / cellPx).toInt() + (h / cellPx).toInt() + 4) * 4)
        var arr = gridArray
        if (arr == null || arr.size < needFloats) {
            arr = FloatArray(needFloats)
            gridArray = arr
        }
        var i = 0

        // 竖线：x = anchorX + n * cellPx
        var n = ceil((0f - anchorX) / cellPx).toInt()
        val nEnd = floor((w - anchorX) / cellPx).toInt()
        while (n <= nEnd && i + 3 < arr.size) {
            val x = anchorX + n * cellPx
            arr[i++] = x
            arr[i++] = 0f
            arr[i++] = x
            arr[i++] = h.toFloat()
            n++
        }

        // 横线：y = anchorY + j * cellPx
        var j = ceil((0f - anchorY) / cellPx).toInt()
        val jEnd = floor((h - anchorY) / cellPx).toInt()
        while (j <= jEnd && i + 3 < arr.size) {
            val y = anchorY + j * cellPx
            arr[i++] = 0f
            arr[i++] = y
            arr[i++] = w.toFloat()
            arr[i++] = y
            j++
        }

        gridVertexCount = i / 2
        uploadVBO(vboGrid, arr, i, gridLastLen)
        gridLastLen = i
    }

    /**
     * 底图位图变化时重传纹理。
     * 位图有可能是解码压缩后的（最大边 ≤ 3000），这里用 drawScale 还原成"地图原始像素"尺寸，
     * 后续绘制仍然走 outerMatrix（像素→屏幕），因此与原逻辑的位置/尺寸完全一致。
     */
    private fun uploadPngIfNeeded(mapView: CreateMapView3D) {
        val bitmap = mapView.getPngBitmap() ?: return
        val version = mapView.getPngBitmapVersion()
        if (pngTexture != 0 && version == pngTextureVersion && bitmap === pngBitmapRef) return
        if (bitmap.isRecycled) return
        if (bitmap.width > maxTextureSize || bitmap.height > maxTextureSize) {
            Log.w(
                TAG,
                "png bitmap ${bitmap.width}x${bitmap.height} exceeds GL_MAX_TEXTURE_SIZE=$maxTextureSize"
            )
            pngTextureVersion = version
            pngBitmapRef = bitmap
            return
        }

        val drawScale = mapView.getPngDrawScale()
        pngQuadWidth = bitmap.width * drawScale
        pngQuadHeight = bitmap.height * drawScale

        if (pngTexture == 0) {
            val tex = IntArray(1)
            GLES20.glGenTextures(1, tex, 0)
            pngTexture = tex[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, pngTexture)
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE
            )
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, pngTexture)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        pngTextureVersion = version
        pngBitmapRef = bitmap
    }

    /**
     * 绘制底图。
     * 模型矩阵 = PngMapView矩阵 ∘ 平移(offset) ∘ 缩放(原始像素宽高)，
     * 与 PngMapView.onDraw 的 canvas 变换顺序（concat(matrix) → translate(offset) → scale(drawScale)）一致；
     * 矩阵取自 PngMapView 自身，保持扩展地图"增加区域"模式下底图不跟随拖动的原有行为。
     */
    private fun drawPng(mapView: CreateMapView3D) {
        if (pngTexture == 0 || pngQuadWidth <= 0f || pngQuadHeight <= 0f) return
        if (!mapView.copyPngMatrixInto(pngOuterMatrix)) return

        pngModelMatrix.set(pngOuterMatrix)
        pngModelMatrix.preTranslate(mapView.getPngOffsetX(), mapView.getPngOffsetY())
        // 位图实际像素 -> 地图原始像素
        pngModelMatrix.preScale(pngQuadWidth, pngQuadHeight)
        matrixToGL(pngModelMatrix, quadModelGl)
        drawTexturedQuad(pngTexture, quadModelGl)
    }

    /** 为 canvas 图层分配/重建与 Surface 等尺寸的离屏位图 */
    private fun ensureOverlayBitmap(w: Int, h: Int) {
        if (w <= 0 || h <= 0) return
        val cur = overlayBitmap
        if (cur != null && cur.width == w && cur.height == h && !cur.isRecycled) return
        cur?.recycle()
        val bmp = try {
            Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        } catch (e: OutOfMemoryError) {
            Log.e(TAG, "ensureOverlayBitmap OOM: ${w}x$h")
            null
        }
        overlayBitmap = bmp
        overlayCanvas = if (bmp != null) Canvas(bmp) else null
        overlayTextureUploaded = false
        overlayDirty = true
    }

    /**
     * 按需把 canvas 图层栅格化并上传纹理。
     * 与原逻辑一致：这些图层用固定屏幕尺寸绘制（不随缩放变化），所以必须在屏幕空间栅格化，
     * 矩阵或内容变化时重新生成；这里限制到 ~30fps，避免每帧上传整屏纹理。
     */
    private fun refreshOverlayIfNeeded(mapView: CreateMapView3D) {
        if (!overlayDirty) return
        val now = SystemClock.elapsedRealtime()
        if (now - overlayLastRefreshMs < OVERLAY_MIN_INTERVAL_MS) return
        val bmp = overlayBitmap ?: return
        val canvas = overlayCanvas ?: return
        if (bmp.isRecycled) return

        // 透明清屏后按原顺序绘制各图层（等价于 drawFrame 中对 mapLayers 的遍历）
        canvas.drawColor(0, PorterDuff.Mode.CLEAR)
        mapView.drawOverlayLayers(canvas)

        if (overlayTexture == 0) {
            val tex = IntArray(1)
            GLES20.glGenTextures(1, tex, 0)
            overlayTexture = tex[0]
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTexture)
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_S,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_WRAP_T,
                GLES20.GL_CLAMP_TO_EDGE
            )
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
        }

        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, overlayTexture)
        if (!overlayTextureUploaded) {
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
            overlayTextureUploaded = true
        } else {
            GLUtils.texSubImage2D(GLES20.GL_TEXTURE_2D, 0, 0, 0, bmp)
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)

        overlayLastRefreshMs = now
        overlayDirty = false
    }

    /** 把 canvas 图层纹理按 1:1 屏幕像素叠加到当前帧 */
    private fun drawOverlay() {
        if (overlayTexture == 0 || surfaceW <= 0 || surfaceH <= 0) return
        overlayModelMatrix.setScale(surfaceW.toFloat(), surfaceH.toFloat())
        matrixToGL(overlayModelMatrix, overlayModelGl)
        // 位图为预乘 alpha，使用预乘混合，避免抗锯齿/半透明边缘发黑
        GLES20.glBlendFunc(GLES20.GL_ONE, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        drawTexturedQuad(overlayTexture, overlayModelGl)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
    }

    /** 用单位四边形 + 模型矩阵绘制一张纹理 */
    private fun drawTexturedQuad(texture: Int, modelGl: FloatArray) {
        GMatrix.multiplyMM(quadMvp, 0, projectionMatrix, 0, modelGl, 0)
        GLES20.glUseProgram(programTex)
        GLES20.glUniformMatrix4fv(uMVPTexQuad, 1, false, quadMvp, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texture)
        GLES20.glUniform1i(uTextureQuad, 0)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboQuad[0])
        GLES20.glEnableVertexAttribArray(aPosTex)
        GLES20.glVertexAttribPointer(aPosTex, 2, GLES20.GL_FLOAT, false, 4 * 4, 0)
        GLES20.glEnableVertexAttribArray(aTexCoordTex)
        GLES20.glVertexAttribPointer(aTexCoordTex, 2, GLES20.GL_FLOAT, false, 4 * 4, 2 * 4)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPosTex)
        GLES20.glDisableVertexAttribArray(aTexCoordTex)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    // ---------- 机器人绘制 ----------
//    private fun drawRobot() {
//        if (robotTexture == 0) return
//        GLES20.glUseProgram(programRobot)
//        GLES20.glUniformMatrix4fv(uMVPRobot, 1, false, mvpMatrix, 0)
//
//        // 构建模型矩阵：先旋转，后平移
//        val model = FloatArray(16)
//        GMatrix.setIdentityM(model, 0)
//        GMatrix.translateM(model, 0, robotPose[0], robotPose[1], 0f)
//        GMatrix.rotateM(model, 0, Math.toDegrees(robotPose[2].toDouble()).toFloat(), 0f, 0f, 1f)
//        GLES20.glUniformMatrix4fv(uModelMatrixRobot, 1, false, model, 0)
//
//        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
//        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, robotTexture)
//        GLES20.glUniform1i(uTextureRobot, 0)
//
//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboRobot[0])
//        GLES20.glEnableVertexAttribArray(aPosRobot)
//        GLES20.glVertexAttribPointer(aPosRobot, 2, GLES20.GL_FLOAT, false, 4 * 4, 0)
//        GLES20.glEnableVertexAttribArray(aTexRobot)
//        GLES20.glVertexAttribPointer(aTexRobot, 2, GLES20.GL_FLOAT, false, 4 * 4, 2 * 4)
//
//        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
//
//        GLES20.glDisableVertexAttribArray(aPosRobot)
//        GLES20.glDisableVertexAttribArray(aTexRobot)
//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
//    }

    // 修改 drawRobot，使用动态缩放
    private fun drawRobot() {
        if (robotTexture == 0) return

        // 计算当前缩放下的屏幕像素大小
        val screenPx = ROBOT_BASE_SIZE * (cachedMapScale / cachedResolution)
        // 若小于最小阈值，则扩大世界尺寸以保证可见性
        val worldSize = if (screenPx < MIN_SCREEN_PX) {
            MIN_SCREEN_PX * cachedResolution / cachedMapScale
        } else {
            ROBOT_BASE_SIZE
        }

        GLES20.glUseProgram(programRobot)
        GLES20.glUniformMatrix4fv(uMVPRobot, 1, false, mvpMatrix, 0)

        // 构建模型矩阵：缩放 → 旋转 → 平移
        val model = FloatArray(16)
        GMatrix.setIdentityM(model, 0)
        GMatrix.translateM(model, 0, robotPose[0], robotPose[1], 0f)
        GMatrix.rotateM(model, 0, Math.toDegrees(robotPose[2].toDouble()).toFloat(), 0f, 0f, 1f)
        GMatrix.scaleM(model, 0, worldSize, worldSize, 1f)  // 注意顺序：先缩放再旋转平移，所以放在最后乘
        GLES20.glUniformMatrix4fv(uModelMatrixRobot, 1, false, model, 0)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, robotTexture)
        GLES20.glUniform1i(uTextureRobot, 0)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboRobot[0])
        GLES20.glEnableVertexAttribArray(aPosRobot)
        GLES20.glVertexAttribPointer(aPosRobot, 2, GLES20.GL_FLOAT, false, 4 * 4, 0)
        GLES20.glEnableVertexAttribArray(aTexRobot)
        GLES20.glVertexAttribPointer(aTexRobot, 2, GLES20.GL_FLOAT, false, 4 * 4, 2 * 4)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPosRobot)
        GLES20.glDisableVertexAttribArray(aTexRobot)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    // ---------- 纹理上传 ----------
    private fun uploadRobotTexture() {
        val bmp = robotBitmap ?: return
        if (robotTexture == 0) {
            val tex = IntArray(1)
            GLES20.glGenTextures(1, tex, 0)
            robotTexture = tex[0]
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, robotTexture)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    }

    // ---------- 点云、关键帧等方法保持不变（复用现有代码） ----------
    // ... 以下为现有的 rebuildPointCloudVBO, rebuildKeyframePointsVBO, drawPoints, drawLines 等函数 ...
    // 由于篇幅，这里省略，直接使用你最新版本中的实现，注意不要改动任何逻辑。

    // 需要包含所有原有方法：createDigitTexture, updateMVPMatrix, matrixToGL, uploadVBO, uploadVBOFromBuffer, obtainUploadBuffer, createProgram, loadShader 等。
    // 确保 onDetachedFromWindow 中也释放 robotTexture 和 vboRobot。

    // 此处占位，表示原来的完整逻辑（从你的最新版本中复制过来）
    // ==============================================================
    // 以下是你现有代码中的所有重建和绘制函数，保持不变
    /**
     * 点云增量上传：新关键帧直接追加到"当前块"的独立 VBO。
     * 没有全局 staging、没有全局大 VBO —— 内存与分配次数都随块数线性、可控。
     */
    private fun rebuildPointCloudVBO() {
        synchronized(keyFrames3D) {
            // 回环改写位姿 / 数据清空：所有块作废，从零重建
            if (pointCloudFullDirty) {
                pointCloudFullDirty = false
                pcUploadedKeyframes = 0
                releaseAllChunks()
            }
            for (i in pcUploadedKeyframes until keyFrameOrder.size) {
                val kf = keyFrameOrder[i]
                val world = kf.worldPoints ?: continue
                val n = minOf(kf.pointCount * 2, world.size)
                if (n <= 0) continue
                appendKfToChunk(i, world, n)
            }
            pcUploadedKeyframes = keyFrameOrder.size
            pointCloudVertexCount = chunkedPointCount()
        }
    }

    private fun chunkedPointCount(): Int {
        var total = 0
        for (c in cloudChunks) total += c.pointCount
        buildingChunk?.let { total += it.pointCount }
        return total
    }

    /** 把一个关键帧的点追加到当前块（必要时新建块 / 扩容重传） */
    private fun appendKfToChunk(kfIndex: Int, world: FloatArray, n: Int) {
        var c = buildingChunk
        if (c == null) {
            c = CloudChunk(
                kfIndex, 0, 0,
                Float.MAX_VALUE, Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE
            )
            buildingChunk = c
        }
        if (c.vboId == 0) {
            val ids = intArrayOf(0)
            GLES20.glGenBuffers(1, ids, 0)
            c.vboId = ids[0]
        }

        val points = n / 2
        val need = c.pointCount + points
        if (need > c.capacityPoints) {
            // 按 1.25 倍扩容；单块最大也就 4.8MB 左右，重传代价很小
            val newCap = maxOf(need, c.capacityPoints * 5 / 4, 4096)
            reallocChunk(c, newCap)
        }

        val scratch = ensureScratch(n)
        scratch.clear()
        scratch.put(world, 0, n)
        scratch.position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, c.vboId)
        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, c.pointCount * 8, n * 4, scratch)
        // 这里刻意不查 glGetError：它可能触发 GPU 管线同步，放在每帧上传路径上会明显掉帧。
        // 错误检查放在扩容（低频）和帧末（每 5 秒一次）做。
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        c.pointCount = need
        var minX = c.minX
        var minY = c.minY
        var maxX = c.maxX
        var maxY = c.maxY
        var i = 0
        while (i < n) {
            val x = world[i]
            val y = world[i + 1]
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
            i += 2
        }
        c.minX = minX
        c.minY = minY
        c.maxX = maxX
        c.maxY = maxY
        c.kfCount++

        // 块满（关键帧数或点数）就封口，后续点落到新块
        if (c.kfCount >= CHUNK_KEYFRAMES || c.pointCount >= CHUNK_MAX_POINTS) {
            enqueueLod(c)
            cloudChunks.add(c)
            buildingChunk = null
        }
    }

    /** 重新分配块 VBO 容量，并把该块已有数据整块重传（只在该块扩容时发生） */
    private fun reallocChunk(c: CloudChunk, newCapPoints: Int) {
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, c.vboId)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            newCapPoints * 8,
            null,
            GLES20.GL_DYNAMIC_DRAW
        )
        checkGlError("chunkAlloc:$newCapPoints")
        c.capacityPoints = newCapPoints

        if (c.pointCount > 0) {
            val scratch = ensureScratch(c.pointCount * 2)
            scratch.clear()
            var written = 0
            val end = minOf(c.kfStartIndex + c.kfCount, keyFrameOrder.size)
            for (k in c.kfStartIndex until end) {
                val kf = keyFrameOrder[k]
                val w = kf.worldPoints ?: continue
                val nn = minOf(kf.pointCount * 2, w.size)
                if (nn <= 0) continue
                if (written + nn > c.pointCount * 2) break
                scratch.put(w, 0, nn)
                written += nn
            }
            scratch.position(0)
            if (written > 0) {
                GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, written * 4, scratch)
                checkGlError("chunkReupload")
            }
        }
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    /** 上传用 direct buffer，按需扩容后复用 */
    private fun ensureScratch(floats: Int): FloatBuffer {
        val cur = uploadScratch
        if (cur == null || cur.capacity() < floats) {
            uploadScratch = ByteBuffer.allocateDirect(floats * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
        }
        return uploadScratch!!
    }

    /**
     * 把一个已封口块的点拼成一段连续数组，排进 LOD 生成队列。
     * 单块约 12 万点（≈1ms 拷贝），每 400 帧才一次，不做这个反而更亏。
     */
    private fun enqueueLod(c: CloudChunk) {
        if (!LOD_ENABLED) return
        val totalFloats = c.pointCount * 2
        if (totalFloats <= 0) return
        val pts = FloatArray(totalFloats)
        var w = 0
        val end = minOf(c.kfStartIndex + c.kfCount, keyFrameOrder.size)
        for (k in c.kfStartIndex until end) {
            val wp = keyFrameOrder[k].worldPoints ?: continue
            val nn = minOf(wp.size, totalFloats - w)
            if (nn <= 0) break
            System.arraycopy(wp, 0, pts, w, nn)
            w += nn
        }
        if (w <= 0) return
        // 输出上界就是原始点数（体素抽稀只会更少），避免中途扩容
        lodJobs.addLast(LodJob(c, pts, 0, HashSet(maxOf(16, w / 8)), FloatArray(w), 0))
    }

    /**
     * 分帧生成 LOD：每帧最多处理 [LOD_POINTS_PER_FRAME] 个点（约 2ms），
     * 一块 12 万点分摊到 6 帧完成。绝不能一次性跑完 —— 那会在每个块封口时造成明显顿挫。
     */
    private fun processLodQueue() {
        val job = lodJobs.firstOrNull() ?: return
        val pts = job.points
        var budget = LOD_POINTS_PER_FRAME
        while (job.cursor + 1 < pts.size && budget > 0) {
            val x = pts[job.cursor]
            val y = pts[job.cursor + 1]
            val ix = Math.floor((x / LOD_VOXEL_METERS).toDouble()).toLong()
            val iy = Math.floor((y / LOD_VOXEL_METERS).toDouble()).toLong()
            val key = (ix shl 32) or (iy and 0xffffffffL)
            if (job.seen.add(key)) {
                job.out[job.outCount++] = x
                job.out[job.outCount++] = y
            }
            job.cursor += 2
            budget--
        }
        if (job.cursor + 1 >= pts.size) {
            lodJobs.removeFirst()
            uploadChunkLod(job)
        }
    }

    /** LOD 生成完成后上传到该块自己的 VBO */
    private fun uploadChunkLod(job: LodJob) {
        if (!LOD_ENABLED || job.outCount <= 0) return
        val c = job.chunk
        if (c.lodVboId == 0) {
            val ids = intArrayOf(0)
            GLES20.glGenBuffers(1, ids, 0)
            c.lodVboId = ids[0]
        }
        val scratch = ensureScratch(job.outCount)
        scratch.clear()
        scratch.put(job.out, 0, job.outCount)
        scratch.position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, c.lodVboId)
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            job.outCount * 4,
            scratch,
            GLES20.GL_DYNAMIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        c.lodPointCount = job.outCount / 2
    }

    /** 释放所有块的 VBO（回环重建 / reset / detach） */
    private fun releaseAllChunks() {
        for (c in cloudChunks) {
            if (c.vboId != 0) GLES20.glDeleteBuffers(1, intArrayOf(c.vboId), 0)
            if (c.lodVboId != 0) GLES20.glDeleteBuffers(1, intArrayOf(c.lodVboId), 0)
            c.vboId = 0
            c.lodVboId = 0
            c.lodPointCount = 0
        }
        buildingChunk?.let {
            if (it.vboId != 0) GLES20.glDeleteBuffers(1, intArrayOf(it.vboId), 0)
            if (it.lodVboId != 0) GLES20.glDeleteBuffers(1, intArrayOf(it.lodVboId), 0)
        }
        cloudChunks.clear()
        buildingChunk = null
        lodJobs.clear()
    }

    private var lastGlErrLogMs = 0L
    private var lastGlCheckMs = 0L

    /**
     * GL 错误检查：之前大缓冲分配失败是"静默"的，导致点云丢了也无从得知。
     * 注意 glGetError 可能同步管线，**只允许低频调用**（内部做了 2 秒节流）。
     */
    private fun checkGlError(tag: String) {
        val nowCheck = SystemClock.elapsedRealtime()
        if (nowCheck - lastGlCheckMs < 2000L) return
        lastGlCheckMs = nowCheck
        var err = GLES20.glGetError()
        if (err == GLES20.GL_NO_ERROR) return
        val now = SystemClock.elapsedRealtime()
        if (now - lastGlErrLogMs > 5000L) {
            lastGlErrLogMs = now
            Log.e("GLCORE", "$tag glError=0x${Integer.toHexString(err)} " +
                    "chunks=${cloudChunks.size} points=$pointCloudVertexCount")
        }
        while (err != GLES20.GL_NO_ERROR) {
            err = GLES20.glGetError()
        }
    }

    /** 分块是否与当前视野相交 */
    private fun isChunkVisible(c: CloudChunk): Boolean {
        if (!visValid) return true
        return c.maxX >= visMinX && c.minX <= visMaxX && c.maxY >= visMinY && c.minY <= visMaxY
    }

    /**
     * 只画与视野相交的分块，每块用自己的 VBO。
     * 20000+ 步通常几十块，视野内只有几块，绘制点数从数百万降到十几万。
     */
    private fun drawCloudChunks(color: FloatArray, size: Float) {
        var drawn = 0
        var visibleCount = 0
        val worldPerPx = if (frameScale > 0f) frameResolution / frameScale else 0.05f
        // 缩到 1m 约 8px 以下时用体素抽稀版本：全局视野下点数从千万级降到百万级
        val useLod = LOD_ENABLED && worldPerPx >= LOD_WORLD_PER_PX
        for (c in cloudChunks) {
            if (!isChunkVisible(c) || c.vboId == 0 || c.pointCount <= 0) continue
            visibleCount++
            if (useLod && c.lodVboId != 0 && c.lodPointCount > 0) {
                drawPointRange(c.lodVboId, 0, c.lodPointCount, color, size)
                drawn += c.lodPointCount
            } else {
                drawPointRange(c.vboId, 0, c.pointCount, color, size)
                drawn += c.pointCount
            }
        }
        val tail = buildingChunk
        if (tail != null && tail.vboId != 0 && tail.pointCount > 0 && isChunkVisible(tail)) {
            visibleCount++
            drawPointRange(tail.vboId, 0, tail.pointCount, color, size)
            drawn += tail.pointCount
        }
        lastDrawnCloudPoints = drawn
        lastVisibleChunks = visibleCount
    }



    /**
     * 重建实时上激光点云 VBO
     * 锁内从liveBuffer拷贝一份数据快照，锁外再执行GL上传（锁持有时间仅µs级，
     * 不会阻塞激光数据写入线程），彻底避免读到写了一半的缓冲
     */
    private fun rebuildLivePointVBO() {
        var len = 0
        var snapshot: FloatBuffer? = null
        synchronized(liveLock) {
            val buf = liveBuffer
            val count = liveVertexCount
            if (buf == null || count <= 0) {
                liveDrawCount = 0
            } else {
                len = count * 2
                if (buf.remaining() >= len) {
                    val tmp = obtainUploadBuffer(len)
                    tmp.put(buf)
                    tmp.position(0)
                    snapshot = tmp
                    liveDrawCount = count
                }
                // remaining不足（持锁后理论不可达）：防御性保留上一帧
            }
        }
        if (snapshot != null) {
            uploadVBOFromBuffer(vboLivePoint, snapshot!!, len, lastLenLive)
            lastLenLive = len
        }
    }

    /**
     * 关键帧点：直接遍历追加顺序数组，不再 keyFrames3D.entries.toList()。
     * 20000 步时每次重建都会分配一个 2 万元素的 List（还含 Entry 对象），
     * 10 次/秒就是持续的 GC 压力，是缩放/平移卡顿的放大器之一。
     */
    private fun rebuildKeyframePointsVBO() {
        var i = 0
        synchronized(keyFrames3D) {
            val size = keyFrameOrder.size
            if (size == 0) {
                keyframePointCount = 0
                return
            }
            if (keyframePointsArray == null || keyframePointsArray!!.size < size * 2) {
                keyframePointsArray = FloatArray(size * 2)
            }
            val arr = keyframePointsArray!!
            for (frame in keyFrameOrder) {
                arr[i++] = frame.robotPos[0]
                arr[i++] = frame.robotPos[1]
            }
        }
        keyframePointCount = i / 2
        uploadVBO(vboKeyframePoints, keyframePointsArray!!, i, keyframePointsLastLen)
        keyframePointsLastLen = i
    }

    private fun rebuildKeyframeLinesVBO() {
        var i = 0
        synchronized(keyFrames3D) {
            val size = keyFrameOrder.size
            if (size == 0) {
                keyframeLineVertexCount = 0
                return
            }
            val needed = size * 4
            if (keyframeLinesArray == null || keyframeLinesArray!!.size < needed) {
                keyframeLinesArray = FloatArray(needed)
            }
            val arr = keyframeLinesArray!!
            for (frame in keyFrameOrder) {
                if (i + 3 >= arr.size) break
                val x = frame.robotPos[0]
                val y = frame.robotPos[1]
                val t = frame.robotPos[2]
                arr[i++] = x
                arr[i++] = y
                arr[i++] = x + DIR_LINE_LENGTH * cos(t)
                arr[i++] = y + DIR_LINE_LENGTH * sin(t)
            }
        }
        keyframeLineVertexCount = i / 2
        uploadVBO(vboKeyframeLines, keyframeLinesArray!!, i, keyframeLinesLastLen)
        keyframeLinesLastLen = i
    }

    /**
     * 实时关键帧编号。
     *
     * 与旧关键帧共用同一套规则：
     * - 字号由 textScreenPx() 决定（随缩放变化并钳制），两张文字观感一致
     * - 只生成视野内的编号（视野外直接跳过，20000 步时能省掉绝大部分实例）
     * - 按屏幕间距避让，缩小时自动变稀，放大时逐步显示更密
     */
    private fun rebuildTextVBO() {
        val worldPerPx = if (frameScale > 0f) frameResolution / frameScale else 0.05f
        val h = textWorldHeight(worldPerPx)
        liveTextHeight = h
        val gapWorld = TEXT_GAP_PX * worldPerPx
        val minGapWorld = TEXT_MIN_GAP_PX * worldPerPx

        if (visValid) {
            textVisMinX = visMinX
            textVisMinY = visMinY
            textVisMaxX = visMaxX
            textVisMaxY = visMaxY
        }
        val ids = synchronized(keyFrames3D) { keyFrameIds.toIntArray() }
        val instances = ArrayList<Float>(1024)
        var lastX = Float.NaN
        var lastY = 0f
        for (id in ids) {
            val frame = keyFrames3D[id] ?: continue
            val x = frame.robotPos[0]
            val y = frame.robotPos[1]
            // 避让必须对"整条轨迹"计算，而不是只对视野内的点：
            // 否则平移时第一个可见点变了，避让相位就跟着变，编号会整片跳变/闪烁。
            val gapOk = lastX.isNaN() ||
                    (x - lastX) * (x - lastX) + (y - lastY) * (y - lastY) >= minGapWorld * minGapWorld
            if (!gapOk) continue
            lastX = x
            lastY = y
            // 只有落在视野内的才生成实例（省掉绝大部分绘制，但不影响避让相位）
            if (!isInVisibleRect(x, y)) continue
            // 编号放在关键帧点上方
            appendTextCentered(instances, id.toString(), x, y + h * 0.5f + gapWorld, h)
        }

        textInstanceCount = instances.size / 4
        if (textInstanceCount == 0) return
        // 复用 direct buffer：原来每帧 allocateDirect 一块，Cleaner 跟不上，
        // native 内存会一路涨（实测 2 万步时 68MB 且不回落），同时造成 GC/掉帧。
        val need = instances.size
        var buf: FloatBuffer? = textUploadScratch
        if (buf == null || buf.capacity() < need) {
            buf = ByteBuffer.allocateDirect(need * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            textUploadScratch = buf
        }
        val fb = buf!!
        fb.clear()
        for (v in instances) fb.put(v)
        fb.position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboTextInstance[0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            need * 4,
            fb,
            GLES20.GL_DYNAMIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    private fun drawText() {
        GLES20.glUseProgram(programText)
        GLES20.glUniformMatrix4fv(uMVPTex, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(uCharAspectScaleTex, texAspectScale)
        // 建图关键帧文字：与旧关键帧同一套缩放规则（随缩放变化并钳制）
        GLES20.glUniform1f(
            uCharHeightText,
            if (liveTextHeight > 0f) liveTextHeight else CHAR_WORLD_HEIGHT
        )
        GLES20.glUniform4fv(uTextColorText, 1, COLOR_WHITE, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textTexture)
        GLES20.glUniform1i(uTextureTex, 0)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboTextGeometry[0])
        GLES20.glEnableVertexAttribArray(aVertexText)
        GLES20.glVertexAttribPointer(aVertexText, 2, GLES20.GL_FLOAT, false, 4 * 4, 0)
        GLES20.glEnableVertexAttribArray(aTexOffsetText)
        GLES20.glVertexAttribPointer(aTexOffsetText, 2, GLES20.GL_FLOAT, false, 4 * 4, 2 * 4)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboTextInstance[0])
        GLES20.glEnableVertexAttribArray(aWorldPosText)
        GLES20.glVertexAttribPointer(aWorldPosText, 2, GLES20.GL_FLOAT, false, 4 * 4, 0)
        GLES30.glVertexAttribDivisor(aWorldPosText, 1)

        GLES20.glEnableVertexAttribArray(aUminText)
        GLES20.glVertexAttribPointer(aUminText, 1, GLES20.GL_FLOAT, false, 4 * 4, 2 * 4)
        GLES30.glVertexAttribDivisor(aUminText, 1)

        GLES20.glEnableVertexAttribArray(aUmaxText)
        GLES20.glVertexAttribPointer(aUmaxText, 1, GLES20.GL_FLOAT, false, 4 * 4, 3 * 4)
        GLES30.glVertexAttribDivisor(aUmaxText, 1)

        GLES30.glDrawArraysInstanced(GLES20.GL_TRIANGLE_STRIP, 0, 4, textInstanceCount)

        GLES30.glVertexAttribDivisor(aWorldPosText, 0)
        GLES30.glVertexAttribDivisor(aUminText, 0)
        GLES30.glVertexAttribDivisor(aUmaxText, 0)

        GLES20.glDisableVertexAttribArray(aVertexText)
        GLES20.glDisableVertexAttribArray(aTexOffsetText)
        GLES20.glDisableVertexAttribArray(aWorldPosText)
        GLES20.glDisableVertexAttribArray(aUminText)
        GLES20.glDisableVertexAttribArray(aUmaxText)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    // 注意：drawPoints / drawLines 必须自己绑定 programPoint，不能依赖 onDrawFrame
    // 帧首那次 glUseProgram。原因：drawText() 会把当前程序切成 programText 并且不恢复，
    // 若这里不重绑，drawText 之后的点/线绘制会误用文字着色器——
    // 表现为「打开关键帧后实时红色点云消失」（u_Color/u_PointSize 的 location 不属于
    // 当前程序，glUniform 直接 GL_INVALID_OPERATION；a_WorldPos/a_Umin/a_Umax 又处于
    // disable 状态取常量 0，顶点被文字着色器算到同一点上）。
    // 另外 uniform 值存储在 program 对象内，帧首设过的 u_MVPMatrix 会一直保留，
    // 因此这里只需重绑程序，无需重新上传矩阵。
    private fun drawPoints(
        vbo: Int, count: Int, color: FloatArray, size: Float, round: Boolean = false
    ) = drawPointRange(vbo, 0, count, color, size, round)

    /** 从 VBO 的 first 顶点开始画 count 个点（点云分块剔除用） */
    private fun drawPointRange(
        vbo: Int, first: Int, count: Int, color: FloatArray, size: Float,
        round: Boolean = false
    ) {
        if (count <= 0) return
        GLES20.glUseProgram(programPoint)
        GLES20.glUniform4fv(uColorPoint, 1, color, 0)
        GLES20.glUniform1f(uPointSize, size)
        GLES20.glUniform1f(uRoundPoint, if (round) 1f else 0f)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glEnableVertexAttribArray(aPosPoint)
        GLES20.glVertexAttribPointer(aPosPoint, 2, GLES20.GL_FLOAT, false, 0, 0)
        GLES20.glDrawArrays(GLES20.GL_POINTS, first, count)
        GLES20.glDisableVertexAttribArray(aPosPoint)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    /** 实心三角形（朝向线等需要精确宽度的图形，避开 glLineWidth 的平台差异） */
    private fun drawTriangles(vbo: Int, count: Int, color: FloatArray) {
        GLES20.glUseProgram(programPoint)
        GLES20.glUniformMatrix4fv(uMVPPoint, 1, false, mvpMatrix, 0)
        GLES20.glUniform4fv(uColorPoint, 1, color, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glEnableVertexAttribArray(aPosPoint)
        GLES20.glVertexAttribPointer(aPosPoint, 2, GLES20.GL_FLOAT, false, 0, 0)
        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, count)
        GLES20.glDisableVertexAttribArray(aPosPoint)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    private fun drawLines(vbo: Int, count: Int, color: FloatArray, mvp: FloatArray? = null) {
        GLES20.glUseProgram(programPoint)   // 同 drawPoints，不依赖外部程序状态
        if (mvp != null) {
            // 网格线跑屏幕正交投影，这里需要覆盖 MVP（点云那条路径仍沿用帧首设置的矩阵）
            GLES20.glUniformMatrix4fv(uMVPPoint, 1, false, mvp, 0)
        }
        GLES20.glUniform4fv(uColorPoint, 1, color, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glEnableVertexAttribArray(aPosPoint)
        GLES20.glVertexAttribPointer(aPosPoint, 2, GLES20.GL_FLOAT, false, 0, 0)
        GLES20.glDrawArrays(GLES20.GL_LINES, 0, count)
        GLES20.glDisableVertexAttribArray(aPosPoint)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    //    private fun updateMVPMatrix(mapView: CreateMapView3D) {
//        val md = mapView.mSrf.mapData
//        val res = if (md.resolution > 0) md.resolution else 0.05f
//        worldToPixelMatrix.reset()
//        worldToPixelMatrix.postTranslate(-md.originX, -md.originY)
//        worldToPixelMatrix.postScale(1f / res, -1f / res)
//        worldToPixelMatrix.postTranslate(0f, md.height.toFloat())
//        totalMatrix.set(mapView.outerMatrix)
//        totalMatrix.preConcat(worldToPixelMatrix)
//        matrixToGL(totalMatrix, modelMatrix)
//        GMatrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0)
//    }
    // 修改 updateMVPMatrix，在计算完 matrix 后缓存 scale 和 resolution
    private fun updateMVPMatrix(mapView: CreateMapView3D) {
        val md = mapView.mSrf.mapData
        val res = if (md.resolution > 0) md.resolution else 0.05f
        cachedResolution = res
        cachedMapScale = mapView.mSrf.scale
        // 一帧只取一次矩阵快照，后续所有世界坐标图层都用它
        mapView.snapshotOuterMatrix(frameMatrix)
        frameResolution = res
        frameScale = mapView.mSrf.scale

        worldToPixelMatrix.reset()
        worldToPixelMatrix.postTranslate(-md.originX, -md.originY)
        worldToPixelMatrix.postScale(1f / res, -1f / res)
        worldToPixelMatrix.postTranslate(0f, md.height.toFloat())
        totalMatrix.set(frameMatrix)
        totalMatrix.preConcat(worldToPixelMatrix)
        matrixToGL(totalMatrix, modelMatrix)
        GMatrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0)
    }

    private fun matrixToGL(mat: Matrix, gl: FloatArray) {
        mat.getValues(matrixValues)
        gl[0] = matrixValues[Matrix.MSCALE_X]; gl[1] = matrixValues[Matrix.MSKEW_Y]; gl[2] =
            0f; gl[3] = 0f
        gl[4] = matrixValues[Matrix.MSKEW_X]; gl[5] = matrixValues[Matrix.MSCALE_Y]; gl[6] =
            0f; gl[7] = 0f
        gl[8] = 0f; gl[9] = 0f; gl[10] = 1f; gl[11] = 0f
        gl[12] = matrixValues[Matrix.MTRANS_X]; gl[13] = matrixValues[Matrix.MTRANS_Y]; gl[14] =
            0f; gl[15] = 1f
    }

    /**
     * 手势（拖动/缩放/旋转）进行中调用 true，抬手调用 false。
     * 交互期间降级渲染：跳过文字与网格、点径降到 1px，抬手后补一帧完整画面。
     */
    fun setInteracting(value: Boolean) {
        if (interacting == value) return
        interacting = value
        requestRenderImmediate()
    }

    /** 文字世界高度：把"想要的屏幕字高"换算成 u_CharWorldHeight（补偿字形占比） */
    private fun textWorldHeight(worldPerPx: Float): Float =
        textScreenPx() / GLYPH_HEIGHT_RATIO * worldPerPx

    /** 统一的文字屏幕字号：随缩放线性变化并钳制，实时/旧关键帧共用 */
    private fun textScreenPx(): Float {
        val scale = if (frameScale > 0f) frameScale else 1f
        return (TEXT_BASE_PX * scale / TEXT_REF_SCALE).coerceIn(TEXT_MIN_PX, TEXT_MAX_PX)
    }

    /** 把屏幕四角反变换到世界坐标，得到本帧可见世界 AABB */
    private fun updateVisibleWorldRect() {
        val w = surfaceW
        val h = surfaceH
        if (w <= 0 || h <= 0) {
            visValid = false
            return
        }
        viewCorners[0] = 0f; viewCorners[1] = 0f
        viewCorners[2] = w.toFloat(); viewCorners[3] = 0f
        viewCorners[4] = 0f; viewCorners[5] = h.toFloat()
        viewCorners[6] = w.toFloat(); viewCorners[7] = h.toFloat()
        // 注意：mOuterMatrix 是"地图像素->屏幕"，世界坐标必须先乘 worldToPixelMatrix。
        // totalMatrix 就是世界->屏幕像素（updateMVPMatrix 内已算好），用它反变换才正确。
        invFrameMatrix.set(totalMatrix)
        if (!invFrameMatrix.invert(invFrameMatrix)) {
            visValid = false
            return
        }
        invFrameMatrix.mapPoints(viewCorners)
        var minX = viewCorners[0]
        var maxX = minX
        var minY = viewCorners[1]
        var maxY = minY
        for (i in 1 until 4) {
            val x = viewCorners[i * 2]
            val y = viewCorners[i * 2 + 1]
            if (x < minX) minX = x
            if (x > maxX) maxX = x
            if (y < minY) minY = y
            if (y > maxY) maxY = y
        }
        val margin = TEXT_CLIP_MARGIN_PX * (if (frameScale > 0f) frameResolution / frameScale else 0.05f)
        visMinX = minX - margin
        visMaxX = maxX + margin
        visMinY = minY - margin
        visMaxY = maxY + margin
        visValid = minX.isFinite() && minY.isFinite() && maxX.isFinite() && maxY.isFinite()
    }

    /** 视野相对"上次生成编号时"是否发生了明显变化（阈值取视野宽度的 8%） */
    private fun viewRectChangedSinceText(): Boolean {
        if (!visValid) return false
        if (textVisMinX.isNaN()) return true
        val threshold = (visMaxX - visMinX) * 0.08f
        return kotlin.math.abs(visMinX - textVisMinX) > threshold ||
                kotlin.math.abs(visMaxX - textVisMaxX) > threshold ||
                kotlin.math.abs(visMinY - textVisMinY) > threshold ||
                kotlin.math.abs(visMaxY - textVisMaxY) > threshold
    }

    private fun isInVisibleRect(x: Float, y: Float): Boolean {
        if (!visValid) return true
        return x >= visMinX && x <= visMaxX && y >= visMinY && y <= visMaxY
    }

    // ──────────────── 方案C：world-space 图层（原 canvas 快照通道） ────────────────

    /**
     * 按数据版本/缩放变化，增量同步三组世界坐标几何到 VBO。
     * 数据只在"关键帧/节点/激光变化"或"缩放变化"时重建，平时零成本。
     */
    private fun syncWorldLayers(mapView: CreateMapView3D) {
        val worldPerPx = if (frameScale > 0f) frameResolution / frameScale else 0f
        if (worldPerPx <= 0f) return

        // 约束节点
        val nv = mapView.constrainNodesVersion()
        if (nv != nodeVersion) {
            nodeVersion = nv
            nodeSnapshot = mapView.snapshotConstraintNodes() ?: FloatArray(0)
            val n = nodeSnapshot.size / 3
            val pts = FloatArray(n * 2)
            for (i in 0 until n) {
                pts[i * 2] = nodeSnapshot[i * 3 + 1]
                pts[i * 2 + 1] = nodeSnapshot[i * 3 + 2]
            }
            nodePointCount = n
            if (n > 0) uploadVBO(vboNodePoints, pts, pts.size, -1)
            nodeTextDirty = true
        }

        // 旧关键帧点位
        val kv = mapView.oldKeyFramesVersion()
        if (kv != oldKfVersion) {
            oldKfVersion = kv
            oldKfSnapshot = mapView.snapshotOldKeyFrames() ?: FloatArray(0)
            val n = oldKfSnapshot.size / 4
            val pts = FloatArray(n * 2)
            for (i in 0 until n) {
                pts[i * 2] = oldKfSnapshot[i * 4]
                pts[i * 2 + 1] = oldKfSnapshot[i * 4 + 1]
            }
            oldKfPointCount = n
            if (n > 0) uploadVBO(vboOldKfPoints, pts, pts.size, -1)
            oldKfTextDirty = true
        }

        // 依赖缩放的几何：朝向线长度、文字大小
        if (oldKfTextDirty || nodeTextDirty || oldKfTickScale != frameScale) {
            oldKfTickScale = frameScale
            rebuildScaleDependentGeometry(worldPerPx)
            oldKfTextDirty = false
            nodeTextDirty = false
        }

        // 非建图激光
        val uv = mapView.upLaserVersion
        if (uv != upLaserVersion) {
            upLaserVersion = uv
            val pts = mapView.snapshotUpLaser()
            upLaserCount = pts.size / 2
            if (upLaserCount > 0) uploadVBO(vboUpLaser, pts, pts.size, -1)
        }
    }

    /** 朝向线长度、ID 文字大小都是"屏幕像素"语义，缩放变化时需要按新的 worldPerPx 重建 */
    private fun rebuildScaleDependentGeometry(worldPerPx: Float) {
        // 旧关键帧：朝向线 + ID 文字
        val n = oldKfSnapshot.size / 4
        if (n > 0) {
            val lenWorld = OLD_KF_LINE_LEN_PX * worldPerPx
            val halfW = OLD_KF_LINE_WIDTH_PX * worldPerPx * 0.5f
            // 统一字号：屏幕像素 -> 世界高度（与实时关键帧同一套规则）
            val h = textWorldHeight(worldPerPx)
            val textGapWorld = TEXT_GAP_PX * worldPerPx
            oldKfTextHeight = h
            // 编号避让阈值（世界单位）：屏幕间距不足时跳过该编号
            val minGapWorld = TEXT_MIN_GAP_PX * worldPerPx
            var lastTextX = Float.NaN
            var lastTextY = 0f
            // 每条线 6 个顶点（两个三角形），共 12 个 float，宽度不再依赖 glLineWidth
            val tick = FloatArray(n * 12)
            val text = ArrayList<Float>(n * 16)
            for (i in 0 until n) {
                val x = oldKfSnapshot[i * 4]
                val y = oldKfSnapshot[i * 4 + 1]
                val t = oldKfSnapshot[i * 4 + 2]
                val id = oldKfSnapshot[i * 4 + 3].toInt()
                val dx = cos(t)
                val dy = sin(t)
                // 法向偏移，把线段撑成 3px 宽的细长矩形
                val nx = -dy * halfW
                val ny = dx * halfW
                val ex = x + lenWorld * dx
                val ey = y + lenWorld * dy
                val b = i * 12
                tick[b] = x + nx
                tick[b + 1] = y + ny
                tick[b + 2] = x - nx
                tick[b + 3] = y - ny
                tick[b + 4] = ex + nx
                tick[b + 5] = ey + ny
                tick[b + 6] = ex + nx
                tick[b + 7] = ey + ny
                tick[b + 8] = x - nx
                tick[b + 9] = y - ny
                tick[b + 10] = ex - nx
                tick[b + 11] = ey - ny
                // 编号放在关键帧下方（文字中心 = 点下方 半个字高 + 间距）
                val gapOk = lastTextX.isNaN() ||
                        (x - lastTextX) * (x - lastTextX) + (y - lastTextY) * (y - lastTextY) >=
                        minGapWorld * minGapWorld
                if (gapOk) {
                    appendTextCentered(text, id.toString(), x, y - h * 0.5f - textGapWorld, h)
                    lastTextX = x
                    lastTextY = y
                }
            }
            oldKfTickCount = n * 6
            uploadVBO(vboOldKfTicks, tick, tick.size, -1)
            oldKfTextCount = text.size / 4
            if (oldKfTextCount > 0) {
                uploadVBO(vboOldKfText, text.toFloatArray(), text.size, -1)
            }
        } else {
            oldKfTickCount = 0
            oldKfTextCount = 0
        }

        // 约束节点 ID 文字（原逻辑画在点的右下 +15px）
        val m = nodeSnapshot.size / 3
        if (m > 0) {
            val h = textWorldHeight(worldPerPx)
            nodeTextHeight = h
            val off = 15f * worldPerPx
            val text = ArrayList<Float>(m * 16)
            for (i in 0 until m) {
                val id = nodeSnapshot[i * 3].toInt()
                val x = nodeSnapshot[i * 3 + 1] + off
                val y = nodeSnapshot[i * 3 + 2] - off
                appendTextCentered(text, id.toString(), x, y, h)
            }
            nodeTextCount = text.size / 4
            if (nodeTextCount > 0) {
                uploadVBO(vboNodeText, text.toFloatArray(), text.size, -1)
            }
        } else {
            nodeTextCount = 0
        }
    }

    /** 按屏幕坐标居中排布一串数字，输出 [worldX, worldY, uMin, uMax] 实例 */
    private fun appendTextInstances(out: MutableList<Float>, text: String, cx: Float, cy: Float, h: Float) {
        var total = 0f
        for (ch in text) {
            val d = digitIndex(ch)
            if (d < 0) continue
            total += h * (charUmax[d] - charUmin[d]) * texAspectScale
        }
        var x = cx - total / 2f
        for (ch in text) {
            val d = digitIndex(ch)
            if (d < 0) continue
            out.add(x)
            out.add(cy)
            out.add(charUmin[d])
            out.add(charUmax[d])
            x += h * (charUmax[d] - charUmin[d]) * texAspectScale
        }
    }

    private fun appendTextCentered(out: MutableList<Float>, text: String, cx: Float, cy: Float, h: Float) =
        appendTextInstances(out, text, cx, cy, h)

    private fun digitIndex(ch: Char): Int = if (ch == '-') 10 else ch - '0'

    private fun drawWorldLayers(includeText: Boolean = true) {
        // 约束节点：蓝点 + 蓝字
        if (nodePointCount > 0) {
            drawPoints(vboNodePoints[0], nodePointCount, COLOR_NODE, NODE_POINT_SIZE, true)
        }
        if (includeText && nodeTextCount > 0) {
            drawTextGroup(vboNodeText[0], nodeTextCount, nodeTextHeight, COLOR_NODE)
        }
        // 旧关键帧：紫点 + 紫朝向线 + 紫字
        if (oldKfPointCount > 0) {
            drawPoints(vboOldKfPoints[0], oldKfPointCount, COLOR_OLD_KF, OLD_KF_POINT_SIZE, true)
        }
        if (oldKfTickCount > 0) {
            drawTriangles(vboOldKfTicks[0], oldKfTickCount, COLOR_OLD_KF)
        }
        if (includeText && oldKfTextCount > 0) {
            drawTextGroup(vboOldKfText[0], oldKfTextCount, oldKfTextHeight, COLOR_OLD_KF)
        }
        // 非建图激光：红点
        if (upLaserCount > 0) {
            drawPoints(vboUpLaser[0], upLaserCount, COLOR_UP_LASER, UP_LASER_POINT_SIZE, false)
        }
    }

    /** 用"实例化文字"管线绘制一组数字（字号/颜色可变，位置固定在世界坐标） */
    private fun drawTextGroup(vbo: Int, count: Int, heightWorld: Float, color: FloatArray) {
        if (vbo == 0 || count <= 0 || heightWorld <= 0f) return
        GLES20.glUseProgram(programText)
        GLES20.glUniformMatrix4fv(uMVPTex, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(uCharAspectScaleTex, texAspectScale)
        GLES20.glUniform1f(uCharHeightText, heightWorld)
        GLES20.glUniform4fv(uTextColorText, 1, color, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textTexture)
        GLES20.glUniform1i(uTextureTex, 0)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboTextGeometry[0])
        GLES20.glEnableVertexAttribArray(aVertexText)
        GLES20.glVertexAttribPointer(aVertexText, 2, GLES20.GL_FLOAT, false, 4 * 4, 0)
        GLES20.glEnableVertexAttribArray(aTexOffsetText)
        GLES20.glVertexAttribPointer(aTexOffsetText, 2, GLES20.GL_FLOAT, false, 4 * 4, 2 * 4)

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glEnableVertexAttribArray(aWorldPosText)
        GLES20.glVertexAttribPointer(aWorldPosText, 2, GLES20.GL_FLOAT, false, 4 * 4, 0)
        GLES30.glVertexAttribDivisor(aWorldPosText, 1)

        GLES20.glEnableVertexAttribArray(aUminText)
        GLES20.glVertexAttribPointer(aUminText, 1, GLES20.GL_FLOAT, false, 4 * 4, 2 * 4)
        GLES30.glVertexAttribDivisor(aUminText, 1)

        GLES20.glEnableVertexAttribArray(aUmaxText)
        GLES20.glVertexAttribPointer(aUmaxText, 1, GLES20.GL_FLOAT, false, 4 * 4, 3 * 4)
        GLES30.glVertexAttribDivisor(aUmaxText, 1)

        GLES30.glDrawArraysInstanced(GLES20.GL_TRIANGLE_STRIP, 0, 4, count)

        GLES30.glVertexAttribDivisor(aWorldPosText, 0)
        GLES30.glVertexAttribDivisor(aUminText, 0)
        GLES30.glVertexAttribDivisor(aUmaxText, 0)

        GLES20.glDisableVertexAttribArray(aVertexText)
        GLES20.glDisableVertexAttribArray(aTexOffsetText)
        GLES20.glDisableVertexAttribArray(aWorldPosText)
        GLES20.glDisableVertexAttribArray(aUminText)
        GLES20.glDisableVertexAttribArray(aUmaxText)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    private fun uploadVBO(vbo: IntArray, data: FloatArray, length: Int, lastLen: Int) {
        val buffer = obtainUploadBuffer(length)
        buffer.put(data, 0, length)
        buffer.position(0)
        if (vbo[0] == 0) GLES20.glGenBuffers(1, vbo, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
        if (lastLen == length) GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, length * 4, buffer)
        else GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, length * 4, buffer, GLES20.GL_DYNAMIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    private fun uploadVBOFromBuffer(vbo: IntArray, src: FloatBuffer, length: Int, lastLen: Int) {
        // 防御：缓冲区数据不足时跳过本帧上传（glBufferSubData/glBufferData的JNI层
        // 在remaining()<size时会直接抛IllegalArgumentException导致GLThread崩溃）
        if (length <= 0 || src.remaining() < length) return
        if (vbo[0] == 0) GLES20.glGenBuffers(1, vbo, 0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo[0])
        if (lastLen == length) GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, length * 4, src)
        else GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, length * 4, src, GLES20.GL_DYNAMIC_DRAW)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    private fun obtainUploadBuffer(minCapacity: Int): FloatBuffer {
        if (uploadBuffer == null || uploadBufferCapacity < minCapacity) {
            uploadBuffer = ByteBuffer.allocateDirect(minCapacity * 4).order(ByteOrder.nativeOrder())
                .asFloatBuffer()
            uploadBufferCapacity = minCapacity
        } else uploadBuffer!!.clear()
        return uploadBuffer!!
    }

    private fun createProgram(vShader: String, fShader: String): Int {
        val vs = loadShader(GLES20.GL_VERTEX_SHADER, vShader)
        val fs = loadShader(GLES20.GL_FRAGMENT_SHADER, fShader)
        return GLES20.glCreateProgram().apply {
            GLES20.glAttachShader(this, vs)
            GLES20.glAttachShader(this, fs)
            GLES20.glLinkProgram(this)
        }
    }

    private fun loadShader(type: Int, code: String) =
        GLES20.glCreateShader(type)
            .also { s -> GLES20.glShaderSource(s, code); GLES20.glCompileShader(s) }

    private fun createDigitTexture(): Int {
        val chars = "0123456789-"
        val charCount = chars.length
        val textSize = 48f
        val bmpHeight = 64

        val paint = Paint().apply {
            // 字模必须是白色：片元输出 = 字模 × u_TextColor，
            // 紫色字模会让所有图层编号都偏紫，实时编号也无法做成高对比色
            color = Color.WHITE
            this.textSize = textSize
            isAntiAlias = true
            textAlign = Paint.Align.LEFT
        }
        val charWidths = FloatArray(charCount)
        var totalWidth = 0f
        for (i in 0 until charCount) {
            val w = paint.measureText(chars[i].toString())
            charWidths[i] = w
            totalWidth += w
        }
        val bmpWidth = totalWidth.toInt()
        val bmp = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        val baseline = (bmpHeight + textSize) / 2f - 4f
        var offsetX = 0f
        for (i in 0 until charCount) {
            canvas.drawText(chars[i].toString(), offsetX, baseline, paint)
            offsetX += charWidths[i]
        }
        for (i in 0 until charCount) {
            charUmin[i] = if (i == 0) 0f else charUmax[i - 1]
            charUmax[i] = charUmin[i] + charWidths[i] / totalWidth
        }
        texAspectScale = totalWidth / bmpHeight
        val tex = IntArray(1)
        GLES20.glGenTextures(1, tex, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_S,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLES20.glTexParameteri(
            GLES20.GL_TEXTURE_2D,
            GLES20.GL_TEXTURE_WRAP_T,
            GLES20.GL_CLAMP_TO_EDGE
        )
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        bmp.recycle()
        // 48px 字模缩到 16px 显示，没有 mipmap 会严重走样（数字糊成一团）。
        // GLES3 上下文支持 NPOT 纹理 mipmap；万一生成失败则退回线性采样。
        GLES20.glGenerateMipmap(GLES20.GL_TEXTURE_2D)
        if (GLES20.glGetError() == GLES20.GL_NO_ERROR) {
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR_MIPMAP_LINEAR
            )
        }
        return tex[0]
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 取消尚未执行的补偿帧，避免 surface 销毁后再触发渲染
        renderHandler.removeCallbacksAndMessages(null)
        renderTrailingScheduled.set(false)
        synchronized(keyFrames3D) {
            keyFrames3D.clear()
            keyFrameOrder.clear()
            keyFrameIds.clear()
            cloudPointTrimIndex = 0
            // 分块与上传状态一并释放，避免视图复用/重新 attach 后残留
            releaseAllChunks()
            pcUploadedKeyframes = 0
            uploadScratch = null
            textUploadScratch = null
        }
        pointCloudVertexCount = 0
        pointCloudFullDirty = true
        parent.clear()
        // 清理实时点云数据与复用缓存
        synchronized(liveLock) {
            liveVertexCount = 0
            liveBuffer = null
            liveBufferCapacity = 0
        }
        liveDirty = false
        lastLenLive = 0
        liveDrawCount = 0
        liveKeyframeCloudBuf = null
        liveKeyframeWorldBuf = null
        GLES20.glDeleteProgram(programPoint)
        GLES20.glDeleteProgram(programText)
        GLES20.glDeleteProgram(programRobot)
        GLES20.glDeleteProgram(programTex)
        GLES20.glDeleteTextures(1, intArrayOf(textTexture, robotTexture), 0)
        if (vboLivePoint[0] != 0) GLES20.glDeleteBuffers(1, vboLivePoint, 0)
        if (vboKeyframePoints[0] != 0) GLES20.glDeleteBuffers(1, vboKeyframePoints, 0)
        if (vboKeyframeLines[0] != 0) GLES20.glDeleteBuffers(1, vboKeyframeLines, 0)
        if (vboTextGeometry[0] != 0) GLES20.glDeleteBuffers(1, vboTextGeometry, 0)
        if (vboTextInstance[0] != 0) GLES20.glDeleteBuffers(1, vboTextInstance, 0)
        if (vboRobot[0] != 0) GLES20.glDeleteBuffers(1, vboRobot, 0)

        // 方案A：底图/网格/图层资源
        if (pngTexture != 0) GLES20.glDeleteTextures(1, intArrayOf(pngTexture), 0)
        pngTexture = 0
        pngTextureVersion = -1
        pngBitmapRef = null
        if (overlayTexture != 0) GLES20.glDeleteTextures(1, intArrayOf(overlayTexture), 0)
        overlayTexture = 0
        overlayTextureUploaded = false
        if (vboQuad[0] != 0) GLES20.glDeleteBuffers(1, vboQuad, 0)
        if (vboGrid[0] != 0) GLES20.glDeleteBuffers(1, vboGrid, 0)
        if (vboOldKfPoints[0] != 0) GLES20.glDeleteBuffers(1, vboOldKfPoints, 0)
        if (vboOldKfTicks[0] != 0) GLES20.glDeleteBuffers(1, vboOldKfTicks, 0)
        if (vboOldKfText[0] != 0) GLES20.glDeleteBuffers(1, vboOldKfText, 0)
        if (vboNodePoints[0] != 0) GLES20.glDeleteBuffers(1, vboNodePoints, 0)
        if (vboNodeText[0] != 0) GLES20.glDeleteBuffers(1, vboNodeText, 0)
        if (vboUpLaser[0] != 0) GLES20.glDeleteBuffers(1, vboUpLaser, 0)
        overlayBitmap?.recycle()
        overlayBitmap = null
        overlayCanvas = null
        gridArray = null
    }
}