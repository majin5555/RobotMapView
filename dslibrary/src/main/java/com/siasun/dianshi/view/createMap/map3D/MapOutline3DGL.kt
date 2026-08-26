package com.siasun.dianshi.view.createMap.map3D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Matrix
import android.graphics.PixelFormat
import android.opengl.GLES20
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import android.opengl.Matrix as GMatrix
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
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
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
        private const val POINT_SIZE_CLOUD = 3f
        private const val POINT_SIZE_KEYFRAME = 8f
        private const val POINT_SIZE_LIVE = 3f
        private const val DIR_LINE_LENGTH = 0.5f

        // 文字世界尺寸
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
            attribute vec4 a_Position;
            void main() {
                gl_Position = u_MVPMatrix * a_Position;
                gl_PointSize = u_PointSize;
            }
        """
        private const val FRAGMENT_SHADER_POINT = """
            precision mediump float;
            uniform vec4 u_Color;
            void main() {
                gl_FragColor = u_Color;
            }
        """

        // 文字着色器
        private const val VERTEX_SHADER_TEXT = """
            uniform mat4 u_MVPMatrix;
            uniform float u_CharAspectScale;
            attribute vec2 a_Vertex;
            attribute vec2 a_TexOffset;
            attribute vec2 a_WorldPos;
            attribute float a_Umin;
            attribute float a_Umax;
            varying vec2 v_TexCoord;
            void main() {
                float charWorldWidth = $CHAR_WORLD_HEIGHT * (a_Umax - a_Umin) * u_CharAspectScale;
                vec2 worldPos = a_WorldPos + a_Vertex * vec2(charWorldWidth, $CHAR_WORLD_HEIGHT);
                gl_Position = u_MVPMatrix * vec4(worldPos, 0.0, 1.0);
                v_TexCoord = vec2(a_Umin + a_TexOffset.x * (a_Umax - a_Umin), 1.0 - a_TexOffset.y);
            }
        """
        private const val FRAGMENT_SHADER_TEXT = """
            precision mediump float;
            uniform sampler2D u_Texture;
            varying vec2 v_TexCoord;
            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
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
    }

    // 状态标志
    private var isDetailedEnabled = false
    private var pointCloudDirty = true
    private var keyframeGeometryDirty = true
    private var drawWhiteBackground = false           // 默认透明

    private val keyFrames3D = ConcurrentHashMap<Int, KeyFrame>()

    // 点/线着色器
    private var programPoint = 0
    private var aPosPoint = 0
    private var uColorPoint = 0
    private var uMVPPoint = 0
    private var uPointSize = 0

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
    private var vboPointCloud = intArrayOf(0)
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

    private var pointCloudLastLen = 0
    private var keyframePointsLastLen = 0
    private var keyframeLinesLastLen = 0
    private var lastLenLive = 0   // 实时点云上次长度

    private var pointCloudBuffer: FloatBuffer? = null
    private var pointCloudBufferCapacity = 0
    private var liveBuffer: FloatBuffer? = null
    private var liveBufferCapacity = 0

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

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
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
            liveVertexCount = 0
            liveDirty = true
            requestRender()
            return // 最少包含机器人位置数据
        }
        val mapView = parent.get() ?: return

        val isKeyframe = laserData.rad0.toInt() != -1
        val totalPoints = (laserData.ranges.size - 6) / 3
        // 关键帧采样间隔（缩放越小，间隔越大，避免存储黑点过多）
        val baseSampleInterval = 5
        val dynamicSampleInterval = maxOf(baseSampleInterval, (1f / mapView.mSrf.scale).toInt())

        // 实时红点：完全显示，不过滤（下一帧整体替换，无需压缩）
        val required = totalPoints * 2
        if (liveBuffer == null || liveBufferCapacity < required) {
            liveBuffer = ByteBuffer.allocateDirect(required * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer()
            liveBufferCapacity = required
        } else {
            liveBuffer!!.clear()
        }

        // 关键帧缓存数组（按采样间隔容量申请，复用、仅扩容），黑点存储需控量
        var localCloudBuf: FloatArray? = null
        var localWorldBuf: FloatArray? = null
        if (isKeyframe) {
            val keyframeRequired = (totalPoints / dynamicSampleInterval + 1) * 2
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
                liveBuffer!!.put(worldX)
                liveBuffer!!.put(worldY)
                pointCount++
            }

            // 关键帧黑点：按采样间隔收集，控制存储量
            if (isKeyframe && localCloudBuf != null && localWorldBuf != null && i % dynamicSampleInterval == 0) {
                if (keyIdx + 1 < localCloudBuf.size) {
                    localCloudBuf[keyIdx] = laserX
                    localCloudBuf[keyIdx + 1] = laserY
                    localWorldBuf[keyIdx] = worldX
                    localWorldBuf[keyIdx + 1] = worldY
                    keyIdx += 2
                }
            }
        }
        liveBuffer!!.flip()
        liveVertexCount = pointCount
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
                pointCloudDirty = true
                keyframeGeometryDirty = true
                requestRender()
            }
        }
    }

    fun notifyMatrixChanged() {
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
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        // 点/线
        programPoint = createProgram(VERTEX_SHADER_POINT, FRAGMENT_SHADER_POINT)
        aPosPoint = GLES20.glGetAttribLocation(programPoint, "a_Position")
        uColorPoint = GLES20.glGetUniformLocation(programPoint, "u_Color")
        uMVPPoint = GLES20.glGetUniformLocation(programPoint, "u_MVPMatrix")
        uPointSize = GLES20.glGetUniformLocation(programPoint, "u_PointSize")

        // 文字
        programText = createProgram(VERTEX_SHADER_TEXT, FRAGMENT_SHADER_TEXT)
        aVertexText = GLES20.glGetAttribLocation(programText, "a_Vertex")
        aTexOffsetText = GLES20.glGetAttribLocation(programText, "a_TexOffset")
        aWorldPosText = GLES20.glGetAttribLocation(programText, "a_WorldPos")
        aUminText = GLES20.glGetAttribLocation(programText, "a_Umin")
        aUmaxText = GLES20.glGetAttribLocation(programText, "a_Umax")
        uMVPTex = GLES20.glGetUniformLocation(programText, "u_MVPMatrix")
        uCharAspectScaleTex = GLES20.glGetUniformLocation(programText, "u_CharAspectScale")
        uTextureTex = GLES20.glGetUniformLocation(programText, "u_Texture")

        // 机器人
        programRobot = createProgram(VERTEX_SHADER_ROBOT, FRAGMENT_SHADER_ROBOT)
        aPosRobot = GLES20.glGetAttribLocation(programRobot, "a_Position")
        aTexRobot = GLES20.glGetAttribLocation(programRobot, "a_TexCoord")
        uMVPRobot = GLES20.glGetUniformLocation(programRobot, "u_MVPMatrix")
        uModelMatrixRobot = GLES20.glGetUniformLocation(programRobot, "u_ModelMatrix")
        uTextureRobot = GLES20.glGetUniformLocation(programRobot, "u_Texture")

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

        // 纹理
        textTexture = createDigitTexture()
        // 机器人纹理在 setRobotBitmap 后上传，这里先创建占位
        robotTexture = 0
        uploadRobotTexture()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        GMatrix.orthoM(projectionMatrix, 0, 0f, width.toFloat(), height.toFloat(), 0f, -1f, 1f)
    }

    override fun onDrawFrame(gl: GL10?) {
        val mapView = parent.get() ?: return
        updateMVPMatrix(mapView)

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(programPoint)
        GLES20.glUniformMatrix4fv(uMVPPoint, 1, false, mvpMatrix, 0)

        if (pointCloudDirty) {
            rebuildPointCloudVBO()
            pointCloudDirty = false
        }
        if (pointCloudVertexCount > 0) {
            drawPoints(vboPointCloud[0], pointCloudVertexCount, COLOR_POINT_CLOUD, POINT_SIZE_CLOUD)
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
            if (keyframeGeometryDirty) {
                rebuildKeyframeLinesVBO()
                rebuildTextVBO()
            }
            if (keyframeLineVertexCount > 0) {
                drawLines(vboKeyframeLines[0], keyframeLineVertexCount, COLOR_LINE)
            }
            if (textInstanceCount > 0) {
                drawText()
            }
        }

        // 绘制实时上激光点云（红，始终在黑色关键帧点云之上）
        if (liveDirty) {
            rebuildLivePointVBO()
            liveDirty = false
        }
        if (liveVertexCount > 0) {
            drawPoints(vboLivePoint[0], liveVertexCount, COLOR_LIVE_POINT, POINT_SIZE_LIVE)
        }

        // 最后绘制机器人（在所有元素之上）
        drawRobot()

        if (keyframeGeometryDirty) keyframeGeometryDirty = false
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
    private fun rebuildPointCloudVBO() {
        var totalPoints = 0
        var floatCount = 0
        var buffer: FloatBuffer
        synchronized(keyFrames3D) {
            for (kf in keyFrames3D.values) totalPoints += kf.pointCount
            if (totalPoints == 0) {
                pointCloudVertexCount = 0; return
            }
            val neededFloats = totalPoints * 2
            if (pointCloudBuffer == null || pointCloudBufferCapacity < neededFloats) {
                pointCloudBuffer = ByteBuffer.allocateDirect(neededFloats * 4)
                    .order(ByteOrder.nativeOrder()).asFloatBuffer()
                pointCloudBufferCapacity = neededFloats
            } else {
                pointCloudBuffer!!.clear()
            }
            buffer = pointCloudBuffer!!
            for (kf in keyFrames3D.values) {
                val world = kf.worldPoints ?: continue
                buffer.put(world, 0, kf.pointCount * 2)
            }
            floatCount = buffer.position()
            buffer.flip()
        }
        pointCloudVertexCount = totalPoints
        uploadVBOFromBuffer(vboPointCloud, buffer, floatCount, pointCloudLastLen)
        pointCloudLastLen = floatCount
    }

    /**
     * 重建实时上激光点云 VBO
     */
    private fun rebuildLivePointVBO() {
        val buf = liveBuffer ?: run { liveVertexCount = 0; return }
        val len = liveVertexCount * 2
        if (len <= 0) return
        uploadVBOFromBuffer(vboLivePoint, buf, len, lastLenLive)
        lastLenLive = len
    }

    private fun rebuildKeyframePointsVBO() {
        val entries = keyFrames3D.entries.toList()
        val size = entries.size
        if (size == 0) {
            keyframePointCount = 0; return
        }
        if (keyframePointsArray == null || keyframePointsArray!!.size < size * 2) {
            keyframePointsArray = FloatArray(size * 2)
        }
        val arr = keyframePointsArray!!
        var i = 0
        for ((_, frame) in entries) {
            arr[i++] = frame.robotPos[0]
            arr[i++] = frame.robotPos[1]
        }
        keyframePointCount = i / 2
        uploadVBO(vboKeyframePoints, arr, i, keyframePointsLastLen)
        keyframePointsLastLen = i
    }

    private fun rebuildKeyframeLinesVBO() {
        val entries = keyFrames3D.entries.toList()
        val size = entries.size
        if (size == 0) {
            keyframeLineVertexCount = 0; return
        }
        val needed = size * 4
        if (keyframeLinesArray == null || keyframeLinesArray!!.size < needed) {
            keyframeLinesArray = FloatArray(needed)
        }
        val arr = keyframeLinesArray!!
        var i = 0
        for ((_, frame) in entries) {
            if (i + 3 >= arr.size) break
            val x = frame.robotPos[0];
            val y = frame.robotPos[1];
            val t = frame.robotPos[2]
            arr[i++] = x
            arr[i++] = y
            arr[i++] = x + DIR_LINE_LENGTH * cos(t)
            arr[i++] = y + DIR_LINE_LENGTH * sin(t)
        }
        keyframeLineVertexCount = i / 2
        uploadVBO(vboKeyframeLines, arr, i, keyframeLinesLastLen)
        keyframeLinesLastLen = i
    }

    private fun rebuildTextVBO() {
        val entries = keyFrames3D.entries.toList()
        var totalChars = 0
        for ((id, _) in entries) totalChars += id.toString().length
        if (totalChars == 0) {
            textInstanceCount = 0
            return
        }
        val instanceFloats = totalChars * 4
        val instanceArray = FloatArray(instanceFloats)
        var idx = 0
        for ((id, frame) in entries) {
            val text = id.toString()
            val robotX = frame.robotPos[0]
            val robotY = frame.robotPos[1] + TEXT_OFFSET_WORLD_Y
            var cursorX = robotX
            for (ch in text) {
                val digit = if (ch == '-') 10 else ch - '0'
                val uMin = charUmin[digit]
                val uMax = charUmax[digit]
                instanceArray[idx++] = cursorX
                instanceArray[idx++] = robotY
                instanceArray[idx++] = uMin
                instanceArray[idx++] = uMax
                val charWorldWidth = CHAR_WORLD_HEIGHT * (uMax - uMin) * texAspectScale
                cursorX += charWorldWidth
            }
        }
        val buffer = ByteBuffer.allocateDirect(instanceArray.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer()
        buffer.put(instanceArray).position(0)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboTextInstance[0])
        GLES20.glBufferData(
            GLES20.GL_ARRAY_BUFFER,
            instanceArray.size * 4,
            buffer,
            GLES20.GL_DYNAMIC_DRAW
        )
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
        textInstanceCount = totalChars
    }

    private fun drawText() {
        GLES20.glUseProgram(programText)
        GLES20.glUniformMatrix4fv(uMVPTex, 1, false, mvpMatrix, 0)
        GLES20.glUniform1f(uCharAspectScaleTex, texAspectScale)
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

    private fun drawPoints(vbo: Int, count: Int, color: FloatArray, size: Float) {
        GLES20.glUniform4fv(uColorPoint, 1, color, 0)
        GLES20.glUniform1f(uPointSize, size)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
        GLES20.glEnableVertexAttribArray(aPosPoint)
        GLES20.glVertexAttribPointer(aPosPoint, 2, GLES20.GL_FLOAT, false, 0, 0)
        GLES20.glDrawArrays(GLES20.GL_POINTS, 0, count)
        GLES20.glDisableVertexAttribArray(aPosPoint)
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
    }

    private fun drawLines(vbo: Int, count: Int, color: FloatArray) {
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

        worldToPixelMatrix.reset()
        worldToPixelMatrix.postTranslate(-md.originX, -md.originY)
        worldToPixelMatrix.postScale(1f / res, -1f / res)
        worldToPixelMatrix.postTranslate(0f, md.height.toFloat())
        totalMatrix.set(mapView.outerMatrix)
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
            color = Color.rgb(128, 0, 128)
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
        return tex[0]
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        keyFrames3D.clear()
        parent.clear()
        // 清理实时点云数据与复用缓存
        liveVertexCount = 0
        liveDirty = false
        liveBuffer = null
        liveBufferCapacity = 0
        lastLenLive = 0
        liveKeyframeCloudBuf = null
        liveKeyframeWorldBuf = null
        GLES20.glDeleteProgram(programPoint)
        GLES20.glDeleteProgram(programText)
        GLES20.glDeleteProgram(programRobot)
        GLES20.glDeleteTextures(1, intArrayOf(textTexture, robotTexture), 0)
        if (vboPointCloud[0] != 0) GLES20.glDeleteBuffers(1, vboPointCloud, 0)
        if (vboLivePoint[0] != 0) GLES20.glDeleteBuffers(1, vboLivePoint, 0)
        if (vboKeyframePoints[0] != 0) GLES20.glDeleteBuffers(1, vboKeyframePoints, 0)
        if (vboKeyframeLines[0] != 0) GLES20.glDeleteBuffers(1, vboKeyframeLines, 0)
        if (vboTextGeometry[0] != 0) GLES20.glDeleteBuffers(1, vboTextGeometry, 0)
        if (vboTextInstance[0] != 0) GLES20.glDeleteBuffers(1, vboTextInstance, 0)
        if (vboRobot[0] != 0) GLES20.glDeleteBuffers(1, vboRobot, 0)
    }
}