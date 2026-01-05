package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.util.Log
import android.view.MotionEvent
import com.siasun.dianshi.bean.Point2d
import com.siasun.dianshi.bean.pp.DefPosture
import com.siasun.dianshi.bean.pp.Posture
import com.siasun.dianshi.bean.pp.world.CLayer
import com.siasun.dianshi.bean.pp.world.GenericPath
import com.siasun.dianshi.bean.pp.world.Node
import java.lang.ref.WeakReference
import kotlin.math.sqrt

/**
 * 路线 world_pad.dat文件绘制 编辑
 */
@SuppressLint("ViewConstructor")
class WorldPadView @SuppressLint("ViewConstructor") constructor(
    context: Context?, parent: WeakReference<MapView>
) : SlamWareBaseView(context, parent) {
    private var isDrawingEnabled: Boolean = true
    private var cLayer: CLayer? = null
    private var currentWorkMode: MapView.WorkMode = MapView.WorkMode.MODE_SHOW_MAP

    // 路段编辑相关属性
    private var selectedPath: com.siasun.dianshi.bean.pp.world.Path? = null // 当前选中的路段
    private var selectedNode: Node? = null // 当前选中的节点（用于防止重复回调）
    private var draggingNode: Node? = null // 当前正在拖动的节点
    private var draggingControlPoint: Point2d? = null // 当前正在拖动的控制点
    private var dragStartPoint: PointF? = null // 拖动开始的屏幕坐标
    private val SELECTION_RADIUS_POINT = 20f // 节点选择半径，增大以提高点击灵敏度
    private val SELECTION_RADIUS_PATH = 50f // 路段 选择半径，增大以提高点击灵敏度

    // 路线合并模式相关属性
    private var selectedMergeStartNode: Node? = null // 合并路线的起点
    private var selectedMergeStartPath: com.siasun.dianshi.bean.pp.world.Path? = null // 起点所在的路径
    private var selectedMergeEndNode: Node? = null // 合并路线的终点
    private var selectedMergeEndPath: com.siasun.dianshi.bean.pp.world.Path? = null // 终点所在的路径

    // 删除多条路线模式相关属性
    private var isBoxSelecting: Boolean = false // 是否正在进行框选
    private var boxSelectStartPoint: PointF? = null // 框选开始点
    private var boxSelectEndPoint: PointF? = null // 框选结束点
    private val selectedPathsForDeletion: MutableSet<com.siasun.dianshi.bean.pp.world.Path> =
        mutableSetOf() // 待删除的路线集合

    // 创建路线模式相关属性
    private var pathCreateStartNode: Node? = null // 创建路线的起点
    private var tempPath: GenericPath? = null // 临时创建的路径（用于绘制）

    // 保存parent引用以便安全访问
    private val mapViewRef: WeakReference<MapView> = parent

    // 优化：使用伴生对象创建静态Paint实例，避免重复创建
    companion object {
        // 编辑模式下的特殊画笔
        private val mSelectedPaint: Paint by lazy {
            Paint().apply {
                color = Color.GREEN
                strokeWidth = 3f
                isAntiAlias = true
                style = Paint.Style.STROKE
            }
        }
        private val mRedPaint: Paint by lazy {
            Paint().apply {
                color = Color.RED
                strokeWidth = 1f
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val mPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = Color.BLACK
            textSize = 10f
        }
    }


    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: MapView.WorkMode) {
        currentWorkMode = mode
        // 如果退出路径编辑模式、节点属性编辑模式、路段属性编辑模式、删除模式和创建模式，重置选择状态
        if (mode != MapView.WorkMode.MODE_PATH_EDIT && mode != MapView.WorkMode.MODE_PATH_NODE_ATTR_EDIT && mode != MapView.WorkMode.MODE_PATH_SEGMENT_ATTR_EDIT && mode != MapView.WorkMode.MODE_PATH_DELETE && mode != MapView.WorkMode.MODE_PATH_DELETE_MULTIPLE && mode != MapView.WorkMode.MODE_PATH_MERGE && mode != MapView.WorkMode.MODE_PATH_CREATE) {
            selectedPath = null
            selectedNode = null
            draggingNode = null
            draggingControlPoint = null
            dragStartPoint = null
            // 重置删除多条路线模式的属性
            isBoxSelecting = false
            boxSelectStartPoint = null
            boxSelectEndPoint = null
            selectedPathsForDeletion.clear()
            // 重置路线合并模式的属性
            selectedMergeStartNode = null
            selectedMergeStartPath = null
            selectedMergeEndNode = null
            selectedMergeEndPath = null
            // 重置创建路线模式的属性
            pathCreateStartNode = null
            tempPath = null
        } else if (mode == MapView.WorkMode.MODE_PATH_DELETE_MULTIPLE) {
            // 进入删除多条路线模式，重置相关属性
            selectedPath = null
            selectedNode = null
            draggingNode = null
            draggingControlPoint = null
            dragStartPoint = null
            isBoxSelecting = false
            boxSelectStartPoint = null
            boxSelectEndPoint = null
            selectedPathsForDeletion.clear()
            // 重置路线合并模式的属性
            selectedMergeStartNode = null
            selectedMergeStartPath = null
            selectedMergeEndNode = null
            selectedMergeEndPath = null
            // 重置创建路线模式的属性
            pathCreateStartNode = null
            tempPath = null
        } else if (mode == MapView.WorkMode.MODE_PATH_MERGE) {
            // 进入路线合并模式，重置相关属性
            selectedPath = null
            selectedNode = null
            draggingNode = null
            draggingControlPoint = null
            dragStartPoint = null
            // 重置删除多条路线模式的属性
            isBoxSelecting = false
            boxSelectStartPoint = null
            boxSelectEndPoint = null
            selectedPathsForDeletion.clear()
            // 重置路线合并模式的属性
            selectedMergeStartNode = null
            selectedMergeStartPath = null
            selectedMergeEndNode = null
            selectedMergeEndPath = null
            // 重置创建路线模式的属性
            pathCreateStartNode = null
            tempPath = null
        } else if (mode == MapView.WorkMode.MODE_PATH_CREATE) {
            // 进入创建路线模式，重置相关属性
            selectedPath = null
            selectedNode = null
            draggingNode = null
            draggingControlPoint = null
            dragStartPoint = null
            // 重置删除多条路线模式的属性
            isBoxSelecting = false
            boxSelectStartPoint = null
            boxSelectEndPoint = null
            selectedPathsForDeletion.clear()
            // 重置路线合并模式的属性
            selectedMergeStartNode = null
            selectedMergeStartPath = null
            selectedMergeEndNode = null
            selectedMergeEndPath = null
            // 初始化创建路线模式的属性
            pathCreateStartNode = null
            tempPath = null
        }
        invalidate()
    }

    /**
     * 删除指定路段及其相关节点
     */
    private fun deletePath(path: com.siasun.dianshi.bean.pp.world.Path) {
        val cLayer = this.cLayer ?: return
        val startNode = path.GetStartNode()
        val endNode = path.GetEndNode()

        // 查找路径在路径数组中的索引
        val pathIndex = findPathIndex(path)
        if (pathIndex != -1) {
            // 创建要删除的路径ID向量
            val pathIds = java.util.Vector<Int>()
            pathIds.add(pathIndex)

            // 删除路径并获取删除的节点ID
            val deletedNodeIds = cLayer.DelPath(pathIds)

            // 触发删除回调
            onPathAttributeEditListener?.onPathDeleted(path)

            // 处理删除的节点
            deletedNodeIds.forEach { nodeId ->
                // 查找并触发节点删除回调
                val node = cLayer.GetNode(nodeId)
                node?.let { onPathAttributeEditListener?.onNodeDeleted(it) }
            }

            // 重置选择状态
            selectedPath = null
            selectedNode = null
            draggingNode = null
            draggingControlPoint = null
            dragStartPoint = null

            // 重绘视图
            invalidate()
        }
    }

    /**
     * 查找路径在路径数组中的索引
     */
    private fun findPathIndex(path: com.siasun.dianshi.bean.pp.world.Path): Int {
        val cLayer = this.cLayer ?: return -1

        if (cLayer.m_PathBase != null && cLayer.m_PathBase.m_pPathIdx != null) {
            for (i in 0 until cLayer.m_PathBase.m_uCount) {
                if (cLayer.m_PathBase.m_pPathIdx[i].m_ptr === path) {
                    return i
                }
            }
        }
        return -1
    }

    /**
     * 处理创建路线模式下的点击事件
     */
    private fun handleCreatePathDownEvent(worldPoint: PointF) {
        val mapView = mapViewRef.get() ?: return
        val cLayer = cLayer ?: return
        val pathBase = cLayer.m_PathBase ?: return

        if (pathCreateStartNode == null) {
            // 首次点击，创建起点节点
            val startNode = cLayer.CreateNode(Point2d(worldPoint.x, worldPoint.y))
            // 设置起点属性
            startNode.m_uType = 1 // 1表示起点类型
            startNode.m_uExtType = 1 // 扩展类型
            startNode.m_fHeading = 0f // 航向角
            pathCreateStartNode = startNode
            // 保存临时路径用于绘制
            tempPath = GenericPath()

            invalidate()
        } else {
            // 第二次及以后点击，创建中间/终点节点和曲线路段
            val endNode = cLayer.CreateNode(Point2d(worldPoint.x, worldPoint.y))
            // 设置中间节点属性（直到用户长按结束路径创建才标记为终点）
            endNode.m_uType = 0 // 0表示普通路径节点类型
            endNode.m_uExtType = 0 // 扩展类型
            endNode.m_fHeading = 0f // 航向角

            // 创建带控制点的曲线路段
            // 计算控制点位置
            val controlPoint = calculateControlPoint(pathCreateStartNode!!, endNode)

            // 使用CLayer.CreatePath方法创建曲线路段
            val startPoint = Point2d(pathCreateStartNode!!.x, pathCreateStartNode!!.y)
            val endPoint = Point2d(endNode.x, endNode.y)
            val speed = floatArrayOf(0.5f, 0.5f) // 默认速度
            val guidFunction: Short = 0 // 默认引导功能类型
            val controlPointDistance = 0.5f // 默认控制点距离

            // 调用正确的CreatePath方法签名，创建曲线路径（类型10）
            val newPath = cLayer.CreatePath(
                startPoint,
                endPoint,
                pathCreateStartNode!!.m_uId,
                endNode.m_uId,
                10,
                speed,
                guidFunction,
                controlPointDistance
            )

            // 设置路径属性
            if (newPath != null) {
                // 如果是GenericPath类型，可以设置控制点
                if (newPath is GenericPath && newPath.m_Curve != null) {
                    newPath.m_Curve.m_ptKey = arrayOf(controlPoint)
                    newPath.m_Curve.m_nCountKeyPoints = 1
                }
                // 注意：不需要再次调用pathBase.AddPath(newPath)，因为CLayer.CreatePath内部已经添加了
            }

            // 设置当前终点为下一段路径的起点
            // 将上一段的终点节点类型从起点改为普通节点
            pathCreateStartNode?.m_uType = 0
            pathCreateStartNode?.m_uExtType = 0
            pathCreateStartNode = endNode
            // 清空临时路径，为下一段路径准备
            tempPath = null
            invalidate()
        }
    }

    /**
     * 计算控制点位置
     */
    private fun calculateControlPoint(startNode: Node, endNode: Node): Point2d {
        // 简单地计算起点和终点中间位置作为控制点
        val midX = (startNode.x + endNode.x) / 2
        val midY = (startNode.y + endNode.y) / 2
        // 可以根据需要调整控制点位置，使其产生更自然的曲线
        return Point2d(midX, midY)
    }

    /**
     * 更新框选区域内的选中路线
     */
    private fun updateBoxSelection() {
        if (boxSelectStartPoint == null || boxSelectEndPoint == null) return

        val mapView = mapViewRef.get() ?: return
        val cLayer = this.cLayer ?: return

        // 清空当前选中的路线
        selectedPathsForDeletion.clear()

        // 获取框选区域的边界
        val minX = Math.min(boxSelectStartPoint!!.x, boxSelectEndPoint!!.x)
        val minY = Math.min(boxSelectStartPoint!!.y, boxSelectEndPoint!!.y)
        val maxX = Math.max(boxSelectStartPoint!!.x, boxSelectEndPoint!!.x)
        val maxY = Math.max(boxSelectStartPoint!!.y, boxSelectEndPoint!!.y)

        // 遍历所有路线，检查是否在框选区域内
        if (cLayer.m_PathBase != null && cLayer.m_PathBase.m_pPathIdx != null) {
            for (i in 0 until cLayer.m_PathBase.m_uCount) {
                val path = cLayer.m_PathBase.m_pPathIdx[i].m_ptr
                if (isPathInBox(path, minX, minY, maxX, maxY)) {
                    selectedPathsForDeletion.add(path)
                }
            }
        }
    }

    /**
     * 检查路线是否在框选区域内
     */
    private fun isPathInBox(
        path: com.siasun.dianshi.bean.pp.world.Path,
        minX: Float,
        minY: Float,
        maxX: Float,
        maxY: Float
    ): Boolean {
        val mapView = mapViewRef.get() ?: return false
        val startNode = path.GetStartNode()
        val endNode = path.GetEndNode()

        if (startNode != null && endNode != null) {
            // 将路线的起点和终点转换为屏幕坐标
            val startScreen = mapView.worldToScreen(startNode.x, startNode.y)
            val endScreen = mapView.worldToScreen(endNode.x, endNode.y)

            // 检查起点或终点是否在框选区域内
            if ((startScreen.x >= minX && startScreen.x <= maxX && startScreen.y >= minY && startScreen.y <= maxY) || (endScreen.x >= minX && endScreen.x <= maxX && endScreen.y >= minY && endScreen.y <= maxY)) {
                return true
            }

            // 检查线段是否与框选区域相交
            val boxLines = arrayOf(
                arrayOf(PointF(minX, minY), PointF(maxX, minY)), // 上边框
                arrayOf(PointF(maxX, minY), PointF(maxX, maxY)), // 右边框
                arrayOf(PointF(maxX, maxY), PointF(minX, maxY)), // 下边框
                arrayOf(PointF(minX, maxY), PointF(minX, minY))  // 左边框
            )

            for (boxLine in boxLines) {
                if (linesIntersect(startScreen, endScreen, boxLine[0], boxLine[1])) {
                    return true
                }
            }
        }

        return false
    }

    /**
     * 检查两条线段是否相交
     */
    private fun linesIntersect(p1: PointF, p2: PointF, p3: PointF, p4: PointF): Boolean {
        val d = (p2.x - p1.x) * (p4.y - p3.y) - (p2.y - p1.y) * (p4.x - p3.x)
        if (d == 0f) return false

        val ua = ((p4.x - p3.x) * (p1.y - p3.y) - (p4.y - p3.y) * (p1.x - p3.x)) / d
        val ub = ((p2.x - p1.x) * (p1.y - p3.y) - (p2.y - p1.y) * (p1.x - p3.x)) / d

        return ua in 0f..1f && ub in 0f..1f
    }

    /**
     * 删除选中的多条路线
     */
    private fun deleteSelectedPaths() {
        if (selectedPathsForDeletion.isEmpty()) return

        val cLayer = this.cLayer ?: return

        // 创建要删除的路径ID向量
        val pathIds = java.util.Vector<Int>()
        val deletedPaths = mutableListOf<com.siasun.dianshi.bean.pp.world.Path>()

        // 收集所有要删除的路径ID
        for (path in selectedPathsForDeletion) {
            val pathIndex = findPathIndex(path)
            if (pathIndex != -1) {
                pathIds.add(pathIndex)
                deletedPaths.add(path)
            }
        }

        // 批量删除路径
        if (pathIds.isNotEmpty()) {
            val deletedNodeIds = cLayer.DelPath(pathIds)

            // 触发删除回调
            for (path in deletedPaths) {
                onPathAttributeEditListener?.onPathDeleted(path)
            }

            // 处理删除的节点
            deletedNodeIds.forEach { nodeId ->
                val node = cLayer.GetNode(nodeId)
                node?.let { onPathAttributeEditListener?.onNodeDeleted(it) }
            }
        }

        // 清空选中的路线集合
        selectedPathsForDeletion.clear()

        // 重绘视图
        invalidate()
    }

    /**
     * 处理触摸事件
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 在路径编辑模式、节点属性编辑模式、路段属性编辑模式、删除模式、合并模式和创建模式下都处理触摸事件
        if (currentWorkMode != MapView.WorkMode.MODE_PATH_EDIT && currentWorkMode != MapView.WorkMode.MODE_PATH_NODE_ATTR_EDIT && currentWorkMode != MapView.WorkMode.MODE_PATH_SEGMENT_ATTR_EDIT && currentWorkMode != MapView.WorkMode.MODE_PATH_DELETE && currentWorkMode != MapView.WorkMode.MODE_PATH_DELETE_MULTIPLE && currentWorkMode != MapView.WorkMode.MODE_PATH_MERGE && currentWorkMode != MapView.WorkMode.MODE_PATH_CREATE) {
            return super.onTouchEvent(event)
        }

        val mapView = mapViewRef.get() ?: return true
        val screenPoint = PointF(event.x, event.y)
        val worldPoint = mapView.screenToWorld(screenPoint.x, screenPoint.y)

        // 创建路线模式下的处理
        if (currentWorkMode == MapView.WorkMode.MODE_PATH_CREATE) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 处理创建路线的点击事件
                    handleCreatePathDownEvent(worldPoint)
                    return true
                }

                MotionEvent.ACTION_UP -> {
                    // 检查是否是长按结束路径创建
                    if (event.eventTime - event.downTime > 500) {
                        // 长按超过500ms，结束路径创建
                        pathCreateStartNode = null
                        tempPath = null
                        invalidate()
                    }
                    return true
                }
            }
            return true
        }

        // 删除多条路线模式下的处理
        if (currentWorkMode == MapView.WorkMode.MODE_PATH_DELETE_MULTIPLE) {
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 开始框选
                    isBoxSelecting = true
                    boxSelectStartPoint = screenPoint
                    boxSelectEndPoint = screenPoint
                    return true
                }

                MotionEvent.ACTION_MOVE -> {
                    // 更新框选区域
                    if (isBoxSelecting) {
                        boxSelectEndPoint = screenPoint
                        // 根据框选区域更新选中的路线
                        updateBoxSelection()
                        invalidate()
                        return true
                    }
                }

                MotionEvent.ACTION_UP -> {
                    // 结束框选
                    if (isBoxSelecting) {
                        isBoxSelecting = false
                        // 根据最终框选区域更新选中的路线
                        updateBoxSelection()
                        // 如果有选中的路线，执行删除操作
                        if (selectedPathsForDeletion.isNotEmpty()) {
                            deleteSelectedPaths()
                        }
                        // 清空框选区域
                        boxSelectStartPoint = null
                        boxSelectEndPoint = null
                        invalidate()
                        return true
                    }
                }
            }
            return true
        }

        // 其他模式下的原有处理
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                // 尝试选择路段或节点
                handleDownEvent(screenPoint, worldPoint)
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                // 如果正在拖动节点或控制点，更新位置
                if ((draggingNode != null || draggingControlPoint != null) && selectedPath != null) {
                    handleMoveEvent(worldPoint)
                    return true
                }
            }

            MotionEvent.ACTION_UP -> {
                // 结束拖动
                draggingNode = null
                draggingControlPoint = null
                dragStartPoint = null
                return true
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 处理按下事件，选择路段、节点或控制点
     */
    private fun handleDownEvent(screenPoint: PointF, worldPoint: PointF) {
        val mapView = mapViewRef.get() ?: return
        val cLayer = this.cLayer ?: return

        if (cLayer.m_PathBase != null && cLayer.m_PathBase.m_pPathIdx != null) {
            for (i in 0 until cLayer.m_PathBase.m_uCount) {
                val path = cLayer.m_PathBase.m_pPathIdx[i].m_ptr
                val startNode = path.GetStartNode()
                val endNode = path.GetEndNode()

                // 检查路段点击
                if (startNode != null && endNode != null) {
                    val startScreen = mapView.worldToScreen(startNode.x, startNode.y)
                    val endScreen = mapView.worldToScreen(endNode.x, endNode.y)

                    if (pointToLineDistance(
                            screenPoint, startScreen, endScreen
                        ) <= SELECTION_RADIUS_PATH
                    ) {
                        draggingNode = null
                        draggingControlPoint = null
                        dragStartPoint = null

                        // 检查起点点击
                        val startScreenPoint = mapView.worldToScreen(startNode.x, startNode.y)
                        if (distanceBetweenPoints(
                                startScreenPoint, screenPoint
                            ) <= SELECTION_RADIUS_POINT
                        ) {
                            selectedPath = path

                            // 只有在路径编辑模式下允许拖动节点，节点属性编辑模式下只选中不允许拖动
                            if (currentWorkMode == MapView.WorkMode.MODE_PATH_EDIT) {
                                draggingNode = startNode
                                draggingControlPoint = null
                                dragStartPoint = screenPoint
                            } else if (currentWorkMode == MapView.WorkMode.MODE_PATH_MERGE) {
                                // 路线合并模式下，选择起点
                                if (selectedMergeStartNode == null) {
                                    // 还没有选择起点，选择当前起点
                                    selectedMergeStartNode = startNode
                                    selectedMergeStartPath = path
                                } else if (selectedMergeEndNode == null && path != selectedMergeStartPath) {
                                    // 已经选择了起点，现在选择终点（必须在不同路径上）
                                    selectedMergeEndNode = startNode
                                    // 起点和终点都已选择，执行连接
                                    connectSelectedNodes()
                                } else {
                                    // 重置选择，重新开始
                                    selectedMergeStartNode = startNode
                                    selectedMergeStartPath = path
                                    selectedMergeEndNode = null
                                }
                            }

                            // 触发节点选中回调（在路径编辑模式和节点属性编辑模式下都触发）
                            // 在节点属性编辑模式下，只有当点击的节点与当前选中的节点不同时才触发回调
                            if ((currentWorkMode == MapView.WorkMode.MODE_PATH_EDIT || currentWorkMode == MapView.WorkMode.MODE_PATH_NODE_ATTR_EDIT)) {

                                // 节点属性编辑模式下检查是否重复点击同一节点
                                if (currentWorkMode == MapView.WorkMode.MODE_PATH_NODE_ATTR_EDIT) {
                                    // 只有当点击的节点与当前选中的节点不同时，才触发回调
                                    if (selectedNode != startNode || selectedPath != path) {
                                        selectedNode = startNode
                                        onPathAttributeEditListener?.onNodeSelected(startNode, path)
                                    }
                                }
                                return
                            } else if (currentWorkMode == MapView.WorkMode.MODE_PATH_MERGE) {
                                invalidate()
                                return
                            }
                        }

                        // 检查终点点击
                        val endScreenPoint = mapView.worldToScreen(endNode.x, endNode.y)
                        if (distanceBetweenPoints(
                                endScreenPoint, screenPoint
                            ) <= SELECTION_RADIUS_POINT
                        ) {
                            selectedPath = path

                            // 只有在路径编辑模式下允许拖动节点，节点属性编辑模式下只选中不允许拖动
                            if (currentWorkMode == MapView.WorkMode.MODE_PATH_EDIT) {
                                draggingNode = endNode
                                draggingControlPoint = null
                                dragStartPoint = screenPoint
                            } else if (currentWorkMode == MapView.WorkMode.MODE_PATH_MERGE) {
                                // 路线合并模式下，选择终点
                                if (selectedMergeStartNode == null) {
                                    // 还没有选择起点，选择当前终点作为起点
                                    selectedMergeStartNode = endNode
                                    selectedMergeStartPath = path
                                } else if (selectedMergeEndNode == null && path != selectedMergeStartPath) {
                                    // 已经选择了起点，现在选择终点（必须在不同路径上）
                                    selectedMergeEndNode = endNode
                                    // 起点和终点都已选择，执行连接
                                    connectSelectedNodes()
                                } else {
                                    // 重置选择，重新开始
                                    selectedMergeStartNode = endNode
                                    selectedMergeStartPath = path
                                    selectedMergeEndNode = null
                                }
                            }

                            // 触发节点选中回调（在路径编辑模式和节点属性编辑模式下都触发）
                            // 在节点属性编辑模式下，只有当点击的节点与当前选中的节点不同时才触发回调
                            if ((currentWorkMode == MapView.WorkMode.MODE_PATH_EDIT || currentWorkMode == MapView.WorkMode.MODE_PATH_NODE_ATTR_EDIT)) {

                                // 节点属性编辑模式下检查是否重复点击同一节点
                                if (currentWorkMode == MapView.WorkMode.MODE_PATH_NODE_ATTR_EDIT) {
                                    // 只有当点击的节点与当前选中的节点不同时，才触发回调
                                    if (selectedNode != endNode || selectedPath != path) {
                                        selectedNode = endNode
                                        onPathAttributeEditListener?.onNodeSelected(endNode, path)
                                    }
                                }
                                return
                            } else if (currentWorkMode == MapView.WorkMode.MODE_PATH_MERGE) {
                                invalidate()
                                return
                            }
                        }

                        // 触发路段选中回调（在路径编辑模式、路段属性编辑模式和删除模式下都触发）
                        if (currentWorkMode == MapView.WorkMode.MODE_PATH_EDIT || currentWorkMode == MapView.WorkMode.MODE_PATH_SEGMENT_ATTR_EDIT || currentWorkMode == MapView.WorkMode.MODE_PATH_DELETE) {

                            // 路段属性编辑模式下检查是否重复点击同一路段
                            if (currentWorkMode == MapView.WorkMode.MODE_PATH_SEGMENT_ATTR_EDIT) {
                                // 只有当点击的路段与当前选中的路段不同时，才触发回调
                                if (selectedPath != path) {
                                    selectedPath = path
                                    onPathAttributeEditListener?.onPathSelected(path)
                                }
                            } else if (currentWorkMode == MapView.WorkMode.MODE_PATH_DELETE) {
                                // 删除模式下，点击路段直接删除
                                deletePath(path)
                            } else {
                                // 路径编辑模式下每次点击都触发回调
                                selectedPath = path
                            }
                            return
                        }

                        invalidate()
                        return
                    }
                }

                // 检查控制点是否被点击（仅对GenericPath有效）
                if (path is GenericPath && path.m_Curve != null) {
                    val bezier = path.m_Curve
                    if (bezier.m_ptKey != null) {
                        for (j in 0 until bezier.m_nCountKeyPoints) {
                            val controlPoint = bezier.m_ptKey[j]
                            val controlScreenPoint =
                                mapView.worldToScreen(controlPoint.x, controlPoint.y)
                            if (distanceBetweenPoints(
                                    controlScreenPoint, screenPoint
                                ) <= SELECTION_RADIUS_POINT
                            ) {
                                selectedPath = path
                                draggingControlPoint = controlPoint
                                draggingNode = null
                                dragStartPoint = screenPoint
                                invalidate()
                                return
                            }
                        }
                    }
                }
            }
        }

        // 如果没有选择到任何路段、节点或控制点，取消选择
        selectedPath = null
        selectedNode = null
        draggingNode = null
        draggingControlPoint = null
        dragStartPoint = null
        invalidate()
    }

    /**
     * 处理移动事件，更新拖动节点或控制点的位置
     */
    private fun handleMoveEvent(worldPoint: PointF) {

        // 更新节点位置 - 只有在路径编辑模式下才允许拖动节点
        if (draggingNode != null && currentWorkMode == MapView.WorkMode.MODE_PATH_EDIT) {

            // 直接访问节点的x和y属性，因为Node类继承自Point2d，x和y是public字段
            try {
                draggingNode!!.x = worldPoint.x
                draggingNode!!.y = worldPoint.y

                // 获取所有路段，检查并更新包含该节点的所有路段
                val cLayer = this.cLayer ?: return
                if (cLayer.m_PathBase != null && cLayer.m_PathBase.m_pPathIdx != null) {
                    for (i in 0 until cLayer.m_PathBase.m_uCount) {
                        val path = cLayer.m_PathBase.m_pPathIdx[i].m_ptr

                        // 如果是GenericPath类型且包含当前拖动的节点
                        if (path is GenericPath && path.m_Curve != null) {
                            val startNode = path.GetStartNode()
                            val endNode = path.GetEndNode()

                            // 检查当前拖动的节点是否是该路段的起点或终点
                            if (draggingNode == startNode || draggingNode == endNode) {
                                // 更新曲线的起点或终点
                                if (draggingNode == startNode) {
                                    // 更新曲线起点
                                    path.m_Curve.m_ptKey[0].x = worldPoint.x
                                    path.m_Curve.m_ptKey[0].y = worldPoint.y
                                    // 更新路径起点状态
                                    path.m_pstStart.x = worldPoint.x
                                    path.m_pstStart.y = worldPoint.y

                                } else if (draggingNode == endNode) {
                                    // 更新曲线终点
                                    path.m_Curve.m_ptKey[path.m_nCountCtrlPoints + 1].x =
                                        worldPoint.x
                                    path.m_Curve.m_ptKey[path.m_nCountCtrlPoints + 1].y =
                                        worldPoint.y
                                    // 更新路径终点状态
                                    path.m_pstEnd.x = worldPoint.x
                                    path.m_pstEnd.y = worldPoint.y
                                }

                                // 更新曲线参数
                                path.m_Curve?.CreateSamplePoints()
                                // 更新路径参数
                                path.ModifyParmByCurve()

                                // 记录调试信息
                                val curveLength = path.m_Curve?.m_fTotalLen ?: 0f
                                val pathLength = path.m_fSize
                                Log.d(
                                    "PathView2",
                                    "路段${i + 1} - 曲线长度: $curveLength, 路径长度: $pathLength"
                                )
                            }
                        }
                    }
                }

                invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        // 更新控制点位置
        else if (draggingControlPoint != null) {
            try {
                // 直接修改控制点的坐标（Point2d类有公共的x和y属性）
                draggingControlPoint!!.x = worldPoint.x
                draggingControlPoint!!.y = worldPoint.y

                // 如果控制点属于当前选中的GenericPath，更新曲线参数
                if (selectedPath is GenericPath) {
                    val genericPath = selectedPath as GenericPath
                    if (genericPath.m_Curve != null) {
                        // 更新曲线采样点和长度
                        genericPath.m_Curve?.CreateSamplePoints()
                        // 记录曲线总长度
                        val curveLength = genericPath.m_Curve?.m_fTotalLen ?: 0f
                        // 更新曲线参数
                        genericPath.ModifyParmByCurve()
                        // 记录路径长度
                        val pathLength = genericPath.m_fSize
                        // 验证长度是否一致
                        Log.d(
                            "PathView2",
                            "控制点拖动 - 曲线长度: $curveLength, 路径长度: $pathLength"
                        )
                    }
                }

                invalidate()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 计算两点之间的距离
     */
    private fun distanceBetweenPoints(p1: PointF, p2: PointF): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    /**
     * 计算点到线段的距离
     */
    private fun pointToLineDistance(point: PointF, lineStart: PointF, lineEnd: PointF): Float {
        val A = point.x - lineStart.x
        val B = point.y - lineStart.y
        val C = lineEnd.x - lineStart.x
        val D = lineEnd.y - lineStart.y

        val dot = A * C + B * D
        val lenSq = C * C + D * D
        var param = -1f

        if (lenSq != 0f) {
            param = dot / lenSq
        }

        val xx: Float
        val yy: Float

        if (param < 0f) {
            xx = lineStart.x
            yy = lineStart.y
        } else if (param > 1f) {
            xx = lineEnd.x
            yy = lineEnd.y
        } else {
            xx = lineStart.x + param * C
            yy = lineStart.y + param * D
        }

        val dx = point.x - xx
        val dy = point.y - yy
        return Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isDrawingEnabled) {
            val mapView = mapViewRef.get() ?: return

            cLayer?.let { it ->
                // 应用矩阵变换，确保拖动地图时路径跟随移动
                canvas.save()
                canvas.concat(mMatrix)

                if (it.m_PathBase != null && it.m_PathBase.m_MyNode != null) {

                    for (i in 0 until it.m_PathBase.m_uCount) {
                        if (it.m_PathBase.m_pPathIdx != null && it.m_PathBase.m_pPathIdx[i].m_ptr != null) {
                            val path = it.m_PathBase.m_pPathIdx[i].m_ptr

                            // 绘制节点（包含节点编号）
                            val startNode = path.GetStartNode()
                            val endNode = path.GetEndNode()

                            // 根据工作模式绘制不同效果
                            when (currentWorkMode) {
                                MapView.WorkMode.MODE_PATH_EDIT, MapView.WorkMode.MODE_PATH_NODE_ATTR_EDIT -> {
                                    // 编辑模式或节点属性编辑模式
                                    if (selectedPath == path) {
                                        // 绘制选中的路段
                                        path.Draw(mapView.mSrf, canvas, mSelectedPaint)
                                        // 绘制路段编号
                                        path.DrawID(mapView.mSrf, canvas, mPaint)

                                        // 绘制控制点（仅对GenericPath有效）
                                        if (path is GenericPath && currentWorkMode == MapView.WorkMode.MODE_PATH_EDIT) {
                                            // 使用库函数绘制控制点，区分拖动状态
                                            val ctrlColor =
                                                if (draggingControlPoint != null) Color.RED else Color.GREEN
                                            path.DrawCtrlPoints(
                                                mapView.mSrf, canvas, ctrlColor, 5, mPaint
                                            )
                                        }

                                        // 绘制开始节点（红色）
                                        startNode?.Draw(mapView.mSrf, canvas, Color.RED, 10, mPaint)
                                        // 绘制结束节点（红色）
                                        endNode?.Draw(mapView.mSrf, canvas, Color.RED, 10, mPaint)

                                    } else {
                                        // 未选中的路段，正常绘制
                                        path.Draw(mapView.mSrf, canvas, mPaint)
                                        // 在节点属性编辑模式下，显示所有路段的起点和终点
                                        if (currentWorkMode == MapView.WorkMode.MODE_PATH_NODE_ATTR_EDIT) {
                                            // 绘制开始节点（绿色）
                                            startNode?.Draw(
                                                mapView.mSrf, canvas, Color.GREEN, 8, mPaint
                                            )
                                            // 绘制结束节点（蓝色）
                                            endNode?.Draw(
                                                mapView.mSrf, canvas, Color.BLUE, 8, mPaint
                                            )
                                        }
                                    }
                                }

                                MapView.WorkMode.MODE_PATH_SEGMENT_ATTR_EDIT -> {
                                    // 路段属性编辑模式：显示所有路段编号
                                    path.Draw(
                                        mapView.mSrf,
                                        canvas,
                                        if (selectedPath == path) mSelectedPaint else mPaint
                                    )
                                    // 显示所有路段编号
                                    path.DrawID(mapView.mSrf, canvas, mPaint)
                                }

                                MapView.WorkMode.MODE_PATH_DELETE -> {
                                    // 删除模式：绘制所有路段，选中时显示红色高亮
                                    if (selectedPath == path) {
                                        // 绘制选中的路段（红色高亮）
                                        val deletePaint = Paint(mPaint).apply {
                                            color = Color.RED
                                            strokeWidth = 3f
                                        }
                                        path.Draw(mapView.mSrf, canvas, deletePaint)
                                        // 绘制路段编号
                                        path.DrawID(mapView.mSrf, canvas, mPaint)
                                        // 绘制节点（红色）
                                        startNode?.Draw(mapView.mSrf, canvas, Color.RED, 10, mPaint)
                                        endNode?.Draw(mapView.mSrf, canvas, Color.RED, 10, mPaint)
                                    } else {
                                        // 未选中的路段，正常绘制
                                        path.Draw(mapView.mSrf, canvas, mPaint)
                                        // 显示所有节点
                                        startNode?.Draw(
                                            mapView.mSrf, canvas, Color.GREEN, 8, mPaint
                                        )
                                        endNode?.Draw(mapView.mSrf, canvas, Color.BLUE, 8, mPaint)
                                    }
                                }

                                MapView.WorkMode.MODE_PATH_DELETE_MULTIPLE -> {
                                    // 删除多条路线模式：先正常绘制所有路线
                                    path.Draw(mapView.mSrf, canvas, mPaint)
                                }

                                MapView.WorkMode.MODE_PATH_MERGE -> {
                                    // 路线合并模式：正常绘制路段，放大显示选中的节点
                                    path.Draw(mapView.mSrf, canvas, mPaint)

                                    // 检查并绘制起点节点
                                    if (startNode != null) {
                                        if (startNode == selectedMergeStartNode) {
                                            // 选中的起点，放大显示（绿色）
                                            startNode.Draw(
                                                mapView.mSrf, canvas, Color.GREEN, 15, mPaint
                                            )
                                        } else if (startNode == selectedMergeEndNode) {
                                            // 选中的终点，放大显示（蓝色）
                                            startNode.Draw(
                                                mapView.mSrf, canvas, Color.BLUE, 15, mPaint
                                            )
                                        } else {
                                            // 未选中的起点，正常显示（绿色）
                                            startNode.Draw(
                                                mapView.mSrf, canvas, Color.GREEN, 8, mPaint
                                            )
                                        }
                                    }

                                    // 检查并绘制终点节点
                                    if (endNode != null) {
                                        if (endNode == selectedMergeStartNode) {
                                            // 选中的起点，放大显示（绿色）
                                            endNode.Draw(
                                                mapView.mSrf, canvas, Color.GREEN, 15, mPaint
                                            )
                                        } else if (endNode == selectedMergeEndNode) {
                                            // 选中的终点，放大显示（蓝色）
                                            endNode.Draw(
                                                mapView.mSrf, canvas, Color.BLUE, 15, mPaint
                                            )
                                        } else {
                                            // 未选中的终点，正常显示（蓝色）
                                            endNode.Draw(
                                                mapView.mSrf, canvas, Color.BLUE, 8, mPaint
                                            )
                                        }
                                    }
                                }

                                else -> {
                                    // 非编辑模式，正常绘制
                                    path.Draw(mapView.mSrf, canvas, mPaint)
//                                    // 绘制路段编号
//                                    path.DrawID(mapView.mSrf, canvas, mPaint)
//                                    // 绘制节点（红色）
//                                    startNode?.Draw(mapView.mSrf, canvas, Color.RED, 10, mPaint)
//                                    endNode?.Draw(mapView.mSrf, canvas, Color.RED, 10, mPaint)
                                }
                            }
                        }
                    }
                }
                // 恢复画布状态
                canvas.restore()

                // 在删除多条路线模式下，绘制选中的路线为红色
                if (currentWorkMode == MapView.WorkMode.MODE_PATH_DELETE_MULTIPLE) {
                    // 绘制所有选中的路线为红色（应用矩阵变换）
                    canvas.save()
                    canvas.concat(mMatrix)
                    for (path in selectedPathsForDeletion) {
                        path.Draw(mapView.mSrf, canvas, mRedPaint)
                    }
                    canvas.restore()

                    // 绘制框选区域（不应用矩阵变换，使用屏幕坐标直接绘制）
                    if (isBoxSelecting && boxSelectStartPoint != null && boxSelectEndPoint != null) {
                        val boxPaint = Paint().apply {
                            color = Color.YELLOW
                            strokeWidth = 1f
                            style = Paint.Style.STROKE
                            alpha = 150
                        }

                        val left = Math.min(boxSelectStartPoint!!.x, boxSelectEndPoint!!.x)
                        val top = Math.min(boxSelectStartPoint!!.y, boxSelectEndPoint!!.y)
                        val right = Math.max(boxSelectStartPoint!!.x, boxSelectEndPoint!!.x)
                        val bottom = Math.max(boxSelectStartPoint!!.y, boxSelectEndPoint!!.y)

                        // 绘制框选矩形
                        canvas.drawRect(left, top, right, bottom, boxPaint)

                        // 绘制半透明填充
                        val fillPaint = Paint().apply {
                            color = Color.YELLOW
                            style = Paint.Style.FILL
                            alpha = 50
                        }
                        canvas.drawRect(left, top, right, bottom, fillPaint)
                    }
                }

                // 在创建路线模式下，绘制临时的起点节点
                if (currentWorkMode == MapView.WorkMode.MODE_PATH_CREATE && pathCreateStartNode != null) {
                    val mapView = mapViewRef.get() ?: return
                    // 应用矩阵变换
                    canvas.save()
                    canvas.concat(mMatrix)
                    // 绘制起点节点（红色圆点，大小为12）
                    pathCreateStartNode!!.Draw(mapView.mSrf, canvas, Color.RED, 12, mPaint)
                    // 绘制节点编号
//                    pathCreateStartNode!!.DrawID(mapView.mSrf, canvas, mPaint)
                    canvas.restore()
                }
                // 在创建路线模式下，绘制临时曲线路段
                if (currentWorkMode == MapView.WorkMode.MODE_PATH_CREATE && tempPath != null) {
                    val mapView = mapViewRef.get() ?: return
                    // 应用矩阵变换
                    canvas.save()
                    canvas.concat(mMatrix)
                    // 绘制临时曲线路段
                    tempPath!!.Draw(mapView.mSrf, canvas, mRedPaint)
                    canvas.restore()
                }
            }
        }
    }

    /**
     * 连接选中的起点和终点
     */
    private fun connectSelectedNodes() {
        // 确保起点和终点都已选择，并且在不同的路径上
        if (selectedMergeStartNode == null || selectedMergeEndNode == null || selectedMergeStartPath == null) {
            return
        }

        val cLayer = this.cLayer ?: return
        val mapView = mapViewRef.get() ?: return

        // 创建一个新的路径来连接两个节点
        val newPath = cLayer.CreatePPLine(selectedMergeStartNode!!, selectedMergeEndNode!!)

        if (newPath != null) {
            // 触发路段创建回调
            onPathAttributeEditListener?.onPathCreated(newPath)

            // 重置选择状态
            selectedMergeStartNode = null
            selectedMergeStartPath = null
            selectedMergeEndNode = null

            // 重绘视图
            invalidate()
        }
    }

    /**
     * 设置是否启用绘制
     */
    fun setDrawingEnabled(enabled: Boolean) {
        this.isDrawingEnabled = enabled
        postInvalidate()
    }


    fun setLayer(cLayer: CLayer) {
        this.cLayer = cLayer
        postInvalidate()
    }

    /**
     * 获取当前的CLayer对象，用于保存路径数据
     */
    fun getLayer(): CLayer? {
        return cLayer
    }

    /**
     * 获取当前选中的路段
     */
    fun getSelectedPath(): Any? {
        return selectedPath
    }

    /**
     * 根据关键位姿创建示教路径（会直接生成地图）
     *
     * @param point2ds 路径关键点数组
     * @param m_KeyPst 路径的关键位姿信息
     * @param pathParam 路径参数
     * @param startNodeId 起点节点ID，如果为-1则创建新节点
     * @return 新创建的路径的终点节点ID，如果创建失败则返回-1
     */
    fun createTeachPath(point2ds: Array<Point2d>, m_KeyPst: DefPosture, pathParam: Short, startNodeId: Int = -1): Int {
        val startPst: Posture = m_KeyPst.m_PstV.get(0)
        val endPst: Posture = m_KeyPst.m_PstV.get(3)

        // 检查起点和终点是否重合，如果重合则不创建路径
        if (startPst.x == endPst.x && startPst.y == endPst.y) {
            return -1
        }

        // 设置控制点
        val pptCtrl = arrayOfNulls<Point2d>(2)
        pptCtrl[0] = point2ds[1]
        pptCtrl[1] = point2ds[2]

        val success = cLayer?.AddGenericPath_PPteach(
            startPst, endPst, pptCtrl, startNodeId, -1, pathParam
        )

        // 获取刚创建的路径的终点节点ID并返回
        var endNodeId = -1
        cLayer?.let {
            if (success == true && it.m_PathBase != null && it.m_PathBase.m_uCount > 0) {
                val newPath = it.m_PathBase.m_pPathIdx[it.m_PathBase.m_uCount - 1].m_ptr
                endNodeId = newPath.m_uEndNode
                // 可以在这里使用startNodeId和endNodeId
            }
        }
        return endNodeId
    }


    // 节点和路段属性编辑回调接口
    interface OnPathAttributeEditListener {
        // 当选中节点时触发
        fun onNodeSelected(node: Node, path: com.siasun.dianshi.bean.pp.world.Path)

        // 当选中路段时触发
        fun onPathSelected(path: com.siasun.dianshi.bean.pp.world.Path)

        // 当删除路段时触发
        fun onPathDeleted(path: com.siasun.dianshi.bean.pp.world.Path)

        // 当删除节点时触发
        fun onNodeDeleted(node: Node)

        // 当创建路段时触发
        fun onPathCreated(path: com.siasun.dianshi.bean.pp.world.Path) {}
    }

    // 回调监听器
    private var onPathAttributeEditListener: OnPathAttributeEditListener? = null

    // 设置回调监听器
    fun setOnPathAttributeEditListener(listener: OnPathAttributeEditListener) {
        this.onPathAttributeEditListener = listener
    }

    /**
     * 清除当前选择
     */
    private fun clearSelection() {
        selectedPath = null
        draggingNode = null
        draggingControlPoint = null
        dragStartPoint = null
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        clearSelection()
    }
}
