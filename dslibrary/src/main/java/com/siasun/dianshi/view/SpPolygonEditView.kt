package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.*
import com.siasun.dianshi.AreaType
import com.siasun.dianshi.bean.SpArea
import com.siasun.dianshi.bean.PointNew
import java.lang.ref.WeakReference

/**
 * 特殊区域
 *
 *      * 5-避障屏蔽区
 *      * 6-相机屏蔽区
 *      * 7-货位区
 *      * 8-感知特殊功能屏蔽与参数调整区
 *      * 9-声呐屏蔽区
 *      * 10-限制区
 */
@SuppressLint("ViewConstructor")
class SpPolygonEditView(context: Context?, parent: WeakReference<MapView>) :
    BasePolygonEditView<SpArea>(context, parent) {

    // 编辑监听器
    private var onSpAreaEditListener: OnSpAreaEditListener? = null

    // 根据特殊区域的 routeType 返回不同的画笔颜色
    private val spPaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    // ----------------- 实现父类抽象方法 -----------------

    override fun getAreaVertices(area: SpArea): MutableList<PointNew> {
        return area.m_VertexPnt
    }

    override fun getAreaName(area: SpArea): String {
        return area.sub_name ?: ""
    }

    override fun isEditMode(mode: WorkMode): Boolean {
        return mode == WorkMode.MODE_SP_AREA_ADD || mode == WorkMode.MODE_SP_AREA_EDIT
    }

    override fun handleDoubleTapSelectArea() {
        setWorkMode(WorkMode.MODE_SP_AREA_EDIT)
    }

    override fun getSpecificAreaPaint(area: SpArea): Paint {
        when (area.routeType) {
            AreaType.AREA_OBSTACLE_AVOIDANCE -> spPaint.color = Color.parseColor("#90FFFF00") // 黄色 避障屏蔽区
            AreaType.AREA_CAMERA_OFF -> spPaint.color = Color.parseColor("#9000FFFF") // 蓝绿色 相机屏蔽区
            AreaType.AREA_CARGO_LOCATION -> spPaint.color = Color.parseColor("#90FF00FF") // 洋红 货位区
            AreaType.AREA_MEMORY_BLOCKING -> spPaint.color = Color.parseColor("#90FFA500") // 橙色 记忆屏蔽区
            AreaType.AREA_SONAR_SHIELDING -> spPaint.color = Color.parseColor("#90800000") // 栗色 声呐屏蔽区
            AreaType.AREA_RESTRICTED -> spPaint.color = Color.parseColor("#90A52A2A") // 棕色 限制区
            else -> spPaint.color = Color.BLACK
        }
        return spPaint
    }

    // ----------------- 路由回调给具体的Listener -----------------

    override fun onSelectedAreaChangedCallback(area: SpArea?) {
        onSpAreaEditListener?.onSelectedAreaChanged(area)
    }

    override fun onVertexDragStartCallback(area: SpArea, vertexIndex: Int) {
        onSpAreaEditListener?.onVertexDragStart(area, vertexIndex)
    }

    override fun onVertexDraggingCallback(area: SpArea, vertexIndex: Int, newX: Float, newY: Float) {
        onSpAreaEditListener?.onVertexDragging(area, vertexIndex, newX, newY)
    }

    override fun onVertexDragEndCallback(area: SpArea, vertexIndex: Int, isInsideMap: Boolean) {
        onSpAreaEditListener?.onVertexDragEnd(area, vertexIndex, isInsideMap)
    }

    override fun onVertexAddedCallback(area: SpArea, vertexIndex: Int, x: Float, y: Float) {
        onSpAreaEditListener?.onVertexAdded(area, vertexIndex, x, y)
    }

    override fun onEdgeRemovedCallback(area: SpArea, edgeIndex: Int) {
        onSpAreaEditListener?.onEdgeRemoved(area, edgeIndex)
    }

    override fun onAreaCreatedCallback(area: SpArea) {
        onSpAreaEditListener?.onAreaCreated(area)
    }

    override fun onAreaDragStartCallback(area: SpArea) {
        onSpAreaEditListener?.onAreaDragStart(area)
    }

    override fun onAreaDraggingCallback(area: SpArea) {
        onSpAreaEditListener?.onAreaDragging(area)
    }

    override fun onAreaDragEndCallback(area: SpArea, isInsideMap: Boolean) {
        onSpAreaEditListener?.onAreaDragEnd(area)
    }

    // ----------------- 对外暴露的方法 -----------------

    /**
     * 在地图中心创建一个矩形区域
     */
    fun createRectangularAreaAtCenter(newArea: SpArea) {
        val mapView = parentMapView.get() ?: return
        val centerX = mapView.viewWidth / 2f
        val centerY = mapView.viewHeight / 2f

        val sizePx = 100f
        val halfSize = sizePx / 2f

        val topLeft = mapView.screenToWorld(centerX - halfSize, centerY - halfSize)
        val topRight = mapView.screenToWorld(centerX + halfSize, centerY - halfSize)
        val bottomRight = mapView.screenToWorld(centerX + halfSize, centerY + halfSize)
        val bottomLeft = mapView.screenToWorld(centerX - halfSize, centerY + halfSize)

        newArea.m_VertexPnt.apply {
            clear()
            add(PointNew(topLeft.x, topLeft.y))
            add(PointNew(topRight.x, topRight.y))
            add(PointNew(bottomRight.x, bottomRight.y))
            add(PointNew(bottomLeft.x, bottomLeft.y))
        }

        list.add(newArea)
        selectedArea = newArea

        onSpAreaEditListener?.onSelectedAreaChanged(newArea)
        onSpAreaEditListener?.onAreaCreated(newArea)

        invalidate()
    }

    /**
     * 设置编辑监听器
     */
    fun setOnSpAreaEditListener(listener: OnSpAreaEditListener?) {
        this.onSpAreaEditListener = listener
    }

    /**
     * 设置要编辑的区域
     */
    fun setSelectedArea(area: SpArea?) {
        setSelectedAreaBase(area)
    }

    /**
     * 设置要绘制的区域数据
     */
    fun setSpAreaData(data: MutableList<SpArea>) {
        synchronized(list) {
            this.list.clear()
            this.list.addAll(data)
        }
        invalidate() // 触发重绘
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onSpAreaEditListener = null
    }

    // 清扫区域编辑回调接口
    interface OnSpAreaEditListener {
        // 选中区域变化
        fun onSelectedAreaChanged(area: SpArea?) {}

        // 顶点开始拖动
        fun onVertexDragStart(area: SpArea, vertexIndex: Int) {}

        // 顶点拖动中
        fun onVertexDragging(area: SpArea, vertexIndex: Int, newX: Float, newY: Float) {}

        // 顶点拖动结束
        fun onVertexDragEnd(area: SpArea, vertexIndex: Int, isInsideMap: Boolean)

        // 添加了新顶点
        fun onVertexAdded(area: SpArea, vertexIndex: Int, x: Float, y: Float)

        // 删除了边
        fun onEdgeRemoved(area: SpArea, edgeIndex: Int)

        // 创建了新区域
        fun onAreaCreated(area: SpArea) {}

        // 区域开始拖动
        fun onAreaDragStart(area: SpArea) {}

        // 区域拖动中
        fun onAreaDragging(area: SpArea) {}

        // 区域拖动结束
        fun onAreaDragEnd(area: SpArea) {}
    }
}
