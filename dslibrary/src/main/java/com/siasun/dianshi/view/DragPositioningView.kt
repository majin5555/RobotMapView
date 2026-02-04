package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import com.ngu.lcmtypes.laser_t
import java.lang.ref.WeakReference
import kotlin.math.atan2
import kotlin.math.cos
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

    companion object {
        private val paint: Paint = Paint().apply {
            color = Color.RED
            strokeWidth = 2f
            style = Paint.Style.FILL
        }
    }


    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 只有在绘制启用状态下才绘制点云
        if (cloudList.isNotEmpty()) {
            val mapView = parent.get() ?: return

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
        val mapView = parent.get() ?: return
        val screenToWorld = mapView.screenToWorld(robotCenter.x + offsetX, robotCenter.y + offsetY)
        dragRobotPose.x = screenToWorld.x
        dragRobotPose.y = screenToWorld.y
        dragRobotPose.theta = initialRobotTheta + offsetRotation
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        currentWorkMode = mode
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
                isDragging = true
                needResetAnchor = false
                val worldPoint = mapView.screenToWorld(event.x, event.y)
                lastTouchX = worldPoint.x
                lastTouchY = worldPoint.y
                return true
            }

            MotionEvent.ACTION_POINTER_DOWN -> {
                if (event.pointerCount == 2) {
                    isRotating = true
                    lastFingerRotation = rotation(event)
                }
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                if (isRotating && event.pointerCount == 2) {
                    val currentRotation = rotation(event)
                    val delta = currentRotation - lastFingerRotation
                    offsetRotation -= delta

                    lastFingerRotation = currentRotation
                    updateDragRobotPose()
                    postInvalidate()
                    return true
                }

                if (isDragging && !isRotating) {
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
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                isDragging = false
                isRotating = false
                return true
            }

            MotionEvent.ACTION_POINTER_UP -> {
                if (event.pointerCount == 2) {
                    isRotating = false
                    needResetAnchor = true
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
