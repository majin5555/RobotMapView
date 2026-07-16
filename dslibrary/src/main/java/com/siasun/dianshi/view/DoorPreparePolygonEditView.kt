package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import com.siasun.dianshi.bean.DoorPrepareArea
import com.siasun.dianshi.bean.PointNew
import java.lang.ref.WeakReference

/**
 * 过门准备区
 */
@SuppressLint("ViewConstructor")
class DoorPreparePolygonEditView(context: Context?, parent: WeakReference<MapView>) :
    BasePolygonEditView<DoorPrepareArea>(context, parent) {

    // 编辑监听器 - 使用强引用确保回调能被触发
    private var onDoorPrepareAreaEditListener: OnDoorPrepareAreaEditListener? = null

    // ----------------- 实现父类抽象方法 -----------------

    override fun getAreaVertices(area: DoorPrepareArea): MutableList<PointNew> {
        return area.m_VertexPnt
    }

    override fun getAreaName(area: DoorPrepareArea): String {
        return area.sub_name ?: ""
    }

    override fun isEditMode(mode: WorkMode): Boolean {
        return mode == WorkMode.MODE_DOOR_PREPARE_AREA_EDIT || mode == WorkMode.MODE_DOOR_PREPARE_AREA_ADD
    }

    override fun handleDoubleTapSelectArea() {
        parentMapView.get()?.mPolygonEditViewPoint?.setWorkMode(WorkMode.MODE_SHOW_MAP)
        setWorkMode(WorkMode.MODE_DOOR_PREPARE_AREA_EDIT)
    }

    // ----------------- 路由回调给具体的Listener -----------------

    override fun onSelectedAreaChangedCallback(area: DoorPrepareArea?) {
        onDoorPrepareAreaEditListener?.onSelectedAreaChanged(area)
    }

    override fun onVertexDragStartCallback(area: DoorPrepareArea, vertexIndex: Int) {
        onDoorPrepareAreaEditListener?.onVertexDragStart(area, vertexIndex)
    }

    override fun onVertexDraggingCallback(area: DoorPrepareArea, vertexIndex: Int, newX: Float, newY: Float) {
        onDoorPrepareAreaEditListener?.onVertexDragging(area, vertexIndex, newX, newY)
    }

    override fun onVertexDragEndCallback(area: DoorPrepareArea, vertexIndex: Int, isInsideMap: Boolean) {
        onDoorPrepareAreaEditListener?.onVertexDragEnd(area, vertexIndex, isInsideMap)
    }

    override fun onVertexAddedCallback(area: DoorPrepareArea, vertexIndex: Int, x: Float, y: Float) {
        onDoorPrepareAreaEditListener?.onVertexAdded(area, vertexIndex, x, y)
    }

    override fun onEdgeRemovedCallback(area: DoorPrepareArea, edgeIndex: Int) {
        onDoorPrepareAreaEditListener?.onEdgeRemoved(area, edgeIndex)
    }

    override fun onVertexRemovedCallback(area: DoorPrepareArea, vertexIndex: Int) {
        onDoorPrepareAreaEditListener?.onVertexRemoved(area, vertexIndex)
    }

    override fun onAreaCreatedCallback(area: DoorPrepareArea) {
        onDoorPrepareAreaEditListener?.onAreaCreated(area)
    }

    override fun onAreaDragStartCallback(area: DoorPrepareArea) {
        onDoorPrepareAreaEditListener?.onAreaDragStart(area)
    }

    override fun onAreaDraggingCallback(area: DoorPrepareArea) {
        onDoorPrepareAreaEditListener?.onAreaDragging(area)
    }

    override fun onAreaDragEndCallback(area: DoorPrepareArea, isInsideMap: Boolean) {
        onDoorPrepareAreaEditListener?.onAreaDragEnd(area, isInsideMap)
    }

    override fun onAreaClickCallback(area: DoorPrepareArea) {
        onDoorPrepareAreaEditListener?.onAreaClick(area)
    }

    // ----------------- 对外暴露的方法 -----------------

    /**
     * 在地图中心创建一个矩形区域
     */
    fun createRectangularAreaAtCenter(newArea: DoorPrepareArea) {
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

        onDoorPrepareAreaEditListener?.onSelectedAreaChanged(selectedArea)
        onDoorPrepareAreaEditListener?.onAreaCreated(newArea)

        invalidate()
    }

    fun createAreaFromFlatPointsDouble(
        flatPoints: List<Double>,
        newArea: DoorPrepareArea,
    ) {
        createAreaFromFlatPoints(flatPoints.map { it.toFloat() }, newArea)
    }

    private fun createAreaFromFlatPoints(flatPoints: List<Float>, newArea: DoorPrepareArea) {
        if (flatPoints.size < 6 || flatPoints.size % 2 != 0) {
            // 至少3个点（6个值），且必须成对
            return
        }

        val points = mutableListOf<PointNew>()

        var i = 0
        while (i < flatPoints.size) {
            val x = flatPoints[i]
            val y = flatPoints[i + 1]
            points.add(PointNew(x, y))
            i += 2
        }

        // 填充数据
        newArea.m_VertexPnt.apply {
            clear()
            addAll(points)
        }

        synchronized(list) {
            list.add(newArea)
        }

        // 通知创建完成
        onDoorPrepareAreaEditListener?.onAreaCreated(newArea)

        invalidate()
    }

    /**
     * 设置编辑监听器
     */
    fun setOnDoorPrepareAreaEditListener(listener: OnDoorPrepareAreaEditListener?) {
        this.onDoorPrepareAreaEditListener = listener
    }

    /**
     * 设置要编辑的区域
     */
    fun setSelectedDoorPrepareArea(area: DoorPrepareArea?) {
        setSelectedAreaBase(area)
    }

    /**
     * 设置要绘制的区域数据
     */
    fun setDoorPrepareAreaData(data: MutableList<DoorPrepareArea>) {
        synchronized(list) {
            this.list.clear()
            this.list.addAll(data)
        }
        invalidate() // 触发重绘
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onDoorPrepareAreaEditListener = null
    }

    // 过门准备区编辑回调接口
    interface OnDoorPrepareAreaEditListener {
        // 选中区域变化
        fun onSelectedAreaChanged(area: DoorPrepareArea?) {}

        // 顶点开始拖动
        fun onVertexDragStart(area: DoorPrepareArea?, vertexIndex: Int) {}

        // 顶点拖动中
        fun onVertexDragging(area: DoorPrepareArea?, vertexIndex: Int, newX: Float, newY: Float) {}

        // 顶点拖动结束
        fun onVertexDragEnd(area: DoorPrepareArea?, vertexIndex: Int, isInsideMap: Boolean)

        // 添加了新顶点
        fun onVertexAdded(area: DoorPrepareArea?, vertexIndex: Int, x: Float, y: Float)

        // 删除了边
        fun onEdgeRemoved(area: DoorPrepareArea?, edgeIndex: Int)

        // 删除了顶点
        fun onVertexRemoved(area: DoorPrepareArea?, vertexIndex: Int)

        // 创建了新区域
        fun onAreaCreated(area: DoorPrepareArea?) {}

        // 区域开始拖动
        fun onAreaDragStart(area: DoorPrepareArea?) {}

        // 区域拖动中
        fun onAreaDragging(area: DoorPrepareArea?) {}

        // 区域拖动结束
        fun onAreaDragEnd(area: DoorPrepareArea?, isInsideMap: Boolean) {}

        // 点击区域
        fun onAreaClick(area: DoorPrepareArea?) {}
    }
}
