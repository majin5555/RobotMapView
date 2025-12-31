package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.Log
import com.siasun.dianshi.R
import com.siasun.dianshi.bean.LineNew
import com.siasun.dianshi.bean.PointNew
import com.siasun.dianshi.bean.TeachPoint
import com.siasun.dianshi.bean.pp.Bezier
import com.siasun.dianshi.bean.pp.PathPlanResultBean
import java.lang.ref.WeakReference

/**
 * 路线
 */
@SuppressLint("ViewConstructor")
class PathView @SuppressLint("ViewConstructor") constructor(
    context: Context?, parent: WeakReference<MapView>
) : SlamWareBaseView(context, parent) {

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

    // 预加载字符串资源，避免重复创建
    private val startPointText by lazy { context?.getString(R.string.start_point) ?: "" }
    private val endPointText by lazy { context?.getString(R.string.end_point) ?: "" }

    // 优化：创建可复用的Path对象，避免在onDraw中频繁创建
    private val bezierPath = Path()

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        // 绘制试教中的点 - 使用副本避免并发修改
        val pointListCopy = synchronized(teachPointList) {
            teachPointList.toList()
        }
        for (point in pointListCopy) {
            drawTeachPointIng(canvas, point)
        }

        // 绘制清扫路线
        mCleanPathPlanResultBean?.let { cleanPath ->
            Log.d("mCleanPathPlanResultBean","绘制清扫路线 ${mCleanPathPlanResultBean.toString()}")
            // 采样绘制直线
            for (i in cleanPath.m_vecLineOfPathPlan.indices) {
                drawPPLinePath(canvas, cleanPath.m_vecLineOfPathPlan[i])
            }
            // 采样绘制贝塞尔曲线
            for (i in cleanPath.m_vecBezierOfPathPlan.indices) {
                drawPPBezierPath(canvas, cleanPath.m_vecBezierOfPathPlan[i])
            }
        }

        // 绘制全局路径 - 不使用采样率，确保完整显示
        mGlobalPathPlanResultBean?.let { globalPath ->
            // 绘制直线 - 不跳过任何点
            for (line in globalPath.m_vecLineOfPathPlan) {
                drawPPLinePath(canvas, line)
            }
            // 绘制贝塞尔曲线 - 不跳过任何点
            for (bezier in globalPath.m_vecBezierOfPathPlan) {
                drawPPBezierPath(canvas, bezier)
            }

            // 创建世界系坐标点
            if (globalPath.startPoint != null && globalPath.startPoint.size >= 3 &&
                globalPath.endPoint != null && globalPath.endPoint.size >= 3
            ) {
                val startPoint2d = PointNew(globalPath.startPoint[0], globalPath.startPoint[1])
                val endPoint2d = PointNew(globalPath.endPoint[0], globalPath.endPoint[1])

                drawStartAndEndPoint(
                    canvas,
                    startPoint2d,
                    endPoint2d,
                    startPointText,
                    endPointText
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


    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        // 清理资源，防止内存泄漏
        clearTeachPoint()
        clearPathPlan()
        mCleanPathPlanResultBean = null
        mGlobalPathPlanResultBean = null
    }


    /**
     * 清除当前选择
     */
    fun clearSelection() {

        invalidate()
    }
}
