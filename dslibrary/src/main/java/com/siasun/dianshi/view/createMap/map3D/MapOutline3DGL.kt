package com.siasun.dianshi.view.createMap.map3D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
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
        private const val POINT_SIZE_CLOUD = 3f
        private const val POINT_SIZE_KEYFRAME = 8f
        private const val DIR_LINE_LENGTH = 0.5f

        // 文字世界高度（固定），宽度由纹理比例自动计算
        private const val CHAR_WORLD_HEIGHT = 0.4f
        private const val TEXT_OFFSET_WORLD_Y = 0.2f

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

        // 文字着色器：支持动态字符宽度和精确纹理采样
        private const val VERTEX_SHADER_TEXT = """
            uniform mat4 u_MVPMatrix;
            uniform float u_CharAspectScale;   // 纹理总宽度 / 高度，用于将UV宽度转为世界宽度
            attribute vec2 a_Vertex;
            attribute vec2 a_TexOffset;        // x: 0(左)或1(右), y: 0(下)或1(上)
            attribute vec2 a_WorldPos;          // 字符左下角世界坐标
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
    }

    private var isDetailedEnabled = false
    private var pointCloudDirty = true
    private var keyframeGeometryDirty = true

    private val keyFrames3D = ConcurrentHashMap<Int, KeyFrame>()

    private var programPoint = 0
    private var aPosPoint = 0
    private var uColorPoint = 0
    private var uMVPPoint = 0
    private var uPointSize = 0

    private var programText = 0
    private var aVertexText = 0
    private var aTexOffsetText = 0
    private var aWorldPosText = 0
    private var aUminText = 0
    private var aUmaxText = 0
    private var uMVPTex = 0
    private var uCharAspectScaleTex = 0
    private var uTextureTex = 0

    private val mvpMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private var vboPointCloud = intArrayOf(0)
    private var vboKeyframePoints = intArrayOf(0)
    private var vboKeyframeLines = intArrayOf(0)
    private var vboTextGeometry = intArrayOf(0)
    private var vboTextInstance = intArrayOf(0)

    private var pointCloudVertexCount = 0
    private var keyframePointCount = 0
    private var keyframeLineVertexCount = 0
    private var textInstanceCount = 0

    private var pointCloudLastLen = 0
    private var keyframePointsLastLen = 0
    private var keyframeLinesLastLen = 0

    private var pointCloudBuffer: FloatBuffer? = null
    private var pointCloudBufferCapacity = 0

    private var keyframePointsArray: FloatArray? = null
    private var keyframeLinesArray: FloatArray? = null

    private var uploadBuffer: FloatBuffer? = null
    private var uploadBufferCapacity = 0

    private val worldToPixelMatrix = Matrix()
    private val totalMatrix = Matrix()
    private val matrixValues = FloatArray(9)
    private var lastRangeSize = 0

    private var textTexture = 0
    private var screenWidth = 1
    private var screenHeight = 1

    // 纹理图集参数（紧凑布局后计算）
    private var charUmin = FloatArray(11)
    private var charUmax = FloatArray(11)
    private var texAspectScale = 1f   // 纹理总宽度 / 高度

    init {
        setEGLContextClientVersion(3)
        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        setZOrderOnTop(false)
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

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 0f)
        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        programPoint = createProgram(VERTEX_SHADER_POINT, FRAGMENT_SHADER_POINT)
        aPosPoint = GLES20.glGetAttribLocation(programPoint, "a_Position")
        uColorPoint = GLES20.glGetUniformLocation(programPoint, "u_Color")
        uMVPPoint = GLES20.glGetUniformLocation(programPoint, "u_MVPMatrix")
        uPointSize = GLES20.glGetUniformLocation(programPoint, "u_PointSize")

        programText = createProgram(VERTEX_SHADER_TEXT, FRAGMENT_SHADER_TEXT)
        aVertexText = GLES20.glGetAttribLocation(programText, "a_Vertex")
        aTexOffsetText = GLES20.glGetAttribLocation(programText, "a_TexOffset")
        aWorldPosText = GLES20.glGetAttribLocation(programText, "a_WorldPos")
        aUminText = GLES20.glGetAttribLocation(programText, "a_Umin")
        aUmaxText = GLES20.glGetAttribLocation(programText, "a_Umax")
        uMVPTex = GLES20.glGetUniformLocation(programText, "u_MVPMatrix")
        uCharAspectScaleTex = GLES20.glGetUniformLocation(programText, "u_CharAspectScale")
        uTextureTex = GLES20.glGetUniformLocation(programText, "u_Texture")

        // 固定几何体 VBO（四边形 + 纹理偏移）
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
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)

        GLES20.glGenBuffers(1, vboTextInstance, 0)

        // 创建紧凑纹理并获取 uMin/uMax 数组
        textTexture = createDigitTexture()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        GMatrix.orthoM(projectionMatrix, 0, 0f, width.toFloat(), height.toFloat(), 0f, -1f, 1f)
        screenWidth = width
        screenHeight = height
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

        if (keyframeGeometryDirty) keyframeGeometryDirty = false
    }

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

//    private fun rebuildKeyframePointsVBO() {
//        val size = keyFrames3D.size
//        if (size == 0) { keyframePointCount = 0; return }
//        if (keyframePointsArray == null || keyframePointsArray!!.size < size * 2) {
//            keyframePointsArray = FloatArray(size * 2)
//        }
//        val arr = keyframePointsArray!!
//        var i = 0
//        for (kf in keyFrames3D.values) {
//            if (i + 1 >= arr.size) break
//            arr[i++] = kf.robotPos[0]
//            arr[i++] = kf.robotPos[1]
//        }
//        keyframePointCount = i / 2
//        uploadVBO(vboKeyframePoints, arr, i, keyframePointsLastLen)
//        keyframePointsLastLen = i
//    }

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
        val size = keyFrames3D.size
        if (size == 0) {
            keyframeLineVertexCount = 0; return
        }
        val needed = size * 4
        if (keyframeLinesArray == null || keyframeLinesArray!!.size < needed) {
            keyframeLinesArray = FloatArray(needed)
        }
        val arr = keyframeLinesArray!!
        var i = 0
        for (kf in keyFrames3D.values) {
            if (i + 3 >= arr.size) break
            val x = kf.robotPos[0];
            val y = kf.robotPos[1];
            val t = kf.robotPos[2]
            arr[i++] = x
            arr[i++] = y
            arr[i++] = x + DIR_LINE_LENGTH * cos(t)
            arr[i++] = y + DIR_LINE_LENGTH * sin(t)
        }
        keyframeLineVertexCount = i / 2
        uploadVBO(vboKeyframeLines, arr, i, keyframeLinesLastLen)
        keyframeLinesLastLen = i
    }

    // ---------- 文字实例 VBO ----------
//    private fun rebuildTextVBO() {
//        var totalChars = 0
//        for ((id) in keyFrames3D) totalChars += id.toString().length
//        if (totalChars == 0) {
//            textInstanceCount = 0
//            return
//        }
//
//        // 每个字符实例数据： worldX, worldY, uMin, uMax (4 floats)
//        val instanceFloats = totalChars * 4
//        val instanceArray = FloatArray(instanceFloats)
//        var idx = 0
//        for ((id, frame) in keyFrames3D) {
//            val text = id.toString()
//            val robotX = frame.robotPos[0]
//            val robotY = frame.robotPos[1] + TEXT_OFFSET_WORLD_Y
//            var cursorX = robotX
//            for (ch in text) {
//                val digit = if (ch == '-') 10 else ch - '0'
//                val uMin = charUmin[digit]
//                val uMax = charUmax[digit]
//                instanceArray[idx++] = cursorX   // worldX
//                instanceArray[idx++] = robotY    // worldY
//                instanceArray[idx++] = uMin
//                instanceArray[idx++] = uMax
//                // 计算世界宽度（用于光标推进），与着色器一致
//                val charWorldWidth = CHAR_WORLD_HEIGHT * (uMax - uMin) * texAspectScale
//                cursorX += charWorldWidth
//            }
//        }
//
//        val buffer = ByteBuffer.allocateDirect(instanceArray.size * 4)
//            .order(ByteOrder.nativeOrder()).asFloatBuffer()
//        buffer.put(instanceArray).position(0)
//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboTextInstance[0])
//        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, instanceArray.size * 4, buffer, GLES20.GL_DYNAMIC_DRAW)
//        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
//
//        textInstanceCount = totalChars
//    }

    private fun rebuildTextVBO() {
        // 获取键值对快照，防止并发修改导致数组越界
        val entries = keyFrames3D.entries.toList()
        var totalChars = 0
        for ((id, _) in entries) {
            totalChars += id.toString().length
        }
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
                instanceArray[idx++] = cursorX   // worldX
                instanceArray[idx++] = robotY    // worldY
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

        // 绑定几何体 VBO
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboTextGeometry[0])
        GLES20.glEnableVertexAttribArray(aVertexText)
        GLES20.glVertexAttribPointer(aVertexText, 2, GLES20.GL_FLOAT, false, 4 * 4, 0)
        GLES20.glEnableVertexAttribArray(aTexOffsetText)
        GLES20.glVertexAttribPointer(aTexOffsetText, 2, GLES20.GL_FLOAT, false, 4 * 4, 2 * 4)

        // 绑定实例 VBO
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

    private fun updateMVPMatrix(mapView: CreateMapView3D) {
        val md = mapView.mSrf.mapData
        val res = if (md.resolution > 0) md.resolution else 0.05f
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

    /**
     * 生成紧凑数字纹理，并为每个字符计算归一化的 uMin/uMax
     */
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

        // 测量每个字符的实际宽度
        val charWidths = FloatArray(charCount)
        var totalWidth = 0f
        for (i in 0 until charCount) {
            val w = paint.measureText(chars[i].toString())
            charWidths[i] = w
            totalWidth += w
        }

        // 创建与总宽度匹配的 Bitmap
        val bmpWidth = totalWidth.toInt()
        val bmp = Bitmap.createBitmap(bmpWidth, bmpHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        paint.textAlign = Paint.Align.LEFT
        val baseline = (bmpHeight + textSize) / 2f - 4f

        var offsetX = 0f
        for (i in 0 until charCount) {
            canvas.drawText(chars[i].toString(), offsetX, baseline, paint)
            offsetX += charWidths[i]
        }

        // 计算归一化 uMin/uMax
        for (i in 0 until charCount) {
            charUmin[i] = if (i == 0) 0f else charUmax[i - 1]
            charUmax[i] = charUmin[i] + charWidths[i] / totalWidth
        }

        // 纹理宽高比
        texAspectScale = totalWidth / bmpHeight

        // 上传纹理
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
        GLES20.glDeleteProgram(programPoint)
        GLES20.glDeleteProgram(programText)
        GLES20.glDeleteTextures(1, intArrayOf(textTexture), 0)
        if (vboPointCloud[0] != 0) GLES20.glDeleteBuffers(1, vboPointCloud, 0)
        if (vboKeyframePoints[0] != 0) GLES20.glDeleteBuffers(1, vboKeyframePoints, 0)
        if (vboKeyframeLines[0] != 0) GLES20.glDeleteBuffers(1, vboKeyframeLines, 0)
        if (vboTextGeometry[0] != 0) GLES20.glDeleteBuffers(1, vboTextGeometry, 0)
        if (vboTextInstance[0] != 0) GLES20.glDeleteBuffers(1, vboTextInstance, 0)
    }
}