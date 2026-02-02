package com.siasun.dianshi.view.createMap.map3D

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.siasun.dianshi.bean.ConstraintNode
import com.siasun.dianshi.view.SlamWareBaseView
import java.lang.ref.WeakReference
import com.siasun.dianshi.view.createMap.CreateMapWorkMode

/**
 * 人工约束节点
 */
@SuppressLint("ViewConstructor")
class ConstrainNodes(context: Context?, val parent: WeakReference<CreateMapView3D>) :
    SlamWareBaseView<CreateMapView3D>(context, parent) {
    private val TAG = this::class.java.simpleName
    private var currentWorkMode = CreateMapWorkMode.MODE_SHOW_MAP

    //添加人工约束节点数据
    private val keyConstraintNodes: MutableList<ConstraintNode> = mutableListOf()


    /**
     * 设置工作模式
     */
    fun setWorkMode(mode: CreateMapWorkMode) {
        if (currentWorkMode == mode) return // 避免重复设置

        currentWorkMode = mode

    }

    companion object {
        val constraintNodePaint = Paint().apply {
            color = Color.BLUE
            isAntiAlias = true
            style = Paint.Style.FILL
            strokeWidth = 15f
        }
    }


    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val mapView = parent.get() ?: return
        canvas.save()
        if (keyConstraintNodes.isNotEmpty()) {
            keyConstraintNodes.forEach { point ->
                val screenPos = mapView.worldToScreen(point.x.toFloat(), point.y.toFloat())
                canvas.drawPoint(screenPos.x, screenPos.y, constraintNodePaint)
                //绘制上线点名称
                canvas.drawText(
                    "${point.id}", (screenPos.x + 15), (screenPos.y + 15), constraintNodePaint
                )
            }
        }
        canvas.restore()
    }


    /**
     * 外部接口：添加人工约束节点数据
     */

    fun addConstraintNodes(constraintNode: ConstraintNode) {
        keyConstraintNodes.add(constraintNode)
    }

    /**
     * 清理资源，防止内存泄漏
     */
    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        keyConstraintNodes.clear()
        // 清理父引用
        parent.clear()
    }
}
