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

import android.graphics.Matrix

/**
 * 人工约束节点
 */
@SuppressLint("ViewConstructor")
class ConstrainNodes(context: Context?, val parent: WeakReference<CreateMapView3D>) :
    SlamWareBaseView<CreateMapView3D>(context, parent) {
    private val TAG = this::class.java.simpleName
    private var currentWorkMode = CreateMapWorkMode.MODE_SHOW_MAP

    //添加人工约束节点数据 (使用同步列表确保线程安全)
    private val keyConstraintNodes: MutableList<ConstraintNode> = java.util.Collections.synchronizedList(mutableListOf())
    
    // 缓存数据
    private var mWorldPoints: FloatArray? = null
    private var mScreenPoints: FloatArray? = null
    private var isDirty = false
    
    // 矩阵对象，复用避免分配
    private val mWorldToPixelMatrix = Matrix()
    private val mTotalMatrix = Matrix()
    
    // 实例画笔，避免静态副作用
    private val mDrawPaint = Paint(constraintNodePaint)


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
        
        // 1. 获取并锁定数据快照，防止遍历过程中被修改
        val nodesSnapshot: List<ConstraintNode>
        synchronized(keyConstraintNodes) {
            if (keyConstraintNodes.isEmpty()) {
                canvas.restore()
                return
            }
            nodesSnapshot = ArrayList(keyConstraintNodes)
        }
        
        val count = nodesSnapshot.size
        
        // 2. 检查并重建世界坐标缓存
        if (isDirty || mWorldPoints == null || mWorldPoints!!.size < count * 2) {
            if (mWorldPoints == null || mWorldPoints!!.size < count * 2) {
                mWorldPoints = FloatArray(count * 2)
            }
            // 填充世界坐标
            for (i in 0 until count) {
                val node = nodesSnapshot[i]
                mWorldPoints!![i * 2] = node.x.toFloat()
                mWorldPoints!![i * 2 + 1] = node.y.toFloat()
            }
            isDirty = false
        }
        
        // 3. 准备屏幕坐标缓存
        if (mScreenPoints == null || mScreenPoints!!.size < count * 2) {
            mScreenPoints = FloatArray(count * 2)
        }
        
        // 4. 构建变换矩阵 (World -> Screen)
        // 必须在同步块中获取 mapData 数据
        var resolution = 0.05f
        synchronized(mapView.mSrf.mapData) {
            val mapData = mapView.mSrf.mapData
            resolution = mapData.resolution
            if (resolution <= 0) resolution = 0.05f
            
            // 构建 World -> Pixel 矩阵
            mWorldToPixelMatrix.reset()
            mWorldToPixelMatrix.postTranslate(-mapData.originX, -mapData.originY)
            mWorldToPixelMatrix.postScale(1f / resolution, -1f / resolution)
            mWorldToPixelMatrix.postTranslate(0f, mapData.height.toFloat())
        }
        
        // Total = OuterMatrix * WorldToPixelMatrix
        mTotalMatrix.set(mapView.outerMatrix)
        mTotalMatrix.preConcat(mWorldToPixelMatrix)
        
        // 5. 批量变换坐标 (Native高效运算)
        // 将 mWorldPoints 变换为 mScreenPoints
        mTotalMatrix.mapPoints(mScreenPoints, 0, mWorldPoints, 0, count)
        
        // 6. 批量绘制点 (Hardware Accelerated)
        // 调整 Paint 大小，这里我们直接绘制在屏幕坐标上，不需要调整 Paint 的 Scale
        // 因为我们没有 scale canvas，而是变换了点坐标
        canvas.drawPoints(mScreenPoints!!, 0, count * 2, mDrawPaint)
        
        // 7. 绘制文本 (必须遍历)
        // 使用预计算好的屏幕坐标
        for (i in 0 until count) {
            val sx = mScreenPoints!![i * 2]
            val sy = mScreenPoints!![i * 2 + 1]
            val node = nodesSnapshot[i]
            
            canvas.drawText(
                "${node.id}",
                (sx + 15),
                (sy + 15),
                mDrawPaint
            )
        }
        
        canvas.restore()
    }


    /**
     * 外部接口：添加人工约束节点数据
     */

    fun addConstraintNodes(constraintNode: ConstraintNode) {
        keyConstraintNodes.add(constraintNode)
        isDirty = true
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
