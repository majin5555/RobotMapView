package com.siasun.dianshi.view

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.siasun.dianshi.R
import com.siasun.dianshi.bean.LineNew
import com.siasun.dianshi.bean.pp.PathPlanResultBean
import com.siasun.dianshi.bean.PointNew
import com.siasun.dianshi.bean.TeachPoint
import com.siasun.dianshi.bean.pp.Angle
import com.siasun.dianshi.bean.pp.Bezier
import com.siasun.dianshi.bean.pp.Posture
import com.siasun.dianshi.bean.world.GenericPath
import com.siasun.dianshi.bean.world.World
import com.siasun.dianshi.utils.RouteEdit
import java.lang.ref.WeakReference

/**
 * 路线
 */
class PathView @SuppressLint("ViewConstructor") constructor(
    context: Context?,
    parent: WeakReference<MapView>
) :
    SlamWareBaseView(context, parent) {
    private var isDrawingEnabled: Boolean = true
    var mRouteEdit = RouteEdit() //路径操作对象(路径创建、编辑)


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
            style = Paint.Style.FILL
            strokeWidth = 2f
            color = Color.BLACK
            isFilterBitmap = true
            isDither = true
            xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
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

    // 保存parent引用以便安全访问
    private val mapViewRef: WeakReference<MapView> = parent

    @SuppressLint("DrawAllocation")
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (isDrawingEnabled) {

            // 绘制试教中的点 - 使用副本避免并发修改
            val pointListCopy = synchronized(teachPointList) {
                teachPointList.toList()
            }
            for (point in pointListCopy) {
                drawTeachPointIng(canvas, point)
            }

            // 绘制清扫路线
            mCleanPathPlanResultBean?.let { cleanPath ->
                // 采样绘制直线
                for (i in cleanPath.m_vecLineOfPathPlan.indices step SAMPLE_RATE) {
                    drawPPLinePath(canvas, cleanPath.m_vecLineOfPathPlan[i])
                }
                // 采样绘制贝塞尔曲线
                for (i in cleanPath.m_vecBezierOfPathPlan.indices step SAMPLE_RATE) {
                    drawPPBezierPath(canvas, cleanPath.m_vecBezierOfPathPlan[i])
                }
            }

            // 绘制全局路径
            mGlobalPathPlanResultBean?.let { globalPath ->
                // 采样绘制直线
                for (i in globalPath.m_vecLineOfPathPlan.indices step SAMPLE_RATE) {
                    drawPPLinePath(canvas, globalPath.m_vecLineOfPathPlan[i])
                }
                // 采样绘制贝塞尔曲线
                for (i in globalPath.m_vecBezierOfPathPlan.indices step SAMPLE_RATE) {
                    drawPPBezierPath(canvas, globalPath.m_vecBezierOfPathPlan[i])
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
            val mapView = mapViewRef.get() ?: return

            mWorld?.let {
                mWorld?.m_layers?.let {
                    // 应用矩阵变换，确保拖动地图时路径跟随移动
                    canvas.save()
                    canvas.concat(mMatrix)
                    
                    //绘制地图
                    it.Draw(mapView.mSrf, canvas)

                    mRouteEdit.m_KeyPst.Draw(mapView.mSrf, canvas, mPaint)

                    //重点显示要编辑的路径
                    for (i in mRouteEdit.m_nCurPathIndex.indices) {
                        val pPath = it.m_PathBase.m_pPathIdx[mRouteEdit.m_nCurPathIndex[i]].m_ptr
                        if (pPath != null) {
                            //避免删除节点，会引起删除线，需要进行是否为空的判断
                            pPath.Draw(mapView.mSrf, canvas, Color.GREEN, 3)
                            // 修改操作下显示控制点
                            if (pPath.m_uType.toInt() == 10 && mRouteEdit.mEditWorldStage == mRouteEdit.WRD_MOD_NODE) {
                                (pPath as GenericPath).DrawCtrlPoints(
                                    mapView.mSrf,
                                    canvas,
                                    null,
                                    Color.GREEN,
                                    5
                                )
                                if (mRouteEdit.mCurKeyId > 0) {
                                    (pPath).m_Curve.m_ptKey[mRouteEdit.mCurKeyId - 1].Draw(
                                        mapView.mSrf, canvas, Color.RED, 8
                                    )
                                }
                            }
                            //在"GetStartNode"这里会空
                            val tempStart = pPath.GetStartNode()
                            val tempEnd = pPath.GetEndNode()
                            if (tempStart == null || tempEnd == null) {
                                continue
                            }
                            pPath.GetStartNode().Draw(mapView.mSrf, canvas, Color.GREEN)
                            tempEnd.Draw(mapView.mSrf, canvas, Color.GREEN)
                        }
                    }

                    //显示选择的节点
                    if (mRouteEdit.mCurNodeId != -1) {
                        val node = it.GetNode(mRouteEdit.mCurNodeId)
                        // 修改操作下显示带位子的点
                        if (node != null && mRouteEdit.mEditWorldStage == mRouteEdit.WRD_MOD_NODE) {
                            //将节点的姿态绘制出来。便于修改角度
                            mRouteEdit.m_ModNodePos.Clear()
                            val pst = Posture()
                            pst.x = node.x
                            pst.y = node.y
                            val mAngles = arrayOfNulls<Angle>(4)
                            val nCount = it.GetNodeHeadingAngle(mRouteEdit.mCurNodeId, mAngles, 4)
                            if (nCount > 0) {
                                pst.fThita = mAngles[0]!!.m_fRad
                            }
                            mRouteEdit.m_ModNodePos.AddPst(pst)
                            mRouteEdit.m_ModNodePos.m_SelectPstID = 0 //默认被选中
                            mRouteEdit.m_ModNodePos.Draw(mapView.mSrf, canvas, mPaint)
                        }
                        node?.Draw(mapView.mSrf, canvas, Color.RED)
                    }
                    if (mRouteEdit.m_RegConDownCount == 1 && mRouteEdit.mEditWorldStage == mRouteEdit.WRD_ADD_REG_CON) {
                        mRouteEdit.m_RegConStart.Draw(mapView.mSrf, canvas, Color.BLUE, 5) //color
                    }
                    
                    // 恢复画布状态
                    canvas.restore()
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
    }

    private var mWorld: World? = null
    fun setWorld(world: World) {
        mWorld = world
    }
}
