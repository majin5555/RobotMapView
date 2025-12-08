package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.util.Log
import com.siasun.dianshi.R
import com.siasun.dianshi.bean.LineNew
import com.siasun.dianshi.bean.pp.PathPlanResultBean
import com.siasun.dianshi.bean.PointNew
import com.siasun.dianshi.bean.TeachPoint
import com.siasun.dianshi.bean.pp.Bezier
import java.lang.ref.WeakReference

/**
 * 路线
 */
class PathView @SuppressLint("ViewConstructor") constructor(
    context: Context?,
    parent: WeakReference<MapView>
) :
    SlamWareBaseView(context, parent) {
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
    }

    //试教中的绿色的点的集合
    private val teachPointList = mutableListOf<TeachPoint>()
    private var mCleanPathPlanResultBean: PathPlanResultBean? = null // 清扫路径规划结果
    private var mGlobalPathPlanResultBean: PathPlanResultBean? = null //全局路径规划结果
    
    // 优化：创建可复用的Path对象，避免在onDraw中频繁创建
    private val bezierPath = Path()

    init {

    }

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        //绘制试教中的点
        for (point in teachPointList) {
            drawTeachPointIng(canvas, point)
        }

        //绘制清扫路线
        mCleanPathPlanResultBean?.let {cleanPath ->
            // 优化：使用for循环代替forEach，减少lambda创建
            for (line in cleanPath.m_vecLineOfPathPlan) {
                drawPPLinePath(canvas, line)
            }
            for (bezier in cleanPath.m_vecBezierOfPathPlan) {
                drawPPBezierPath(canvas, bezier)
            }
        }
        
        //绘制全局路径
        mGlobalPathPlanResultBean?.let { globalPath ->
            for (line in globalPath.m_vecLineOfPathPlan) {
                drawPPLinePath(canvas, line)
            }
            for (bezier in globalPath.m_vecBezierOfPathPlan) {
                drawPPBezierPath(canvas, bezier)
            }

            // 创建世界系坐标点
            if (globalPath.startPoint != null && globalPath.startPoint.size >= 3 && 
                globalPath.endPoint != null && globalPath.endPoint.size >= 3) {
                val startPoint2d = PointNew(globalPath.startPoint[0], globalPath.startPoint[1])
                val endPoint2d = PointNew(globalPath.endPoint[0], globalPath.endPoint[1])

                drawStartAndEndPoint(
                    canvas,
                    startPoint2d,
                    endPoint2d,
                    context.getString(R.string.start_point),
                    context.getString(R.string.end_point)
                )
            }
        }
    }

    /**
     * 绘制示教点
     *
     */
    private fun drawTeachPointIng(canvas: Canvas, point: TeachPoint) {
        val mapView = mParent.get() ?: return
        val pnt: PointF = mapView.worldToScreen(point.x.toFloat(), point.y.toFloat())
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

        // 优化：复用Path对象，避免每次绘制都创建新的Path
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
        drawLabel(canvas, startPointName!!, startPoint, mGreenPaint)
        drawCircle(canvas, endPoint, 10f, mRedPaint)
        drawLabel(canvas, endPointName!!, endPoint, mRedPaint)
    }

    /**
     * 外部接口: 设置试教点 试教中
     */
    fun setTeachPoint(point: TeachPoint) {
        teachPointList.add(point)
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
        teachPointList.clear()
        invalidate()

    }

    fun clearPathPlan() {
        setGlobalPathPlanResultBean(null)
        setCleanPathPlanResultBean(null)
        invalidate()

    }


}
