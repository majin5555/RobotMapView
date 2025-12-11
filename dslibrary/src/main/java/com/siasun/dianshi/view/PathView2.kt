package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.Log
import android.view.MotionEvent
import com.siasun.dianshi.bean.LineNew
import com.siasun.dianshi.bean.Point2d
import com.siasun.dianshi.bean.PointNew
import com.siasun.dianshi.bean.TeachPoint
import com.siasun.dianshi.bean.pp.Bezier
import com.siasun.dianshi.bean.pp.PathPlanResultBean
import com.siasun.dianshi.bean.pp.world.CLayer
import com.siasun.dianshi.bean.pp.world.GenericPath
import com.siasun.dianshi.bean.pp.world.Node
import java.lang.ref.WeakReference
import kotlin.math.sqrt

/**
 * 路线
 */
@SuppressLint("ViewConstructor")
class PathView2 @SuppressLint("ViewConstructor") constructor(
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
    private val SELECTION_RADIUS = 30f // 路段/节点选择半径
    
    // 路线合并模式相关属性
    private var selectedMergeStartNode: Node? = null // 合并路线的起点
    private var selectedMergeStartPath: com.siasun.dianshi.bean.pp.world.Path? = null // 起点所在的路径
    private var selectedMergeEndNode: Node? = null // 合并路线的终点
    private var selectedMergeEndPath: com.siasun.dianshi.bean.pp.world.Path? = null // 终点所在的路径
    
    // 删除多条路线模式相关属性
    private var isBoxSelecting: Boolean = false // 是否正在进行框选
    private var boxSelectStartPoint: PointF? = null // 框选起点
    private var boxSelectEndPoint: PointF? = null // 框选终点
    private val selectedPathsForDeletion: MutableSet<com.siasun.dianshi.bean.pp.world.Path> = mutableSetOf() // 选中待删除的路线集合
    private val redPaint: Paint by lazy { // 红色画笔，用于标记选中的路线
        Paint().apply {
            color = Color.RED
            strokeWidth = 3f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
    }

    // 编辑模式下的特殊画笔
    private val mSelectedPaint: Paint by lazy {
        Paint().apply {
            color = Color.GREEN
            strokeWidth = 3f
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
    }

    private val mDraggingPaint: Paint by lazy {
        Paint().apply {
            color = Color.RED
            strokeWidth = 2f
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    // 专用画笔：起点蓝色、终点黑色、控制点绿色
    private val mStartPointPaint: Paint by lazy {
        Paint().apply {
            color = Color.BLUE
            strokeWidth = 2f
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    private val mEndPointPaint: Paint by lazy {
        Paint().apply {
            color = Color.BLACK
            strokeWidth = 2f
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }

    private val mControlPointPaint: Paint by lazy {
        Paint().apply {
            color = Color.GREEN
            strokeWidth = 1f
            isAntiAlias = true
            style = Paint.Style.FILL
        }
    }


    // 优化：使用伴生对象创建静态Paint实例，避免重复创建
    companion object {
        private val mRedPaint: Paint by lazy {
            Paint().apply {
                color = Color.RED
                strokeWidth = 1f
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val mGreenPaint: Paint by lazy {
            Paint().apply {
                color = Color.GREEN // 修复：将颜色从RED改为GREEN
                strokeWidth = 1f
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val mTeachPaint: Paint by lazy {
            Paint().apply {
                color = Color.GREEN
                strokeWidth = 1f
                isAntiAlias = true
                style = Paint.Style.FILL
            }
        }
        private val mLinePaint: Paint by lazy {
            Paint().apply {
                color = Color.BLACK
                strokeWidth = 1f
                isAntiAlias = true
                style = Paint.Style.STROKE
            }
        }
        private val mBezierPaint: Paint by lazy {
            Paint().apply {
                color = Color.BLACK
                strokeWidth = 1f
                isAntiAlias = true
                style = Paint.Style.STROKE
            }
        }

        private val mPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = Color.BLACK
        }

        // 采样率，减少绘制点数以提高性能
        private const val SAMPLE_RATE = 1
    }

    //试教中的绿色的点的集合 - 使用同步集合确保线程安全
    private val teachPointList = mutableListOf<TeachPoint>()
    private var mCleanPathPlanResultBean: PathPlanResultBean? = null // 清扫路径规划结果
    private var mGlobalPathPlanResultBean: PathPlanResultBean? = null //全局路径规划结果


    // 优化：创建可复用的Path对象，避免在onDraw中频繁创建
    private val bezierPath = Path()

    // 保存parent引用以便安全访问
    private val mapViewRef: WeakReference<MapView> = parent

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
        fun onPathCreated(path: com.siasun.dianshi.bean.pp.world.Path){}
    }

    // 回调监听器
    private var onPathAttributeEditListener: OnPathAttributeEditListener? = null

    // 设置回调监听器
    fun setOnPathAttributeEditListener(listener: OnPathAttributeEditListener) {
        this.onPathAttributeEditListener = listener
    }

    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: MapView.WorkMode) {
        currentWorkMode = mode
        // 如果退出路径编辑模式、节点属性编辑模式、路段属性编辑模式和删除模式，重置选择状态
        if (mode != MapView.WorkMode.MODE_PATH_EDIT && 
            mode != MapView.WorkMode.MODE_PATH_NODE_ATTR_EDIT &&
            mode != MapView.WorkMode.MODE_PATH_SEGMENT_ATTR_EDIT &&
            mode != MapView.WorkMode.MODE_PATH_DELETE &&
            mode != MapView.WorkMode.MODE_PATH_DELETE_MULTIPLE &&
            mode != MapView.WorkMode.MODE_PATH_MERGE) {
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
     * 根据框选区域更新选中的路线集合
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
    private fun isPathInBox(path: com.siasun.dianshi.bean.pp.world.Path, minX: Float, minY: Float, maxX: Float, maxY: Float): Boolean {
        val mapView = mapViewRef.get() ?: return false
        val startNode = path.GetStartNode()
        val endNode = path.GetEndNode()
        
        if (startNode != null && endNode != null) {
            // 将路线的起点和终点转换为屏幕坐标
            val startScreen = mapView.worldToScreen(startNode.x, startNode.y)
            val endScreen = mapView.worldToScreen(endNode.x, endNode.y)
            
            // 检查起点或终点是否在框选区域内
            if ((startScreen.x >= minX && startScreen.x <= maxX && startScreen.y >= minY && startScreen.y <= maxY) ||
                (endScreen.x >= minX && endScreen.x <= maxX && endScreen.y >= minY && endScreen.y <= maxY)) {
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
        // 在路径编辑模式、节点属性编辑模式、路段属性编辑模式和删除模式下都处理触摸事件
        if (currentWorkMode != MapView.WorkMode.MODE_PATH_EDIT && 
            currentWorkMode != MapView.WorkMode.MODE_PATH_NODE_ATTR_EDIT &&
            currentWorkMode != MapView.WorkMode.MODE_PATH_SEGMENT_ATTR_EDIT &&
            currentWorkMode != MapView.WorkMode.MODE_PATH_DELETE &&
            currentWorkMode != MapView.WorkMode.MODE_PATH_DELETE_MULTIPLE &&
            currentWorkMode != MapView.WorkMode.MODE_PATH_MERGE) {
            return super.onTouchEvent(event)
        }

        val mapView = mapViewRef.get() ?: return true
        val screenPoint = PointF(event.x, event.y)
        val worldPoint = mapView.screenToWorld(screenPoint.x, screenPoint.y)

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
                            screenPoint,
                            startScreen,
                            endScreen
                        ) <= SELECTION_RADIUS
                    ) {
                        draggingNode = null
                        draggingControlPoint = null
                        dragStartPoint = null

                        // 检查起点点击
                        val startScreenPoint = mapView.worldToScreen(startNode.x, startNode.y)
                        if (distanceBetweenPoints(
                                startScreenPoint,
                                screenPoint
                            ) <= SELECTION_RADIUS
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
                            if ((currentWorkMode == MapView.WorkMode.MODE_PATH_EDIT || 
                                currentWorkMode == MapView.WorkMode.MODE_PATH_NODE_ATTR_EDIT)) {
                                
                                // 节点属性编辑模式下检查是否重复点击同一节点
                                if (currentWorkMode == MapView.WorkMode.MODE_PATH_NODE_ATTR_EDIT) {
                                    // 只有当点击的节点与当前选中的节点不同时，才触发回调
                                    if (selectedNode != startNode || selectedPath != path) {
                                        selectedNode = startNode
                                        onPathAttributeEditListener?.onNodeSelected(startNode, path)
                                    }
                                } else {
                                    // 路径编辑模式下每次点击都触发回调
                                    onPathAttributeEditListener?.onNodeSelected(startNode, path)
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
                                endScreenPoint,
                                screenPoint
                            ) <= SELECTION_RADIUS
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
                            if ((currentWorkMode == MapView.WorkMode.MODE_PATH_EDIT || 
                                currentWorkMode == MapView.WorkMode.MODE_PATH_NODE_ATTR_EDIT)) {
                                
                                // 节点属性编辑模式下检查是否重复点击同一节点
                                if (currentWorkMode == MapView.WorkMode.MODE_PATH_NODE_ATTR_EDIT) {
                                    // 只有当点击的节点与当前选中的节点不同时，才触发回调
                                    if (selectedNode != endNode || selectedPath != path) {
                                        selectedNode = endNode
                                        onPathAttributeEditListener?.onNodeSelected(endNode, path)
                                    }
                                } else {
                                    // 路径编辑模式下每次点击都触发回调
                                    onPathAttributeEditListener?.onNodeSelected(endNode, path)
                                }
                                return
                            } else if (currentWorkMode == MapView.WorkMode.MODE_PATH_MERGE) {
                                invalidate()
                                return
                            }
                        }

                        // 触发路段选中回调（在路径编辑模式、路段属性编辑模式和删除模式下都触发）
                        if (currentWorkMode == MapView.WorkMode.MODE_PATH_EDIT || 
                            currentWorkMode == MapView.WorkMode.MODE_PATH_SEGMENT_ATTR_EDIT ||
                            currentWorkMode == MapView.WorkMode.MODE_PATH_DELETE) {
                            
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
                                onPathAttributeEditListener?.onPathSelected(path)
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
                                    controlScreenPoint,
                                    screenPoint
                                ) <= SELECTION_RADIUS
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
//
//            // 绘制试教中的点 - 使用副本避免并发修改
//            val pointListCopy = synchronized(teachPointList) {
//                teachPointList.toList()
//            }
//            for (point in pointListCopy) {
//                drawTeachPointIng(canvas, point)
//            }
//
//            // 绘制清扫路线
//            mCleanPathPlanResultBean?.let { cleanPath ->
//                // 采样绘制直线
//                for (i in cleanPath.m_vecLineOfPathPlan.indices step SAMPLE_RATE) {
//                    drawPPLinePath(canvas, cleanPath.m_vecLineOfPathPlan[i])
//                }
//                // 采样绘制贝塞尔曲线
//                for (i in cleanPath.m_vecBezierOfPathPlan.indices step SAMPLE_RATE) {
//                    drawPPBezierPath(canvas, cleanPath.m_vecBezierOfPathPlan[i])
//                }
//            }
//
//            // 绘制全局路径
//            mGlobalPathPlanResultBean?.let { globalPath ->
//                // 采样绘制直线
//                for (i in globalPath.m_vecLineOfPathPlan.indices step SAMPLE_RATE) {
//                    drawPPLinePath(canvas, globalPath.m_vecLineOfPathPlan[i])
//                }
//                // 采样绘制贝塞尔曲线
//                for (i in globalPath.m_vecBezierOfPathPlan.indices step SAMPLE_RATE) {
//                    drawPPBezierPath(canvas, globalPath.m_vecBezierOfPathPlan[i])
//                }
//
//                // 创建世界系坐标点
//                if (globalPath.startPoint != null && globalPath.startPoint.size >= 3 &&
//                    globalPath.endPoint != null && globalPath.endPoint.size >= 3
//                ) {
//                    val startPoint2d = PointNew(globalPath.startPoint[0], globalPath.startPoint[1])
//                    val endPoint2d = PointNew(globalPath.endPoint[0], globalPath.endPoint[1])
//
//                    drawStartAndEndPoint(
//                        canvas,
//                        startPoint2d,
//                        endPoint2d,
//                        startPointText,
//                        endPointText
//                    )
//                }
//            }
            val mapView = mapViewRef.get() ?: return

            cLayer?.let { it ->
                // 应用矩阵变换，确保拖动地图时路径跟随移动
                canvas.save()
                canvas.concat(mMatrix)

                //绘制world_pad.dat文件路线
//                it.Draw(canvas, mapView.mSrf, mPaint)
                if (it.m_PathBase != null && it.m_PathBase.m_MyNode != null) {
//                 it.m_PathBase.Draw(mapView.mSrf, canvas, mPaint)

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
                                            startNode?.Draw(mapView.mSrf, canvas, Color.GREEN, 8, mPaint)
                                            // 绘制结束节点（蓝色）
                                            endNode?.Draw(mapView.mSrf, canvas, Color.BLUE, 8, mPaint)
                                        }
                                    }
                                }
                                MapView.WorkMode.MODE_PATH_SEGMENT_ATTR_EDIT -> {
                                    // 路段属性编辑模式：显示所有路段编号
                                    path.Draw(mapView.mSrf, canvas, if (selectedPath == path) mSelectedPaint else mPaint)
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
                                        startNode?.Draw(mapView.mSrf, canvas, Color.GREEN, 8, mPaint)
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
                                            startNode.Draw(mapView.mSrf, canvas, Color.GREEN, 15, mPaint)
                                        } else if (startNode == selectedMergeEndNode) {
                                            // 选中的终点，放大显示（蓝色）
                                            startNode.Draw(mapView.mSrf, canvas, Color.BLUE, 15, mPaint)
                                        } else {
                                            // 未选中的起点，正常显示（绿色）
                                            startNode.Draw(mapView.mSrf, canvas, Color.GREEN, 8, mPaint)
                                        }
                                    }
                                    
                                    // 检查并绘制终点节点
                                    if (endNode != null) {
                                        if (endNode == selectedMergeStartNode) {
                                            // 选中的起点，放大显示（绿色）
                                            endNode.Draw(mapView.mSrf, canvas, Color.GREEN, 15, mPaint)
                                        } else if (endNode == selectedMergeEndNode) {
                                            // 选中的终点，放大显示（蓝色）
                                            endNode.Draw(mapView.mSrf, canvas, Color.BLUE, 15, mPaint)
                                        } else {
                                            // 未选中的终点，正常显示（蓝色）
                                            endNode.Draw(mapView.mSrf, canvas, Color.BLUE, 8, mPaint)
                                        }
                                    }
                                }
                                else -> {
                                    // 非编辑模式，正常绘制
                                    path.Draw(mapView.mSrf, canvas, mPaint)
                                }
                            }
                        }
                    }
                }

//                for (i in 0 until m_uCount) {
//                    m_pPathIdx.get(i).m_ptr.DrawID(ScrnRef, Grp, paint)
//                }

//                    mRouteEdit.m_KeyPst.Draw(mapView.mSrf, canvas, mPaint)
//
//                    //重点显示要编辑的路径
//                    for (i in mRouteEdit.m_nCurPathIndex.indices) {
//                        val pPath = it.m_PathBase.m_pPathIdx[mRouteEdit.m_nCurPathIndex[i]].m_ptr
//                        if (pPath != null) {
//                            //避免删除节点，会引起删除线，需要进行是否为空的判断
//                            pPath.Draw(mapView.mSrf, canvas, Color.GREEN, 3)
//                            // 修改操作下显示控制点
//                            if (pPath.m_uType.toInt() == 10 && mRouteEdit.mEditWorldStage == mRouteEdit.WRD_MOD_NODE) {
//                                (pPath as GenericPath).DrawCtrlPoints(
//                                    mapView.mSrf,
//                                    canvas,
//                                    null,
//                                    Color.GREEN,
//                                    5
//                                )
//                                if (mRouteEdit.mCurKeyId > 0) {
//                                    (pPath).m_Curve.m_ptKey[mRouteEdit.mCurKeyId - 1].Draw(
//                                        mapView.mSrf, canvas, Color.RED, 8
//                                    )
//                                }
//                            }
//                            //在"GetStartNode"这里会空
//                            val tempStart = pPath.GetStartNode()
//                            val tempEnd = pPath.GetEndNode()
//                            if (tempStart == null || tempEnd == null) {
//                                continue
//                            }
//                            pPath.GetStartNode().Draw(mapView.mSrf, canvas, Color.GREEN)
//                            tempEnd.Draw(mapView.mSrf, canvas, Color.GREEN)
//                        }
//                    }
//
//                    //显示选择的节点
//                    if (mRouteEdit.mCurNodeId != -1) {
//                        val node = it.GetNode(mRouteEdit.mCurNodeId)
//                        // 修改操作下显示带位子的点
//                        if (node != null && mRouteEdit.mEditWorldStage == mRouteEdit.WRD_MOD_NODE) {
//                            //将节点的姿态绘制出来。便于修改角度
//                            mRouteEdit.m_ModNodePos.Clear()
//                            val pst = Posture()
//                            pst.x = node.x
//                            pst.y = node.y
//                            val mAngles = arrayOfNulls<Angle>(4)
//                            val nCount = it.GetNodeHeadingAngle(mRouteEdit.mCurNodeId, mAngles, 4)
//                            if (nCount > 0) {
//                                pst.fThita = mAngles[0]!!.m_fRad
//                            }
//                            mRouteEdit.m_ModNodePos.AddPst(pst)
//                            mRouteEdit.m_ModNodePos.m_SelectPstID = 0 //默认被选中
//                            mRouteEdit.m_ModNodePos.Draw(mapView.mSrf, canvas, mPaint)
//                        }
//                        node?.Draw(mapView.mSrf, canvas, Color.RED)
//                    }
//                    if (mRouteEdit.m_RegConDownCount == 1 && mRouteEdit.mEditWorldStage == mRouteEdit.WRD_ADD_REG_CON) {
//                        mRouteEdit.m_RegConStart.Draw(mapView.mSrf, canvas, Color.BLUE, 5) //color
//                    }

                // 恢复画布状态
                canvas.restore()

                // 在删除多条路线模式下，绘制选中的路线为红色
                if (currentWorkMode == MapView.WorkMode.MODE_PATH_DELETE_MULTIPLE) {
                    // 绘制所有选中的路线为红色（应用矩阵变换）
                    canvas.save()
                    canvas.concat(mMatrix)
                    for (path in selectedPathsForDeletion) {
                        path.Draw(mapView.mSrf, canvas, redPaint)
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
            }
        }
    }

    /**
     * 绘制示教点
     *
     */
    private fun drawTeachPointIng(canvas: Canvas, point: TeachPoint) {
        val mapView = mParent.get() ?: return
        // 避免重复创建PointF对象
        val pnt = mapView.worldToScreen(point.x.toFloat(), point.y.toFloat())
        drawCircle(canvas, pnt, 10f, mTeachPaint)
    }

    /**
     * 绘制直线
     */
    private fun drawPPLinePath(canvas: Canvas, line: LineNew) {
        val mapView = mParent.get() ?: return
        val startPoint = mapView.worldToScreen(line.ptStart.X, line.ptStart.Y)
        val endPoint = mapView.worldToScreen(line.ptEnd.X, line.ptEnd.Y)
        drawLine(canvas, startPoint, endPoint, mLinePaint)
    }

    /**
     * 绘制曲线
     */
    private fun drawPPBezierPath(canvas: Canvas, bezier: Bezier) {
        val mapView = mParent.get() ?: return

        // 确保贝塞尔曲线有足够的控制点
        if (bezier.m_ptKey.size < 4) return

        // 复用Path对象，避免每次绘制都创建新的Path
        bezierPath.reset()
        val mStart = mapView.worldToScreen(bezier.m_ptKey[0].x, bezier.m_ptKey[0].y)
        val mControl1 = mapView.worldToScreen(bezier.m_ptKey[1].x, bezier.m_ptKey[1].y)
        val mControl2 = mapView.worldToScreen(bezier.m_ptKey[2].x, bezier.m_ptKey[2].y)
        val mEnd = mapView.worldToScreen(bezier.m_ptKey[3].x, bezier.m_ptKey[3].y)
        bezierPath.moveTo(mStart.x, mStart.y)
        bezierPath.cubicTo(mControl1.x, mControl1.y, mControl2.x, mControl2.y, mEnd.x, mEnd.y)
        drawPath(canvas, bezierPath, mBezierPaint)
    }

    /**
     * @description 绘制路径起点与终点
     * @author CheFuX1n9
     * @since 2024/5/20 10:16
     */
    private fun drawStartAndEndPoint(
        canvas: Canvas,
        startPoint2d: PointNew,
        endPoint2d: PointNew,
        startPointName: String?,
        endPointName: String?
    ) {
        val mapView = mParent.get() ?: return
        // 世界系坐标点转换屏幕像素点
        val startPoint = mapView.worldToScreen(startPoint2d.X, startPoint2d.Y)
        val endPoint = mapView.worldToScreen(endPoint2d.X, endPoint2d.Y)

        drawCircle(canvas, startPoint, 10f, mGreenPaint)
        // 添加空值检查，避免空指针异常
        startPointName?.let { drawLabel(canvas, it, startPoint, mGreenPaint) }
        drawCircle(canvas, endPoint, 10f, mRedPaint)
        endPointName?.let { drawLabel(canvas, it, endPoint, mRedPaint) }
    }

    /**
     * 外部接口: 设置试教点 试教中
     */
    fun setTeachPoint(point: TeachPoint) {
        synchronized(teachPointList) {
            teachPointList.add(point)
        }
        invalidate()
    }

    /**
     * 设置清扫路线数据
     */
    fun setCleanPathPlanResultBean(pathPlanResultBean: PathPlanResultBean?) {
        mCleanPathPlanResultBean = pathPlanResultBean
        invalidate()
    }

    fun setGlobalPathPlanResultBean(pathPlanResultBean: PathPlanResultBean?) {
        mGlobalPathPlanResultBean = pathPlanResultBean
        invalidate()
    }

    /**
     * 外部接口: 设置试教点 清除
     */
    fun clearTeachPoint() {
        synchronized(teachPointList) {
            teachPointList.clear()
        }
        invalidate()
    }

    fun clearPathPlan() {
        setGlobalPathPlanResultBean(null)
        setCleanPathPlanResultBean(null)
        invalidate()
    }

    /**
     * 连接选中的起点和终点
     */
    private fun connectSelectedNodes() {
        // 确保起点和终点都已选择，并且在不同的路径上
        if (selectedMergeStartNode == null || selectedMergeEndNode == null || 
            selectedMergeStartPath == null) {
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

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        // 清理资源，防止内存泄漏
        clearTeachPoint()
        clearPathPlan()
        mCleanPathPlanResultBean = null
        mGlobalPathPlanResultBean = null
        draggingControlPoint = null
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
     * 清除当前选择
     */
    fun clearSelection() {
        selectedPath = null
        draggingNode = null
        draggingControlPoint = null
        dragStartPoint = null
        invalidate()
    }
}
