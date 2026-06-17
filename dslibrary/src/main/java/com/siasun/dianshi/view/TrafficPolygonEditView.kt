package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import com.siasun.dianshi.bean.TrafficArea
import com.siasun.dianshi.bean.PointNew
import java.lang.ref.WeakReference

/**
 * 交管区域
 */
@SuppressLint("ViewConstructor")
class TrafficPolygonEditView(context: Context?, parent: WeakReference<MapView>) :
    BasePolygonEditView<TrafficArea>(context, parent) {

    // 编辑监听器 - 使用强引用确保回调能被触发
    private var onTrafficAreaEditListener: OnTrafficAreaEditListener? = null

    // ----------------- 实现父类抽象方法 -----------------

    override fun getAreaVertices(area: TrafficArea): MutableList<PointNew> {
        return area.areaVertexPnt
    }

    override fun getAreaName(area: TrafficArea): String {
        return area.name ?: ""
    }

    override fun isEditMode(mode: WorkMode): Boolean {
        return mode == WorkMode.MODE_TRAFFIC_AREA_EDIT || mode == WorkMode.MODE_TRAFFIC_AREA_ADD
    }

    override fun handleDoubleTapSelectArea() {
        parentMapView.get()?.mPolygonEditViewPoint?.setWorkMode(WorkMode.MODE_SHOW_MAP)
        setWorkMode(WorkMode.MODE_TRAFFIC_AREA_EDIT)
    }

    // ----------------- 路由回调给具体的Listener -----------------

    override fun onSelectedAreaChangedCallback(area: TrafficArea?) {
        onTrafficAreaEditListener?.onSelectedAreaChanged(area)
    }

    override fun onVertexDragStartCallback(area: TrafficArea, vertexIndex: Int) {
        onTrafficAreaEditListener?.onVertexDragStart(area, vertexIndex)
    }

    override fun onVertexDraggingCallback(area: TrafficArea, vertexIndex: Int, newX: Float, newY: Float) {
        onTrafficAreaEditListener?.onVertexDragging(area, vertexIndex, newX, newY)
    }

    override fun onVertexDragEndCallback(area: TrafficArea, vertexIndex: Int, isInsideMap: Boolean) {
        onTrafficAreaEditListener?.onVertexDragEnd(area, vertexIndex, isInsideMap)
    }

    override fun onVertexAddedCallback(area: TrafficArea, vertexIndex: Int, x: Float, y: Float) {
        onTrafficAreaEditListener?.onVertexAdded(area, vertexIndex, x, y)
    }

    override fun onEdgeRemovedCallback(area: TrafficArea, edgeIndex: Int) {
        onTrafficAreaEditListener?.onEdgeRemoved(area, edgeIndex)
    }

    override fun onVertexRemovedCallback(area: TrafficArea, vertexIndex: Int) {
        onTrafficAreaEditListener?.onVertexRemoved(area, vertexIndex)
    }

    override fun onAreaCreatedCallback(area: TrafficArea) {
        onTrafficAreaEditListener?.onAreaCreated(area)
    }

    override fun onAreaDragStartCallback(area: TrafficArea) {
        onTrafficAreaEditListener?.onAreaDragStart(area)
    }

    override fun onAreaDraggingCallback(area: TrafficArea) {
        onTrafficAreaEditListener?.onAreaDragging(area)
    }

    override fun onAreaDragEndCallback(area: TrafficArea, isInsideMap: Boolean) {
        onTrafficAreaEditListener?.onAreaDragEnd(area, isInsideMap)
    }

    override fun onAreaClickCallback(area: TrafficArea) {
        onTrafficAreaEditListener?.onAreaClick(area)
    }

    // ----------------- 对外暴露的方法 -----------------

    /**
     * 在地图中心创建一个矩形区域
     */
    fun createRectangularAreaAtCenter(newArea: TrafficArea) {
        val mapView = parentMapView.get() ?: return
        val centerX = mapView.viewWidth / 2f
        val centerY = mapView.viewHeight / 2f

        val sizePx = 100f
        val halfSize = sizePx / 2f

        val topLeft = mapView.screenToWorld(centerX - halfSize, centerY - halfSize)
        val topRight = mapView.screenToWorld(centerX + halfSize, centerY - halfSize)
        val bottomRight = mapView.screenToWorld(centerX + halfSize, centerY + halfSize)
        val bottomLeft = mapView.screenToWorld(centerX - halfSize, centerY + halfSize)

        newArea.areaVertexPnt.apply {
            clear()
            add(PointNew(topLeft.x, topLeft.y))
            add(PointNew(topRight.x, topRight.y))
            add(PointNew(bottomRight.x, bottomRight.y))
            add(PointNew(bottomLeft.x, bottomLeft.y))
        }

        list.add(newArea)
        selectedArea = newArea

        onTrafficAreaEditListener?.onSelectedAreaChanged(selectedArea)
        onTrafficAreaEditListener?.onAreaCreated(newArea)

        invalidate()
    }

    fun createAreaFromFlatPointsDouble(
        flatPoints: List<Double>,
        newArea: TrafficArea,
    ) {
        createAreaFromFlatPoints(flatPoints.map { it.toFloat() }, newArea)
    }

    private fun createAreaFromFlatPoints(flatPoints: List<Float>, newArea: TrafficArea) {
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
        newArea.areaVertexPnt.apply {
            clear()
            addAll(points)
        }

        synchronized(list) {
            list.add(newArea)
        }

        // 通知创建完成
        onTrafficAreaEditListener?.onAreaCreated(newArea)

        invalidate()
    }

    /**
     * 设置编辑监听器
     */
    fun setOnTrafficAreaEditListener(listener: OnTrafficAreaEditListener?) {
        this.onTrafficAreaEditListener = listener
    }

    /**
     * 设置要编辑的区域
     */
    fun setSelectedTrafficArea(area: TrafficArea?) {
        setSelectedAreaBase(area)
    }

    /**
     * 设置要绘制的区域数据
     */
    fun setTrafficAreaData(data: MutableList<TrafficArea>) {
        synchronized(list) {
            this.list.clear()
            this.list.addAll(data)
        }
        invalidate() // 触发重绘
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onTrafficAreaEditListener = null
    }

    // 清扫区域编辑回调接口
    interface OnTrafficAreaEditListener {
        // 选中区域变化
        fun onSelectedAreaChanged(area: TrafficArea?) {}

        // 顶点开始拖动
        fun onVertexDragStart(area: TrafficArea?, vertexIndex: Int) {}

        // 顶点拖动中
        fun onVertexDragging(area: TrafficArea?, vertexIndex: Int, newX: Float, newY: Float) {}

        // 顶点拖动结束
        fun onVertexDragEnd(area: TrafficArea?, vertexIndex: Int, isInsideMap: Boolean)

        // 添加了新顶点
        fun onVertexAdded(area: TrafficArea?, vertexIndex: Int, x: Float, y: Float)

        // 删除了边
        fun onEdgeRemoved(area: TrafficArea?, edgeIndex: Int)

        // 删除了顶点
        fun onVertexRemoved(area: TrafficArea?, vertexIndex: Int)

        // 创建了新区域
        fun onAreaCreated(area: TrafficArea?) {}

        // 区域开始拖动
        fun onAreaDragStart(area: TrafficArea?) {}

        // 区域拖动中
        fun onAreaDragging(area: TrafficArea?) {}

        // 区域拖动结束
        fun onAreaDragEnd(area: TrafficArea?, isInsideMap: Boolean) {}

        // 点击区域
        fun onAreaClick(area: TrafficArea?) {}
    }
}
