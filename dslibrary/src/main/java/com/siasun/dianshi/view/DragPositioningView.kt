package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import com.ngu.lcmtypes.laser_t
import com.siasun.dianshi.R
import java.lang.ref.WeakReference
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * 拖拽定位 点云
 */
@SuppressLint("ViewConstructor")
class DragPositioningView(context: Context?, val parent: WeakReference<MapView>) :
    SlamWareBaseView<MapView>(context, parent) {

    //激光点云
    private val cloudList = ArrayList<PointF>()

    data class DragRobotPose(var x: Float = 0f, var y: Float = 0f, var theta: Float = 0f)

    //拖拽定位后的车体坐标
    val dragRobotPose = DragRobotPose()

    // 当前工作模式
    private var currentWorkMode: WorkMode = WorkMode.MODE_SHOW_MAP

    private var offsetX = 0f
    private var offsetY = 0f
    private var offsetRotation = 0f // 旋转偏移量 (弧度)
    private var robotCenter = PointF(0f, 0f) // 机器人初始中心
    private var initialRobotTheta = 0f

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    // 旋转相关
    private var isRotating = false
    private var lastFingerRotation = 0f
    private var needResetAnchor = false
    private val onRobotMatrix = Matrix()

    // 机器人相关
    private val robotBitmap: Bitmap? by lazy {
        BitmapFactory.decodeResource(resources, R.mipmap.current_location)
    }

    // 控制是否绘制
    private var isDrawingEnabled: Boolean = false


    /**
     * 设置是否启用绘制
     */
    fun setDrawingEnabled(enabled: Boolean) {
        this.isDrawingEnabled = enabled
        postInvalidate()
    }

    companion object {
        private val paint: Paint = Paint().apply {
            color = Color.RED
            strokeWidth = 2f
            style = Paint.Style.FILL
        }
        private val robotPaint = Paint().apply {
            isAntiAlias = true
            alpha = 255 // 完全不透明
        }
    }


    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mapView = parent.get() ?: return

        // 只有在绘制启用状态下才绘制点云
        if (cloudList.isNotEmpty()) {

            // 预分配数组大小
            val pointsArray = FloatArray(cloudList.size * 2)
            var index = 0


            for (point in cloudList) {
                // 1. 旋转 (绕 robotCenter)
                val dx = point.x - robotCenter.x
                val dy = point.y - robotCenter.y
                val rotatedX = dx * cos(offsetRotation) - dy * sin(offsetRotation) + robotCenter.x
                val rotatedY = dx * sin(offsetRotation) + dy * cos(offsetRotation) + robotCenter.y

                // 2. 平移
                val finalX = rotatedX + offsetX
                val finalY = rotatedY + offsetY

                val screenPoint = mapView.worldToScreen(finalX, finalY)
                pointsArray[index++] = screenPoint.x
                pointsArray[index++] = screenPoint.y
            }

            canvas.drawPoints(pointsArray, paint)

        }
        if (isDrawingEnabled) {
            dragRobotPose.let { pose ->
                robotBitmap?.let { bitmap ->
                    // 重置变换矩阵，避免变换累积导致的跳动
                    onRobotMatrix.reset()
                    // 将世界坐标转换为屏幕坐标
                    val screenPos = mapView.worldToScreen(pose.x, pose.y)

                    // 计算图标中心点偏移，使图标中心与坐标点重合
                    val offsetX = -bitmap.width / 2f
                    val offsetY = -bitmap.height / 2f

                    // 设置变换矩阵：
                    // 1. 先平移到原点（以图标中心为锚点）
                    onRobotMatrix.postTranslate(offsetX, offsetY)
                    // 2. 然后应用旋转（以图标中心为轴心）
                    onRobotMatrix.postRotate(
                        -pose.theta, 0f, 0f // 旋转轴心为图标中心
                    )
                    // 3. 最后平移到屏幕目标位置
                    onRobotMatrix.postTranslate(screenPos.x, screenPos.y)

                    // 绘制机器人图标
                    robotPaint?.let {
                        canvas.drawBitmap(bitmap, onRobotMatrix, it)
                    }
                }
            }
        }
    }

    /**
     * 上激光点云
     */
    fun updateUpLaserScan(laser: laser_t) {
        cloudList.clear()
        val robotX = laser.ranges[0]
        val robotY = laser.ranges[1]
        val robotT = laser.ranges[2]

        // 保存初始机器人中心
        robotCenter.set(robotX, robotY)
        initialRobotTheta = robotT

        if (laser.ranges.size > 3) {
            // 预分配容量
            val expectedSize = (laser.ranges.size / 3) - 1
            cloudList.ensureCapacity(expectedSize)

            for (i in 1 until laser.ranges.size / 3) {
                val laserX = laser.ranges[3 * i]
                val laserY = laser.ranges[3 * i + 1]
                cloudList.add(
                    PointF(
                        laserX * cos(robotT) - laserY * sin(robotT) + robotX,
                        laserX * sin(robotT) + laserY * cos(robotT) + robotY
                    )
                )
            }
        }
        updateDragRobotPose()
        postInvalidate()
    }

    /**
     * 更新拖拽后的车体坐标
     */
    private fun updateDragRobotPose() {
        dragRobotPose.x = robotCenter.x + offsetX
        dragRobotPose.y = robotCenter.y + offsetY
        dragRobotPose.theta = initialRobotTheta + offsetRotation
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        currentWorkMode = mode
        if (mode == WorkMode.MODE_DRAG_POSITION) {
            // 禁用机器人图标绘制
            parent.get()?.mRobotView?.setDrawingEnabled(false)
            setDrawingEnabled(true)
        }

        if (mode != WorkMode.MODE_DRAG_POSITION) {
            // 重置偏移量
            offsetX = 0f
            offsetY = 0f
            offsetRotation = 0f
            isDragging = false
            isRotating = false
        }
        postInvalidate()
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (currentWorkMode != WorkMode.MODE_DRAG_POSITION) return false
        val mapView = parent.get() ?: return false

        when (event.action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                // 判断点击位置是否在机器人附近
                val screenPos = mapView.worldToScreen(dragRobotPose.x, dragRobotPose.y)
                val dist = hypot(event.x - screenPos.x, event.y - screenPos.y)
                // 阈值设为80像素，可根据屏幕密度调整
                if (dist < 80) {
                    isDragging = true
                    needResetAnchor = false
                    val worldPoint = mapView.screenToWorld(event.x, event.y)
                    lastTouchX = worldPoint.x
                    lastTouchY = worldPoint.y
                    return true
                } else {
                    isDragging = false
                    // 点击非机器人区域，交给MapView处理手势（平移、缩放、旋转）
                    mapView.processMapGestures(event)
                    return true
                }
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (isDragging) {
                    if (event.pointerCount == 2) {
                        isRotating = true
                        lastFingerRotation = rotation(event)
                    }
                } else {
                    mapView.processMapGestures(event)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging) {
                    if (isRotating && event.pointerCount == 2) {
                        val currentRotation = rotation(event)
                        val delta = currentRotation - lastFingerRotation
                        offsetRotation -= delta

                        lastFingerRotation = currentRotation
                        updateDragRobotPose()
                        postInvalidate()
                        return true
                    }

                    if (!isRotating) {
                        val worldPoint = mapView.screenToWorld(event.x, event.y)

                        if (needResetAnchor) {
                            lastTouchX = worldPoint.x
                            lastTouchY = worldPoint.y
                            needResetAnchor = false
                            return true
                        }

                        val dx = worldPoint.x - lastTouchX
                        val dy = worldPoint.y - lastTouchY

                        // 累加偏移量
                        offsetX += dx
                        offsetY += dy

                        lastTouchX = worldPoint.x
                        lastTouchY = worldPoint.y
                        updateDragRobotPose()
                        postInvalidate()
                        return true
                    }
                } else {
                    mapView.processMapGestures(event)
                }
                return true
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                if (isDragging) {
                    isDragging = false
                    isRotating = false
                } else {
                    mapView.processMapGestures(event)
                }
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (isDragging) {
                    if (event.pointerCount == 2) {
                        isRotating = false
                        needResetAnchor = true
                    }
                } else {
                    mapView.processMapGestures(event)
                }
                return true
            }
        }
        return false
    }

    private fun rotation(event: MotionEvent): Float {
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        val radians = atan2(deltaY, deltaX)
        return radians.toFloat()
    }

    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        // 清理点云数据
        cloudList.clear()
        // 清理父引用
        parent.clear()
    }
}
