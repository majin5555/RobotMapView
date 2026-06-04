package com.siasun.dianshi.view.createMap.map3D

import android.content.Context
import android.graphics.Color
import android.opengl.*
import android.util.AttributeSet
import android.view.TextureView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

class PointCloudTextureView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : TextureView(context, attrs, defStyleAttr), TextureView.SurfaceTextureListener {

    var dotColor = Color.BLACK
        set(value) {
            field = value
            glThread?.updateDotColor(value)
        }
    var dotSize = 3f
        set(value) {
            field = value
            glThread?.updateDotSize(value)
        }
    var backgroundColor = Color.TRANSPARENT
        set(value) {
            field = value
            glThread?.updateBackgroundColor(value)
        }

    @Volatile
    var totalPoints: Int = 0
        private set

    private val newPointsLock = Any()
    private val pendingPoints = mutableListOf<Float>()

    private var glThread: GLThread? = null
    private var surfaceWidth = 0
    private var surfaceHeight = 0

    init {
        isOpaque = false
        surfaceTextureListener = this
        isFocusable = false
        isFocusableInTouchMode = false
    }

    fun addPoints(points: FloatArray) {
        if (points.isEmpty() || points.size % 2 != 0) return
        synchronized(newPointsLock) {
            pendingPoints.addAll(points.toList())
        }
        glThread?.requestRender()
    }

    fun clearDots() {
        synchronized(newPointsLock) {
            pendingPoints.clear()
        }
        glThread?.clearPoints()
    }

    /**
     * 设置变换矩阵（由外部传入 android.graphics.Matrix 的 9 值数组）
     */
    fun setTransformMatrix(matrix: android.graphics.Matrix) {
        val values = FloatArray(9)
        matrix.getValues(values)
        glThread?.updateTransform(values)
        glThread?.requestRender()
    }

    override fun onSurfaceTextureAvailable(st: android.graphics.SurfaceTexture, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        glThread = GLThread(st).also {
            it.setSize(width, height)
            it.updateDotColor(dotColor)
            it.updateDotSize(dotSize)
            it.updateBackgroundColor(backgroundColor)
            it.start()
        }
    }

    override fun onSurfaceTextureSizeChanged(st: android.graphics.SurfaceTexture, width: Int, height: Int) {
        surfaceWidth = width
        surfaceHeight = height
        glThread?.setSize(width, height)
    }

    override fun onSurfaceTextureDestroyed(st: android.graphics.SurfaceTexture): Boolean {
        glThread?.shutdown()
        glThread = null
        return true
    }

    override fun onSurfaceTextureUpdated(st: android.graphics.SurfaceTexture) {}

    // ========== OpenGL 渲染线程 ==========
    private inner class GLThread(private val surfaceTexture: android.graphics.SurfaceTexture) : Thread() {

        private var eglDisplay: EGLDisplay? = null
        private var eglContext: EGLContext? = null
        private var eglSurface: EGLSurface? = null
        private var program = 0
        private var mvpMatrixHandle = 0
        private var colorUniformHandle = 0
        private var pointSizeUniformHandle = 0
        private var vbo = 0
        private var pointCount = 0

        private val projectionMatrix = FloatArray(16)
        private val modelMatrix = FloatArray(16)
        private val mvpMatrix = FloatArray(16)

        private var dotR = 0f; private var dotG = 0f; private var dotB = 0f; private var dotA = 1f
        private var dotSizeGL = 3f
        private var bgR = 0f; private var bgG = 0f; private var bgB = 0f; private var bgA = 0f

        private val pendingMatrixValues = FloatArray(9)
        private var matrixDirty = false
        private val matrixLock = Any()

        private var vertexBuffer: FloatBuffer? = null
        private var vertexBufferCapacity = 0
        private var vboCapacity = 0
        private var needsUpload = true

        @Volatile
        private var running = true
        private val lock = Object()
        @Volatile
        private var renderRequested = false

        fun setSize(w: Int, h: Int) {
            android.opengl.Matrix.orthoM(projectionMatrix, 0, 0f, w.toFloat(), h.toFloat(), 0f, -1f, 1f)
        }

        fun updateTransform(values: FloatArray) {
            synchronized(matrixLock) {
                System.arraycopy(values, 0, pendingMatrixValues, 0, 9)
                matrixDirty = true
            }
        }

        fun updateDotColor(color: Int) {
            dotR = Color.red(color) / 255f
            dotG = Color.green(color) / 255f
            dotB = Color.blue(color) / 255f
            dotA = Color.alpha(color) / 255f
        }

        fun updateDotSize(size: Float) {
            dotSizeGL = size
        }

        fun updateBackgroundColor(color: Int) {
            bgR = Color.red(color) / 255f
            bgG = Color.green(color) / 255f
            bgB = Color.blue(color) / 255f
            bgA = Color.alpha(color) / 255f
        }

        fun requestRender() {
            synchronized(lock) {
                renderRequested = true
                lock.notifyAll()
            }
        }

        fun clearPoints() {
            synchronized(vertexBuffer ?: this) {
                vertexBuffer?.clear()
                pointCount = 0
                totalPoints = 0
                needsUpload = true
            }
            requestRender()
        }

        fun shutdown() {
            running = false
            requestRender()
        }

        override fun run() {
            initGL()
            while (running) {
                synchronized(lock) {
                    while (!renderRequested && running) {
                        lock.wait()
                    }
                    renderRequested = false
                }
                if (!running) break
                processPendingPoints()
                drawFrame()
            }
            releaseGL()
        }

        private fun initGL() {
            val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            val version = IntArray(2)
            EGL14.eglInitialize(display, version, 0, version, 1)

            val configAttribs = intArrayOf(
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8,
                EGL14.EGL_BLUE_SIZE, 8, EGL14.EGL_ALPHA_SIZE, 8,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_WINDOW_BIT, EGL14.EGL_NONE
            )
            val configs = arrayOfNulls<EGLConfig>(1)
            val numConfigs = IntArray(1)
            EGL14.eglChooseConfig(display, configAttribs, 0, configs as Array<EGLConfig>, 0, 1, numConfigs, 0)
            val config = configs[0]!!

            val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
            val context = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0)

            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            val surface = EGL14.eglCreateWindowSurface(display, config, surfaceTexture, surfaceAttribs, 0)

            EGL14.eglMakeCurrent(display, surface, surface, context)

            eglDisplay = display
            eglContext = context
            eglSurface = surface

            val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, VERTEX_SHADER_CODE)
            val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, FRAGMENT_SHADER_CODE)
            program = GLES20.glCreateProgram().also {
                GLES20.glAttachShader(it, vertexShader)
                GLES20.glAttachShader(it, fragmentShader)
                GLES20.glLinkProgram(it)
            }
            mvpMatrixHandle = GLES20.glGetUniformLocation(program, "u_mvpMatrix")
            colorUniformHandle = GLES20.glGetUniformLocation(program, "u_color")
            pointSizeUniformHandle = GLES20.glGetUniformLocation(program, "u_pointSize")
            GLES20.glUseProgram(program)

            val buffers = IntArray(1)
            GLES20.glGenBuffers(1, buffers, 0)
            vbo = buffers[0]

            android.opengl.Matrix.setIdentityM(modelMatrix, 0)
        }

        private fun processPendingPoints() {
            val newPoints = synchronized(newPointsLock) {
                if (pendingPoints.isEmpty()) null
                else pendingPoints.toFloatArray().also { pendingPoints.clear() }
            }
            if (newPoints != null && newPoints.isNotEmpty()) {
                val newPointCount = newPoints.size / 2
                val requiredFloats = (pointCount + newPointCount) * 2

                synchronized(vertexBuffer ?: this) {
                    if (vertexBuffer == null || requiredFloats > vertexBufferCapacity) {
                        val newCapacity = maxOf(vertexBufferCapacity * 2, requiredFloats).coerceAtLeast(1024)
                        val newBuffer = ByteBuffer
                            .allocateDirect(newCapacity * 4)
                            .order(ByteOrder.nativeOrder())
                            .asFloatBuffer()
                        vertexBuffer?.let { old ->
                            old.rewind()
                            newBuffer.put(old)
                        }
                        vertexBuffer = newBuffer
                        vertexBufferCapacity = newCapacity
                    }

                    vertexBuffer!!.position(pointCount * 2)
                    vertexBuffer!!.put(newPoints)

                    pointCount += newPointCount
                    totalPoints = pointCount
                    needsUpload = true
                }
            }
        }

        private fun drawFrame() {
            if (eglDisplay == null || eglSurface == null) return
            EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

            // 应用最新的模型矩阵
            synchronized(matrixLock) {
                if (matrixDirty) {
                    val m = pendingMatrixValues
                    modelMatrix[0] = m[0]; modelMatrix[1] = m[3]; modelMatrix[2] = 0f; modelMatrix[3] = m[6]
                    modelMatrix[4] = m[1]; modelMatrix[5] = m[4]; modelMatrix[6] = 0f; modelMatrix[7] = m[7]
                    modelMatrix[8] = 0f; modelMatrix[9] = 0f; modelMatrix[10] = 1f; modelMatrix[11] = 0f
                    modelMatrix[12] = m[2]; modelMatrix[13] = m[5]; modelMatrix[14] = 0f; modelMatrix[15] = m[8]
                    matrixDirty = false
                }
            }
            android.opengl.Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, modelMatrix, 0)

            // 上传点数据到 VBO
            if (needsUpload) {
                synchronized(vertexBuffer ?: this) {
                    vertexBuffer?.let { buf ->
                        buf.rewind()
                        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
                        val totalFloats = pointCount * 2
                        if (totalFloats > vboCapacity) {
                            val newVboCapacity = (totalFloats * 1.5).toInt().coerceAtLeast(1024)
                            GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER, newVboCapacity * 4, null, GLES20.GL_DYNAMIC_DRAW)
                            vboCapacity = newVboCapacity
                        }
                        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0, totalFloats * 4, buf)
                        needsUpload = false
                    }
                }
            }

            // 设置背景色、点颜色、点大小
            GLES20.glClearColor(bgR, bgG, bgB, bgA)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            GLES20.glUseProgram(program)
            GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
            GLES20.glUniform4f(colorUniformHandle, dotR, dotG, dotB, dotA)
            GLES20.glUniform1f(pointSizeUniformHandle, dotSizeGL)

            if (pointCount > 0) {
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vbo)
                val positionHandle = GLES20.glGetAttribLocation(program, "a_position")
                GLES20.glEnableVertexAttribArray(positionHandle)
                GLES20.glVertexAttribPointer(positionHandle, 2, GLES20.GL_FLOAT, false, 8, 0)
                GLES20.glDrawArrays(GLES20.GL_POINTS, 0, pointCount)
                GLES20.glDisableVertexAttribArray(positionHandle)
                GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, 0)
            }

            EGL14.eglSwapBuffers(eglDisplay, eglSurface)
        }

        private fun releaseGL() {
            GLES20.glDeleteProgram(program)
            GLES20.glDeleteBuffers(1, intArrayOf(vbo), 0)
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglTerminate(eglDisplay)
        }

        private fun loadShader(type: Int, code: String): Int {
            return GLES20.glCreateShader(type).also { shader ->
                GLES20.glShaderSource(shader, code)
                GLES20.glCompileShader(shader)
            }
        }
    }

    companion object {
        private const val VERTEX_SHADER_CODE = """
            uniform mat4 u_mvpMatrix;
            attribute vec4 a_position;
            uniform float u_pointSize;
            void main() {
                gl_Position = u_mvpMatrix * a_position;
                gl_PointSize = u_pointSize;
            }
        """
        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            uniform vec4 u_color;
            void main() {
                gl_FragColor = u_color;
            }
        """
    }
}