package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import com.siasun.dianshi.bean.CleanAreaNew
import com.siasun.dianshi.bean.PointNew
import java.lang.ref.WeakReference

/**
 * 清扫区域
 */
@SuppressLint("ViewConstructor")
class PolygonEditView(context: Context?, parent: WeakReference<MapView>) :
    BasePolygonEditView<CleanAreaNew>(context, parent) {

    // 编辑监听器 - 使用强引用确保回调能被触发
    private var onCleanAreaEditListener: OnCleanAreaEditListener? = null

    // ----------------- 实现父类抽象方法 -----------------

    override fun getAreaVertices(area: CleanAreaNew): MutableList<PointNew> {
        return area.m_VertexPnt
    }

    override fun getAreaName(area: CleanAreaNew): String {
        return area.sub_name ?: ""
    }

    override fun isEditMode(mode: WorkMode): Boolean {
        return mode == WorkMode.MODE_CLEAN_AREA_EDIT || mode == WorkMode.MODE_CLEAN_AREA_ADD
    }

    override fun handleDoubleTapSelectArea() {
        parentMapView.get()?.mPolygonEditViewPoint?.setWorkMode(WorkMode.MODE_SHOW_MAP)
        setWorkMode(WorkMode.MODE_CLEAN_AREA_EDIT)
    }

    override fun onValidateAndFixStartPoint(area: CleanAreaNew) {
        validateAndFixStartPoint(area)
    }

    // ----------------- 路由回调给具体的Listener -----------------

    override fun onSelectedAreaChangedCallback(area: CleanAreaNew?) {
        onCleanAreaEditListener?.onSelectedAreaChanged(area)
    }

    override fun onVertexDragStartCallback(area: CleanAreaNew, vertexIndex: Int) {
        onCleanAreaEditListener?.onVertexDragStart(area, vertexIndex)
    }

    override fun onVertexDraggingCallback(
        area: CleanAreaNew, vertexIndex: Int, newX: Float, newY: Float
    ) {
        onCleanAreaEditListener?.onVertexDragging(area, vertexIndex, newX, newY)
    }

    override fun onVertexDragEndCallback(
        area: CleanAreaNew, vertexIndex: Int, isInsideMap: Boolean
    ) {
        onCleanAreaEditListener?.onVertexDragEnd(area, vertexIndex, isInsideMap)
    }

    override fun onVertexAddedCallback(area: CleanAreaNew, vertexIndex: Int, x: Float, y: Float) {
        onCleanAreaEditListener?.onVertexAdded(area, vertexIndex, x, y)
    }

    override fun onEdgeRemovedCallback(area: CleanAreaNew, edgeIndex: Int) {
        onCleanAreaEditListener?.onEdgeRemoved(area, edgeIndex)
    }

    override fun onVertexRemovedCallback(area: CleanAreaNew, vertexIndex: Int) {
        onCleanAreaEditListener?.onVertexRemoved(area, vertexIndex)
    }

    override fun onAreaCreatedCallback(area: CleanAreaNew) {
        onCleanAreaEditListener?.onAreaCreated(area)
    }

    override fun onAreaDragStartCallback(area: CleanAreaNew) {
        onCleanAreaEditListener?.onAreaDragStart(area)
    }

    override fun onAreaDraggingCallback(area: CleanAreaNew) {
        onCleanAreaEditListener?.onAreaDragging(area)
    }

    override fun onAreaDragEndCallback(area: CleanAreaNew, isInsideMap: Boolean) {
        onCleanAreaEditListener?.onAreaDragEnd(area, isInsideMap)
    }

    override fun onAreaClickCallback(area: CleanAreaNew) {
        onCleanAreaEditListener?.onAreaClick(area)
    }

    // ----------------- 对外暴露的方法 -----------------

    /**
     * 在地图中心创建一个矩形清扫区域
     */
    fun createRectangularAreaAtCenter(newArea: CleanAreaNew) {
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

        if (newArea.routeType == 2) {
            //初始化开始点是含糊计算的(世界坐标)
            newArea.areaStartPoint.set(0f, 0f)
        } else {
            //初始化开始点是含糊计算的(世界坐标)
            newArea.areaStartPoint.set(topLeft.x, topLeft.y)
        }

        list.add(newArea)
        selectedArea = newArea

        onCleanAreaEditListener?.onSelectedAreaChanged(selectedArea)
        onCleanAreaEditListener?.onAreaCreated(newArea)

        invalidate()
    }

    fun createAreaFromFlatPointsDouble(
        flatPoints: List<Double>,
        newArea: CleanAreaNew,
    ) {
        createAreaFromFlatPoints(flatPoints.map { it.toFloat() }, newArea)
    }

    private fun createAreaFromFlatPoints(flatPoints: List<Float>, newArea: CleanAreaNew) {
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
        onCleanAreaEditListener?.onAreaCreated(newArea)

        invalidate()
    }

    /**
     * 设置编辑监听器
     */
    fun setOnCleanAreaEditListener(listener: OnCleanAreaEditListener?) {
        this.onCleanAreaEditListener = listener
    }

    /**
     * 设置要编辑的区域
     */
    fun setSelectedCleanArea(area: CleanAreaNew?) {
        this.selectedArea = area
        ensureAreaStartPoint(area)

        selectedPointIndex = -1
        isDragging = false
        onCleanAreaEditListener?.onSelectedAreaChanged(area)
        invalidate()
    }

    fun updateAreaStartPoint(newX: Float, newY: Float) {
        selectedArea?.areaStartPoint?.set(newX, newY)
        invalidate()
    }

    fun validateStartPoint() {
        selectedArea?.let { validateAndFixStartPoint(it) }
    }

    /**
     * 设置要绘制的区域数据
     */
    fun setCleanAreaData(data: MutableList<CleanAreaNew>) {
        synchronized(list) {
            this.list.clear()
            this.list.addAll(data)
        }
        invalidate() // 触发重绘
    }

    /**
     * 判断世界坐标点是否在区域内
     */
    fun isStartPointInArea(worldX: Float, worldY: Float): Boolean {
        val area = selectedArea ?: return false
        var isInside = false
        val points = area.m_VertexPnt
        var j = points.size - 1
        for (i in points.indices) {
            if ((points[i].Y > worldY) != (points[j].Y > worldY) && (worldX < (points[j].X - points[i].X) * (worldY - points[i].Y) / (points[j].Y - points[i].Y) + points[i].X)) {
                isInside = !isInside
            }
            j = i
        }
        return isInside
    }

    /**
     * 检查并修正开始点位置，确保其在多边形内部
     */
    private fun validateAndFixStartPoint(area: CleanAreaNew?) {
        area ?: return
        if (area.routeType != 2) {
            if (!isStartPointInArea(area.areaStartPoint.x, area.areaStartPoint.y)) {
                val topLeft = getTopLeftVertex(area.m_VertexPnt)
                topLeft?.let {
                    area.areaStartPoint.set(it.X, it.Y)
                }
            }
        }
    }

    private fun ensureAreaStartPoint(area: CleanAreaNew?) {
        area ?: return
        if (area.routeType == 2) return
        if (area.areaStartPoint.x != 0f || area.areaStartPoint.y != 0f) return

        val topLeftVertex = area.m_VertexPnt.minByOrNull { point -> point.X + point.Y }
        topLeftVertex?.let { point ->
            area.areaStartPoint.set(point.X, point.Y)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onCleanAreaEditListener = null
    }

    // 清扫区域编辑回调接口
    interface OnCleanAreaEditListener {
        // 选中区域变化
        fun onSelectedAreaChanged(area: CleanAreaNew?) {}

        // 顶点开始拖动
        fun onVertexDragStart(area: CleanAreaNew?, vertexIndex: Int) {}

        // 顶点拖动中
        fun onVertexDragging(area: CleanAreaNew?, vertexIndex: Int, newX: Float, newY: Float) {}

        // 顶点拖动结束
        fun onVertexDragEnd(area: CleanAreaNew?, vertexIndex: Int, isInsideMap: Boolean)

        // 添加了新顶点
        fun onVertexAdded(area: CleanAreaNew?, vertexIndex: Int, x: Float, y: Float)

        // 删除了边
        fun onEdgeRemoved(area: CleanAreaNew?, edgeIndex: Int)

        // 删除了顶点
        fun onVertexRemoved(area: CleanAreaNew?, vertexIndex: Int)

        // 创建了新区域
        fun onAreaCreated(area: CleanAreaNew?) {}

        // 区域开始拖动
        fun onAreaDragStart(area: CleanAreaNew?) {}

        // 区域拖动中
        fun onAreaDragging(area: CleanAreaNew?) {}

        // 区域拖动结束
        fun onAreaDragEnd(area: CleanAreaNew?, isInsideMap: Boolean) {}

        // 点击区域
        fun onAreaClick(area: CleanAreaNew?) {}
    }
}