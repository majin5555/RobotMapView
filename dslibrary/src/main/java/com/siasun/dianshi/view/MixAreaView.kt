package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.view.MotionEvent
import com.siasun.dianshi.bean.PassPoints
import com.siasun.dianshi.bean.WorkAreasNew
import com.siasun.dianshi.bean.PointNew
import java.lang.ref.WeakReference

/**
 * 混行区域
 */
@SuppressLint("ViewConstructor")
class MixAreaView(context: Context?, parent: WeakReference<MapView>) :
    BasePolygonEditView<WorkAreasNew>(context, parent) {

    private val clickThreshold = 20f // 像素单位

    // 编辑监听器 - 使用强引用确保回调能被触发
    private var onMixAreaEditListener: OnMixAreaEditListener? = null

    companion object {
        private val mixAreaPaint = Paint().apply {
            style = Paint.Style.STROKE
            color = Color.MAGENTA
            strokeWidth = 2f
            isAntiAlias = true
        }

        private val passPointPain = Paint().apply {
            color = Color.GRAY
            isAntiAlias = true
        }

        const val BASE_RADIUS = 10f
    }

    // ----------------- 实现父类抽象方法 -----------------

    override fun getAreaVertices(area: WorkAreasNew): MutableList<PointNew> {
        return area.areaVertexPnt
    }

    override fun getAreaName(area: WorkAreasNew): String {
        return area.name ?: ""
    }

    override fun isEditMode(mode: WorkMode): Boolean {
        return mode == WorkMode.MODE_MIX_AREA_EDIT || mode == WorkMode.MODE_MIX_AREA_ADD
    }

    override fun getSpecificAreaPaint(area: WorkAreasNew): Paint {
        return mixAreaPaint
    }

    override fun handleDoubleTapSelectArea() {
        setWorkMode(WorkMode.MODE_MIX_AREA_EDIT)
    }

    // ----------------- 路由回调给具体的Listener -----------------

    override fun onSelectedAreaChangedCallback(area: WorkAreasNew?) {
        onMixAreaEditListener?.onSelectedAreaChanged(area)
    }

    override fun onVertexDragStartCallback(area: WorkAreasNew, vertexIndex: Int) {
        onMixAreaEditListener?.onVertexDragStart(area, vertexIndex)
    }

    override fun onVertexDraggingCallback(area: WorkAreasNew, vertexIndex: Int, newX: Float, newY: Float) {
        onMixAreaEditListener?.onVertexDragging(area, vertexIndex, newX, newY)
    }

    override fun onVertexDragEndCallback(area: WorkAreasNew, vertexIndex: Int, isInsideMap: Boolean) {
        onMixAreaEditListener?.onVertexDragEnd(area, vertexIndex)
    }

    override fun onVertexAddedCallback(area: WorkAreasNew, vertexIndex: Int, x: Float, y: Float) {
        onMixAreaEditListener?.onVertexAdded(area, vertexIndex, x, y)
    }

    override fun onEdgeRemovedCallback(area: WorkAreasNew, edgeIndex: Int) {
        onMixAreaEditListener?.onEdgeRemoved(area, edgeIndex)
    }

    override fun onVertexRemovedCallback(area: WorkAreasNew, vertexIndex: Int) {
        onMixAreaEditListener?.onVertexRemoved(area, vertexIndex)
    }

    override fun onAreaCreatedCallback(area: WorkAreasNew) {
        onMixAreaEditListener?.onAreaCreated(area)
    }

    override fun onAreaDragStartCallback(area: WorkAreasNew) {
        onMixAreaEditListener?.onAreaDragStart(area)
    }

    override fun onAreaDraggingCallback(area: WorkAreasNew) {
        onMixAreaEditListener?.onAreaDragging(area)
    }

    override fun onAreaDragEndCallback(area: WorkAreasNew, isInsideMap: Boolean) {
        onMixAreaEditListener?.onAreaDragEnd(area)
    }

    // ----------------- 对外暴露的方法 -----------------

    /**
     * 在地图中心创建一个矩形清扫区域
     */
    fun createRectangularAreaAtCenter(newArea: WorkAreasNew) {
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

        onMixAreaEditListener?.onSelectedAreaChanged(selectedArea)
        onMixAreaEditListener?.onAreaCreated(newArea)

        invalidate()
    }

    /**
     * 设置编辑监听器
     */
    fun setOnMixAreaEditListener(listener: OnMixAreaEditListener?) {
        this.onMixAreaEditListener = listener
    }

    /**
     * 设置要编辑的区域
     */
    fun setSelectedArea(area: WorkAreasNew?) {
        this.selectedArea = area
        selectedPointIndex = -1
        isDragging = false
        onMixAreaEditListener?.onSelectedAreaChanged(area)
        invalidate()
    }

    /**
     * 设置要绘制的区域数据
     */
    fun setMixAreaData(data: MutableList<WorkAreasNew>) {
        synchronized(list) {
            this.list.clear()
            this.list.addAll(data)
        }
        invalidate() // 触发重绘
    }

    // ----------------- 覆盖父类绘制与事件逻辑 -----------------

    override fun drawPolygon(canvas: Canvas, area: WorkAreasNew, isSelected: Boolean) {
        super.drawPolygon(canvas, area, isSelected)

        val mapView = parentMapView.get() ?: return
        val pointLocation = PointF()

        if (currentWorkMode == WorkMode.MODE_MIX_AREA_EDIT) {
            val passPoints = area.passPointsList
            passPoints.forEach { pass ->
                val screenPoint = mapView.worldToScreen(pass.gate.x, pass.gate.y)
                pointLocation.set(screenPoint.x, screenPoint.y)
                passPointPain.color = Color.GREEN
                passPointPain.style = Paint.Style.STROKE
                passPointPain.strokeWidth = 2f
                canvas.drawCircle(pointLocation.x, pointLocation.y, BASE_RADIUS + 5f, passPointPain)
                // 恢复填充样式
                passPointPain.style = Paint.Style.FILL
            }
        }

        area.passPointsList.forEach { passPoint ->
            val screenPoint = mapView.worldToScreen(passPoint.gate.x, passPoint.gate.y)
            pointLocation.set(screenPoint.x, screenPoint.y)
            passPointPain.color = Color.GRAY
            canvas.drawCircle(pointLocation.x, pointLocation.y, BASE_RADIUS, passPointPain)

            pointLocation.x += 10f
            pointLocation.y += 10f
            canvas.drawText(passPoint.id, pointLocation.x, pointLocation.y, textPaint)
        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (currentWorkMode == WorkMode.MODE_MIX_AREA_EDIT || currentWorkMode == WorkMode.MODE_MIX_AREA_ADD) {
            if (event.action == MotionEvent.ACTION_DOWN) {
                val mapView = parentMapView.get() ?: return super.onTouchEvent(event)
                val worldPoint = mapView.screenToWorld(event.x, event.y)
                val clickedPassPoint = findPassPointNearPoint(worldPoint)
                if (clickedPassPoint != null) {
                    onMixAreaEditListener?.onEditPassPoint(clickedPassPoint)
                    return true
                }
            }
        }
        return super.onTouchEvent(event)
    }

    /**
     * 查找指定世界坐标点附近的乘梯点
     */
    private fun findPassPointNearPoint(worldPoint: PointF): PassPoints? {
        val mapView = parentMapView.get() ?: return null
        val currentArea = selectedArea ?: return null

        val passPointsCopy: List<PassPoints>
        synchronized(currentArea.passPointsList) {
            passPointsCopy = currentArea.passPointsList.toList()
        }

        for (passPoint in passPointsCopy) {
            val gateScreenPoint = mapView.worldToScreen(passPoint.gate.x, passPoint.gate.y)
            if (isPointNearScreenPoint(gateScreenPoint, worldPoint, mapView)) {
                return passPoint
            }
        }
        return null
    }

    /**
     * 检查两个点是否在屏幕上足够接近
     */
    private fun isPointNearScreenPoint(
        screenPoint: PointF,
        worldPoint: PointF,
        mapView: MapView
    ): Boolean {
        val worldScreenPoint = mapView.worldToScreen(worldPoint.x, worldPoint.y)
        val dx = screenPoint.x - worldScreenPoint.x
        val dy = screenPoint.y - worldScreenPoint.y
        val distance = Math.sqrt((dx * dx + dy * dy).toDouble())
        return distance <= clickThreshold
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        onMixAreaEditListener = null
    }

    // 清扫区域编辑回调接口
    interface OnMixAreaEditListener {
        fun onSelectedAreaChanged(area: WorkAreasNew?) {}
        fun onVertexDragStart(area: WorkAreasNew, vertexIndex: Int) {}
        fun onVertexDragging(area: WorkAreasNew, vertexIndex: Int, newX: Float, newY: Float) {}
        fun onVertexDragEnd(area: WorkAreasNew, vertexIndex: Int)
        fun onVertexAdded(area: WorkAreasNew, vertexIndex: Int, x: Float, y: Float)
        fun onEdgeRemoved(area: WorkAreasNew, edgeIndex: Int)
        fun onVertexRemoved(area: WorkAreasNew, vertexIndex: Int) {}
        fun onAreaCreated(area: WorkAreasNew) {}
        fun onEditPassPoint(passPoints: PassPoints?) {}
        fun onAreaDragStart(area: WorkAreasNew) {}
        fun onAreaDragging(area: WorkAreasNew) {}
        fun onAreaDragEnd(area: WorkAreasNew) {}
    }
}
