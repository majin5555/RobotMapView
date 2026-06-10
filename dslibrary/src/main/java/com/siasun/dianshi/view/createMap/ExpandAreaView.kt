package com.siasun.dianshi.view.createMap

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.view.MotionEvent
import com.siasun.dianshi.bean.End
import com.siasun.dianshi.bean.ExpandArea
import com.siasun.dianshi.bean.Start
import com.siasun.dianshi.view.WorkMode
import com.siasun.dianshi.view.SlamWareBaseView
import java.lang.ref.WeakReference
import kotlin.math.max
import kotlin.math.min

/**
 * 扩展地图的View 支持2D、3D
 * 支持拖拽和编辑生成矩形功能
 */
@SuppressLint("ViewConstructor")
class ExpandAreaView<T : MapViewInterface>(context: Context?, parent: WeakReference<T>) :
    SlamWareBaseView<T>(context, parent) {
    private var currentWorkMode = WorkMode.MODE_SHOW_MAP


    // 创建过程状态
    private var isCreating = false
    private var editMode = EditMode.NONE
    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var createStartPoint: Start? = null
    private var createEndPoint: End? = null

    private enum class EditMode {
        NONE, DRAG_ALL,
        RESIZE_TL, RESIZE_TR, RESIZE_BL, RESIZE_BR,
        RESIZE_LEFT, RESIZE_RIGHT, RESIZE_TOP, RESIZE_BOTTOM
    }

    // 画笔定义（使用伴生对象创建静态实例，避免重复创建）
    companion object {
        private val creatingRectPaint = Paint().apply {
            color = Color.YELLOW
            style = Paint.Style.FILL
            strokeWidth = 3f
            alpha = 180 // 半透明
            isAntiAlias = true
        }

        private val cornerPaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
        }

        private val cornerStrokePaint = Paint().apply {
            color = Color.parseColor("#1976D2") // 蓝色描边
            style = Paint.Style.STROKE
            strokeWidth = 4f
            isAntiAlias = true
        }

        private val textPaint = Paint().apply {
            color = Color.CYAN
            textSize = 36f
            isAntiAlias = true
            // 增加阴影以保证在复杂背景下的可读性
            setShadowLayer(4f, 1f, 1f, Color.BLACK)
        }
    }

    // 复用的Path对象
    private val tempPath = Path()

    // 区域创建完成监听器
    private var onExpandAreaCreatedListener: OnExpandAreaCreatedListener? = null

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: WorkMode) {
        // 重置状态
        if (mode != WorkMode.MODE_EXTEND_MAP_ADD_REGION) {
            isCreating = false
            editMode = EditMode.NONE
        }

        currentWorkMode = mode

    }

    /**
     * 重置创建状态
     */
    fun resetCreateState() {
        isCreating = false
        editMode = EditMode.NONE
        createStartPoint = null
        createEndPoint = null
        postInvalidate()
    }

    /**
     * 设置区域创建完成监听器
     */
    fun setOnExpandAreaCreatedListener(listener: OnExpandAreaCreatedListener) {
        this.onExpandAreaCreatedListener = listener
    }

    /**
     * 区域创建完成监听器接口
     */
    interface OnExpandAreaCreatedListener {
        fun onExpandAreaCreated(area: ExpandArea)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 只在扩展地图增加区域模式下响应触摸事件
        if (currentWorkMode != WorkMode.MODE_EXTEND_MAP_ADD_REGION) return false

        return handleCreateModeTouch(event)
    }

    private fun getEditMode(eventX: Float, eventY: Float, pTL: PointF, pTR: PointF, pBR: PointF, pBL: PointF): EditMode {
        val threshold = 60f // 增加边缘和角点的点击阈值(屏幕像素)

        fun dist(px: Float, py: Float) = Math.hypot((eventX - px).toDouble(), (eventY - py).toDouble()).toFloat()

        // 优先判断角点拖拽
        if (dist(pTL.x, pTL.y) < threshold) return EditMode.RESIZE_TL
        if (dist(pTR.x, pTR.y) < threshold) return EditMode.RESIZE_TR
        if (dist(pBR.x, pBR.y) < threshold) return EditMode.RESIZE_BR
        if (dist(pBL.x, pBL.y) < threshold) return EditMode.RESIZE_BL

        // 再判断边缘拖拽
        fun distToSegment(x: Float, y: Float, x1: Float, y1: Float, x2: Float, y2: Float): Float {
            val l2 = (x1 - x2) * (x1 - x2) + (y1 - y2) * (y1 - y2)
            if (l2 == 0f) return dist(x1, y1)
            var t = ((x - x1) * (x2 - x1) + (y - y1) * (y2 - y1)) / l2
            t = max(0f, min(1f, t))
            return dist(x1 + t * (x2 - x1), y1 + t * (y2 - y1))
        }

        if (distToSegment(eventX, eventY, pTL.x, pTL.y, pBL.x, pBL.y) < threshold) return EditMode.RESIZE_LEFT
        if (distToSegment(eventX, eventY, pTR.x, pTR.y, pBR.x, pBR.y) < threshold) return EditMode.RESIZE_RIGHT
        if (distToSegment(eventX, eventY, pTL.x, pTL.y, pTR.x, pTR.y) < threshold) return EditMode.RESIZE_TOP
        if (distToSegment(eventX, eventY, pBL.x, pBL.y, pBR.x, pBR.y) < threshold) return EditMode.RESIZE_BOTTOM

        return EditMode.NONE
    }

    /**
     * 处理创建模式的触摸事件
     */
    private fun handleCreateModeTouch(event: MotionEvent): Boolean {
        val mapView = mParent.get() ?: return false

        // 避免多指操作时（如双指缩放地图）误触发拖拽导致坐标系变化
        if (event.pointerCount > 1) {
            if (editMode != EditMode.NONE) {
                editMode = EditMode.NONE
            }
            return false
        }

        if (currentWorkMode == WorkMode.MODE_EXTEND_MAP_ADD_REGION) {
            val worldPoint = mapView.screenToWorld(event.x, event.y)

            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    if (createStartPoint != null && createEndPoint != null) {
                        val minX = min(createStartPoint!!.x, createEndPoint!!.x)
                        val maxX = max(createStartPoint!!.x, createEndPoint!!.x)
                        val minY = min(createStartPoint!!.y, createEndPoint!!.y)
                        val maxY = max(createStartPoint!!.y, createEndPoint!!.y)

                        // 规范化坐标点为 左上角 和 右下角
                        createStartPoint = Start(minX, minY)
                        createEndPoint = End(maxX, maxY)

                        // 转换为屏幕坐标进行距离计算（以提供稳定的点击热区）
                        val pTL = mapView.worldToScreen(minX, minY)
                        val pTR = mapView.worldToScreen(maxX, minY)
                        val pBR = mapView.worldToScreen(maxX, maxY)
                        val pBL = mapView.worldToScreen(minX, maxY)

                        // 检测是否点击到了边缘或角点
                        editMode = getEditMode(event.x, event.y, pTL, pTR, pBR, pBL)

                        if (editMode != EditMode.NONE) {
                            lastTouchX = worldPoint.x
                            lastTouchY = worldPoint.y
                            return true
                        }
                        // 检测是否点击在矩形内部 (拖拽整个框)
                        else if (worldPoint.x in minX..maxX && worldPoint.y in minY..maxY) {
                            editMode = EditMode.DRAG_ALL
                            lastTouchX = worldPoint.x
                            lastTouchY = worldPoint.y
                            return true
                        } else {
                            // 点击在区域外部，交给父 View 处理（允许缩放/拖动地图）
                            return false
                        }
                    }

                    // 只有当前没有正在创建的区域时，才能开始新的绘制
                    if (!isCreating) {
                        isCreating = true
                        createStartPoint = Start(worldPoint.x, worldPoint.y)
                        createEndPoint = End(worldPoint.x, worldPoint.y)
                        postInvalidate()
                        return true
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (editMode != EditMode.NONE && createStartPoint != null && createEndPoint != null) {
                        val dx = worldPoint.x - lastTouchX
                        val dy = worldPoint.y - lastTouchY

                        var newMinX = createStartPoint!!.x
                        var newMinY = createStartPoint!!.y
                        var newMaxX = createEndPoint!!.x
                        var newMaxY = createEndPoint!!.y

                        when (editMode) {
                            EditMode.DRAG_ALL -> {
                                newMinX += dx
                                newMinY += dy
                                newMaxX += dx
                                newMaxY += dy
                            }
                            EditMode.RESIZE_TL -> { newMinX += dx; newMinY += dy }
                            EditMode.RESIZE_TR -> { newMaxX += dx; newMinY += dy }
                            EditMode.RESIZE_BL -> { newMinX += dx; newMaxY += dy }
                            EditMode.RESIZE_BR -> { newMaxX += dx; newMaxY += dy }
                            EditMode.RESIZE_LEFT -> { newMinX += dx }
                            EditMode.RESIZE_RIGHT -> { newMaxX += dx }
                            EditMode.RESIZE_TOP -> { newMinY += dy }
                            EditMode.RESIZE_BOTTOM -> { newMaxY += dy }
                            else -> {}
                        }

                        // 保证 min < max，防止反向拖拽导致坐标系翻转
                        createStartPoint = Start(min(newMinX, newMaxX), min(newMinY, newMaxY))
                        createEndPoint = End(max(newMinX, newMaxX), max(newMinY, newMaxY))

                        lastTouchX = worldPoint.x
                        lastTouchY = worldPoint.y
                        postInvalidate()
                        return true
                    }

                    if (isCreating && createStartPoint != null) {
                        createEndPoint = End(worldPoint.x, worldPoint.y)
                        postInvalidate()
                        return true
                    }
                }

                MotionEvent.ACTION_UP -> {
                    if (editMode != EditMode.NONE) {
                        editMode = EditMode.NONE
                        if (createStartPoint != null && createEndPoint != null) {
                            val newArea = ExpandArea(
                                start = PointF(createStartPoint!!.x, createStartPoint!!.y),
                                end = PointF(createEndPoint!!.x, createEndPoint!!.y)
                            )
                            onExpandAreaCreatedListener?.onExpandAreaCreated(newArea)
                        }
                        return true
                    }

                    if (isCreating && createStartPoint != null && createEndPoint != null) {
                        isCreating = false
                        val newArea = ExpandArea(
                            start = PointF(createStartPoint!!.x, createStartPoint!!.y),
                            end = PointF(createEndPoint!!.x, createEndPoint!!.y)
                        )

                        onExpandAreaCreatedListener?.onExpandAreaCreated(newArea)
                        // 移除 resetCreateState() 调用，需要手动清除才能再次绘制
                        return true
                    }
                }
            }
        }
        return false
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val mapView = mParent.get() ?: return

        canvas.save()

        // 绘制正在创建的区域
        if (createStartPoint != null && createEndPoint != null) {
            val sx = createStartPoint!!.x
            val sy = createStartPoint!!.y
            val ex = createEndPoint!!.x
            val ey = createEndPoint!!.y

            // 获取四个顶点（世界坐标）
            // 左上(sx, sy) -> 右上(ex, sy) -> 右下(ex, ey) -> 左下(sx, ey)
            // 转换为屏幕坐标
            val p1 = mapView.worldToScreen(sx, sy)
            val p2 = mapView.worldToScreen(ex, sy)
            val p3 = mapView.worldToScreen(ex, ey)
            val p4 = mapView.worldToScreen(sx, ey)

            // 使用 Path 绘制，确保旋转时形状正确
            tempPath.reset()
            tempPath.moveTo(p1.x, p1.y)
            tempPath.lineTo(p2.x, p2.y)
            tempPath.lineTo(p3.x, p3.y)
            tempPath.lineTo(p4.x, p4.y)
            tempPath.close()

            canvas.drawPath(tempPath, creatingRectPaint)

            // 如果区域已经存在，且处于扩展地图增加区域模式，绘制控制角点提供编辑视觉反馈
            if (!isCreating && currentWorkMode == WorkMode.MODE_EXTEND_MAP_ADD_REGION) {
                val radius = 12f

                canvas.drawCircle(p1.x, p1.y, radius, cornerPaint)
                canvas.drawCircle(p1.x, p1.y, radius, cornerStrokePaint)

                canvas.drawCircle(p2.x, p2.y, radius, cornerPaint)
                canvas.drawCircle(p2.x, p2.y, radius, cornerStrokePaint)

                canvas.drawCircle(p3.x, p3.y, radius, cornerPaint)
                canvas.drawCircle(p3.x, p3.y, radius, cornerStrokePaint)

                canvas.drawCircle(p4.x, p4.y, radius, cornerPaint)
                canvas.drawCircle(p4.x, p4.y, radius, cornerStrokePaint)
            }

            // 寻找屏幕上视觉的左上角和右下角顶点，使得文字始终跟随视觉角点
            class PointWithWorld(val p: PointF, val wx: Float, val wy: Float)
            val pointsWithWorld = arrayOf(
                PointWithWorld(p1, sx, sy),
                PointWithWorld(p2, ex, sy),
                PointWithWorld(p3, ex, ey),
                PointWithWorld(p4, sx, ey)
            )

            var tlNode = pointsWithWorld[0]
            var brNode = pointsWithWorld[0]
            var minSum = tlNode.p.x + tlNode.p.y
            var maxSum = tlNode.p.x + tlNode.p.y

            for (i in 1..3) {
                val node = pointsWithWorld[i]
                val sum = node.p.x + node.p.y
                if (sum < minSum) {
                    minSum = sum
                    tlNode = node
                }
                if (sum > maxSum) {
                    maxSum = sum
                    brNode = node
                }
            }

            // 绘制左上角和右下角的对应世界坐标
            val tlText = String.format(java.util.Locale.US, "X:%.2f, Y:%.2f", tlNode.wx, tlNode.wy)
            val brText = String.format(java.util.Locale.US, "X:%.2f, Y:%.2f", brNode.wx, brNode.wy)

            // 左上角坐标（左对齐，显示在点外侧）
            textPaint.textAlign = Paint.Align.LEFT
            canvas.drawText(tlText, tlNode.p.x + 15f, tlNode.p.y - 15f, textPaint)

            // 右下角坐标（右对齐，显示在点外侧）
            textPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText(brText, brNode.p.x - 15f, brNode.p.y + 45f, textPaint)
        }

        canvas.restore()
    }

}
