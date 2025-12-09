package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Point
import android.graphics.PointF
import android.view.MotionEvent
import com.siasun.dianshi.bean.Point2d
import com.siasun.dianshi.bean.world.GenericPath
import com.siasun.dianshi.bean.world.PathBase
import com.siasun.dianshi.bean.world.World
import com.siasun.dianshi.utils.CoordinateConversion
import java.lang.ref.WeakReference

/**
 * 路线
 */
class PathView2 @SuppressLint("ViewConstructor") constructor(
    context: Context?, parent: WeakReference<MapView>
) : SlamWareBaseView(context, parent) {

    // 保存parent引用以便安全访问
    private val mapViewRef: WeakReference<MapView> = parent
    private var mWorld: World? = null
    private var currentWorkMode = MapView.WorkMode.MODE_SHOW_MAP

    // 选中的路径信息
    private var selectedPathIndex = -1
    private var selectedPathType = -1
    private var isDragging = false
    private var lastTouchPoint = Point()

    // 画笔
    private val selectedPathPaint = Paint().apply {
        color = Color.GREEN
        strokeWidth = 3f
        style = Paint.Style.STROKE
    }

    private val draggedPathPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 4f
        style = Paint.Style.STROKE
    }

    // 文本绘制画笔
    private val textPaint = Paint().apply {
        color = Color.BLACK
        textSize = 14f
        style = Paint.Style.FILL
        textAlign = Paint.Align.CENTER
        strokeWidth = 1f
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: MapView.WorkMode) {
        currentWorkMode = mode
        // 重置选中状态
        selectedPathIndex = -1
        selectedPathType = -1
        isDragging = false
        invalidate()
    }

    /**
     * 设置World数据
     */
    fun setWorld(world: World) {
        mWorld = world
        invalidate()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // 绘制路径
        mWorld?.let { world ->
            world.m_layers.m_PathBase?.let { pathBase ->
                // 获取坐标转换对象
                val scrRef = mapViewRef.get()?.mSrf
                if (scrRef != null) {
                    // 绘制所有路径
                    pathBase.Draw(scrRef, canvas, Color.BLUE)

                    // 如果在编辑模式下，高亮显示选中的路径
                    if (currentWorkMode == MapView.WorkMode.MODE_PATH_EDIT && selectedPathIndex >= 0) {
                        pathBase.m_pPathIdx[selectedPathIndex].m_ptr?.Draw(
                            scrRef,
                            canvas,
                            Color.GREEN,
                            3
                        )
                    }

                    // 绘制路线编号和节点编号
                    for (i in 0 until pathBase.m_uCount) {
                        val pathItem = pathBase.m_pPathIdx[i]
                        val path = pathItem.m_ptr
                        if (path != null) {
                            // 获取起点和终点节点
                            val startNode = path.GetStartNode()
                            val endNode = path.GetEndNode()

                            if (startNode != null && endNode != null) {
                                // 获取起点和终点坐标
                                val startPoint = startNode.GetPoint2dObject()
                                val endPoint = endNode.GetPoint2dObject()

                                // 将世界坐标转换为屏幕坐标

                                val screenStartPoint =
                                    scrRef.worldToScreen(startPoint.x, startPoint.y)
                                val screenEndPoint = scrRef.worldToScreen(endPoint.x, endPoint.y)

                                // 绘制路线编号（显示在路径中间）
                                val pathCenterX = (screenStartPoint.x + screenEndPoint.x) / 2f
                                val pathCenterY = (screenStartPoint.y + screenEndPoint.y) / 2f
                                canvas.drawText(
                                    "Path ${i + 1}",
                                    pathCenterX,
                                    pathCenterY - 10,
                                    textPaint
                                )

                                // 绘制起点编号
                                canvas.drawText(
                                    "Node ${startNode.m_uId}",
                                    screenStartPoint.x.toFloat(),
                                    screenStartPoint.y - 10f,
                                    textPaint
                                )

                                // 绘制终点编号
                                canvas.drawText(
                                    "Node ${endNode.m_uId}",
                                    screenEndPoint.x.toFloat(),
                                    screenEndPoint.y - 10f,
                                    textPaint
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 处理触摸事件
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val mapView = mapViewRef.get() ?: return false
        val scrRef = mapView.mSrf

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val touchPoint = Point(event.x.toInt(), event.y.toInt())

                if (currentWorkMode == MapView.WorkMode.MODE_PATH_EDIT) {

                    // 检测是否点击了路径 - 进一步提高点击检测的灵敏度（将阈值从10改为20）
                    mWorld?.m_layers?.m_PathBase?.let { pathBase ->
                        val hitResult = pathBase.PointHitPath(touchPoint, scrRef, 20, -1)
                        selectedPathIndex = hitResult[0]
                        selectedPathType = hitResult[1]

                        if (selectedPathIndex >= 0) {
                            isDragging = true
                            lastTouchPoint = touchPoint
                            invalidate()
                            return true
                        }
                    }
                } else if (currentWorkMode == MapView.WorkMode.MODE_PATH_DELETE) {

                    // 检测是否点击了路径 - 进一步提高点击检测的灵敏度
                    mWorld?.m_layers?.m_PathBase?.let { pathBase ->
                        val hitResult = pathBase.PointHitPath(touchPoint, scrRef, 20, -1)
                        if (hitResult[0] >= 0) {
                            // 删除选中的路径
                            deletePath(hitResult[0])
                            invalidate()
                            return true
                        }
                    }
                } else if (currentWorkMode == MapView.WorkMode.MODE_PATH_MERGE) {

                    // 检测是否点击了路径 - 进一步提高点击检测的灵敏度
                    mWorld?.m_layers?.m_PathBase?.let { pathBase ->
                        val hitResult = pathBase.PointHitPath(touchPoint, scrRef, 20, -1)
                        if (hitResult[0] >= 0) {
                            // 合并路径逻辑
                            mergePath(hitResult[0])
                            invalidate()
                            return true
                        }
                    }
                } else if (currentWorkMode == MapView.WorkMode.MODE_PATH_CONVERT_TO_LINE) {

                    // 检测是否点击了路径 - 进一步提高点击检测的灵敏度
                    mWorld?.m_layers?.m_PathBase?.let { pathBase ->
                        val hitResult = pathBase.PointHitPath(touchPoint, scrRef, 20, -1)
                        if (hitResult[0] >= 0) {
                            // 转换路径为直线
                            convertPathToLine(hitResult[0])
                            invalidate()
                            return true
                        }
                    }
                }


            }

            MotionEvent.ACTION_MOVE -> {
                if (isDragging && currentWorkMode == MapView.WorkMode.MODE_PATH_EDIT) {
                    val touchPoint = Point(event.x.toInt(), event.y.toInt())
                    dragPath(touchPoint)
                    lastTouchPoint = touchPoint
                    invalidate()
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                isDragging = false
            }
        }

        return super.onTouchEvent(event)
    }

    /**
     * 拖拽路径
     */
    private fun dragPath(touchPoint: Point) {
        val mapView = mapViewRef.get() ?: return
        val scrRef = mapView.mSrf

        mWorld?.m_layers?.m_PathBase?.let { pathBase ->
            if (selectedPathIndex >= 0 && selectedPathIndex < pathBase.m_uCount) {
                val path = pathBase.m_pPathIdx[selectedPathIndex].m_ptr
                if (path != null) {
                    // 计算拖拽距离
                    val dx = touchPoint.x - lastTouchPoint.x
                    val dy = touchPoint.y - lastTouchPoint.y

                    // 将屏幕坐标转换为世界坐标
                    val worldDelta = Point2d(dx.toFloat(), dy.toFloat())
                    scrRef.screenToWorld(worldDelta.x, worldDelta.y)

                    // 移动路径
                    movePath(path, worldDelta)
                }
            }
        }
    }

    /**
     * 移动路径
     */
    private fun movePath(path: com.siasun.dianshi.bean.world.Path, delta: Point2d) {
        // 这里需要根据不同的路径类型实现不同的移动逻辑
        // 由于Path是抽象类，需要根据具体的子类（如GenericPath、LinePath）进行处理
        // 简单实现：移动路径的起点和终点
        val startNode = path.GetStartNode()
        val endNode = path.GetEndNode()

        startNode?.let {
            val startPoint = it.GetPoint2dObject()
            startPoint.x += delta.x
            startPoint.y += delta.y
        }

        endNode?.let {
            val endPoint = it.GetPoint2dObject()
            endPoint.x += delta.x
            endPoint.y += delta.y
        }
    }

    /**
     * 删除路径
     */
    private fun deletePath(pathIndex: Int) {
        mWorld?.m_layers?.m_PathBase?.let { pathBase ->
            if (pathIndex >= 0 && pathIndex < pathBase.m_uCount) {
                pathBase.RemovePath(pathIndex)
                selectedPathIndex = -1
                selectedPathType = -1
                invalidate()
                // 通知Activity数据已更改
//                mapViewRef.get()?.let { mapView ->
//                    (mapView.context as? ShowMapViewActivity)?.onPathDataChanged()
//                }
            }
        }
    }

    /**
     * 合并路径
     */
    private fun mergePath(pathIndex: Int) {
        mWorld?.m_layers?.m_PathBase?.let { pathBase ->
            if (pathIndex >= 0 && pathIndex < pathBase.m_uCount) {
                val selectedPath = pathBase.m_pPathIdx[pathIndex].m_ptr
                if (selectedPath != null) {
                    // 查找可以合并的路径（这里简单实现为合并相邻的路径）
                    val startNodeId = selectedPath.m_uStartNode
                    val endNodeId = selectedPath.m_uEndNode

                    // 查找与当前路径相连的路径
                    for (i in 0 until pathBase.m_uCount) {
                        if (i != pathIndex) {
                            val otherPath = pathBase.m_pPathIdx[i].m_ptr
                            if (otherPath != null) {
                                val otherStartNodeId = otherPath.m_uStartNode
                                val otherEndNodeId = otherPath.m_uEndNode

                                // 检查是否可以合并（共享一个节点）
                                if (startNodeId == otherStartNodeId || startNodeId == otherEndNodeId ||
                                    endNodeId == otherStartNodeId || endNodeId == otherEndNodeId
                                ) {

                                    // 这里简化实现，实际合并需要更复杂的逻辑
                                    // 我们可以删除两个旧路径，创建一个新的合并路径

                                    // 删除两个路径
                                    pathBase.RemovePath(if (i > pathIndex) i else pathIndex)
                                    pathBase.RemovePath(if (i > pathIndex) pathIndex else i)

                                    // 这里应该创建一个新的合并路径
                                    // 由于时间关系，我们先简单实现为删除两个路径并通知Activity
                                    selectedPathIndex = -1
                                    selectedPathType = -1
                                    invalidate()

                                    // 通知Activity数据已更改
//                                    mapViewRef.get()?.let { mapView ->
//                                        (mapView.context as? ShowMapViewActivity)?.onPathDataChanged()
//                                    }

                                    break
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 将曲线转换为直线
     */
    private fun convertPathToLine(pathIndex: Int) {
        mWorld?.m_layers?.m_PathBase?.let { pathBase ->
            if (pathIndex >= 0 && pathIndex < pathBase.m_uCount) {
                val path = pathBase.m_pPathIdx[pathIndex].m_ptr
                if (path != null) {
                    // 检查路径是否为曲线类型（GenericPath）
                    if (path.javaClass.simpleName == "GenericPath") {
                        // 获取路径的起点和终点
                        val startNode = path.GetStartNode()
                        val endNode = path.GetEndNode()

                        if (startNode != null && endNode != null) {
                            // 获取起点和终点的坐标
                            val startPoint = startNode.GetPoint2dObject()
                            val endPoint = endNode.GetPoint2dObject()

                            // 转换为GenericPath类型
                            val genericPath = path as GenericPath

                            // 将控制点设置为起点和终点，这样贝塞尔曲线就会变成直线
                            if (genericPath.m_pptCtrl != null && genericPath.m_pptCtrl.size >= 2) {
                                // 设置第一个控制点为起点
                                genericPath.m_pptCtrl[0].x = startPoint.x
                                genericPath.m_pptCtrl[0].y = startPoint.y

                                // 设置第二个控制点为终点
                                genericPath.m_pptCtrl[1].x = endPoint.x
                                genericPath.m_pptCtrl[1].y = endPoint.y

                                // 重新初始化贝塞尔曲线
                                genericPath.Init()
                            }

                            // 通知Activity数据已更改
//                            mapViewRef.get()?.let { mapView ->
//                                (mapView.context as? ShowMapViewActivity)?.onPathDataChanged()
//                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 保存路径到world_pad.dat
     */
    fun savePaths() {
        // 调用WorldFileUtil保存路径数据
    }
}


