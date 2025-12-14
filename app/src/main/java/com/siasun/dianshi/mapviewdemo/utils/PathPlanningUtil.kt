package com.siasun.dianshi.mapviewdemo.utils

import android.os.Build
import androidx.annotation.RequiresApi
import com.siasun.dianshi.bean.LineNew
import com.siasun.dianshi.bean.PlanPathResult
import com.siasun.dianshi.bean.Point2d
import com.siasun.dianshi.bean.PointNew
import com.siasun.dianshi.bean.pp.Bezier
import com.siasun.dianshi.bean.pp.PathPlanResultBean
import com.siasun.dianshi.framework.log.LogUtil
import com.siasun.dianshi.mapviewdemo.CLEAN_PATH_PLAN
import com.siasun.dianshi.mapviewdemo.GLOBAL_PATH_PLAN
import com.siasun.dianshi.mapviewdemo.PATH_BEZIER
import com.siasun.dianshi.mapviewdemo.PATH_LINE
import com.siasun.dianshi.mapviewdemo.PATH_MODE
import com.siasun.dianshi.mapviewdemo.TEACH_PATH_PLAN
import com.siasun.dianshi.view.MapView
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * @Author: Mj
 * @Date: 2025/12/09
 * @Description: 路径规划工具类
 */
/**
 * 路径规划工具类
 * 优化：移除静态变量，避免内存泄漏
 * 优化：引入对象池，减少对象创建
 */
object PathPlanningUtil {
    // 常量定义
     const val MAX_PATH_SEGMENTS = 10000 // 最大路段数量，防止内存溢出
     const val MAX_POINT_COUNT = 100000  // 最大点数量，防止内存溢出

    // 对象池 - 用于复用Point2d对象，减少内存分配
    private val point2dPool = ConcurrentLinkedQueue<Point2d>()
    // 对象池 - 用于复用PointNew对象，减少内存分配
    private val pointNewPool = ConcurrentLinkedQueue<PointNew>()

    // 从对象池获取Point2d对象
    private fun obtainPoint2d(): Point2d {
        return point2dPool.poll() ?: Point2d()
    }

    // 从对象池获取PointNew对象
    private fun obtainPointNew(): PointNew {
        return pointNewPool.poll() ?: PointNew()
    }

    // 将Point2d对象归还到对象池
    private fun recyclePoint2d(point: Point2d) {
        if (point2dPool.size < 1000) { // 限制对象池大小
            point2dPool.offer(point)
        }
    }

    // 将PointNew对象归还到对象池
    private fun recyclePointNew(point: PointNew) {
        if (pointNewPool.size < 1000) { // 限制对象池大小
            pointNewPool.offer(point)
        }
    }

    // 清空对象池（可选，用于内存紧张时）
    fun clearObjectPools() {
        point2dPool.clear()
        pointNewPool.clear()
    }

    /**
     * 获取路径规划结果对象
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun getPathPlanResultBean(result: PlanPathResult): PathPlanResultBean {
        return getPathPlanResultBean(result, null)
    }

    /**
     * 获取路径规划结果对象
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun getPathPlanResultBean(result: PlanPathResult, mMapView: MapView?): PathPlanResultBean {
        // 检查数据有效性
        if (result.m_fElementBuffer.size > MAX_POINT_COUNT) {
            LogUtil.e("路径点数量过大，可能导致内存溢出: ${result.m_fElementBuffer.size}")
            val resultBean = PathPlanResultBean()
            resultBean.m_bIsPlanOk = false
            return resultBean
        }

        val pathPlanResultBean = PathPlanResultBean()
        val mRes = result.m_iPlanResult.toByte()

        if ((mRes.toInt() shr 0) and 1 == 1) {
            pathPlanResultBean.m_bIsPlanOk = true
            pathPlanResultBean.m_iPathPlanId = result.m_iRegionNumber

            when (result.m_iPathPlanType) {
                GLOBAL_PATH_PLAN -> handleGlobalPathPlan(result, pathPlanResultBean)
                CLEAN_PATH_PLAN -> handleCleanPathPlan(result, pathPlanResultBean)
                TEACH_PATH_PLAN -> handleTeachPathPlan(result, pathPlanResultBean, mMapView)
                else -> LogUtil.e("未知的路径规划类型: ${result.m_iPathPlanType}")
            }
        } else {
            pathPlanResultBean.m_bIsPlanOk = false
        }
        return pathPlanResultBean
    }

    /**
     * 处理全局路径规划
     */
    private fun handleGlobalPathPlan(result: PlanPathResult, pathPlanResultBean: PathPlanResultBean) {
        // 全局路径规划 - 接收PP返回CMS结果
        if (result.m_strTo != "CMS" || result.m_iPlanResultMode != PATH_MODE) return

        pathPlanResultBean.m_iPathPlanType = GLOBAL_PATH_PLAN

        // 设置起点坐标 - 限制数据量
        result.m_fGloalPathPlanStartPosBuffer.take(3).forEachIndexed { index, fl ->
            pathPlanResultBean.startPoint[index] = fl
        }

        // 设置终点坐标 - 限制数据量
        result.m_fGloalPathPlanGoalPosBuffer.take(3).forEachIndexed { index, fl ->
            pathPlanResultBean.endPoint[index] = fl
        }

        // 限制路段数量，防止内存溢出
        val maxSegments = minOf(result.m_iPathSum, MAX_PATH_SEGMENTS)
        LogUtil.i("PAD解析PP数据 全局路径路段个数：$maxSegments")
        LogUtil.i("PAD解析PP数据 全局路径点位个数：${result.m_fElementBuffer.size}")

        // 解析路径数据
        parsePathData(result, pathPlanResultBean)
    }

    /**
     * 处理清扫路径规划
     */
    private fun handleCleanPathPlan(result: PlanPathResult, pathPlanResultBean: PathPlanResultBean) {
        // 清扫路径规划 - 接收PP返回pad结果
        if ((result.m_strTo != "CMS" && result.m_strTo != "pad") || result.m_iPlanResultMode != PATH_MODE) return

        pathPlanResultBean.m_iPathPlanType = CLEAN_PATH_PLAN

        // 限制路段数量，防止内存溢出
        val maxSegments = minOf(result.m_iPathSum, MAX_PATH_SEGMENTS)
        LogUtil.i("PAD解析PP数据 清扫路径路段个数：$maxSegments")
        LogUtil.i("PAD解析PP数据 清扫路径点位个数：${result.m_fElementBuffer.size}")

        // 解析路径数据
        parsePathData(result, pathPlanResultBean)
    }

    /**
     * 处理示教路线规划
     */
    private fun handleTeachPathPlan(result: PlanPathResult, pathPlanResultBean: PathPlanResultBean, mMapView: MapView?) {
        if (result.m_iPlanResultMode != PATH_MODE) return

        // 限制路段数量，防止内存溢出
        val maxSegments = minOf(result.m_iPathSum, MAX_PATH_SEGMENTS)
        // 检查数据点数量是否足够
        if (maxSegments * 8 > result.m_fElementBuffer.size) return

        pathPlanResultBean.m_iPathPlanType = TEACH_PATH_PLAN

        LogUtil.i("PAD解析PP数据 示教路径路段个数：$maxSegments")
        LogUtil.i("PAD解析PP数据 示教路径点位个数：${result.m_fElementBuffer.size}")

        // 路段个数
        var j = 0
        for (i in 0 until maxSegments) {
            // 使用对象池获取Point2d对象
            val pptKey = arrayOf(
                obtainPoint2d(),
                obtainPoint2d(),
                obtainPoint2d(),
                obtainPoint2d()
            )

            pptKey[0].x = result.m_fElementBuffer[j++]
            pptKey[0].y = result.m_fElementBuffer[j++]
            pptKey[1].x = result.m_fElementBuffer[j++]
            pptKey[1].y = result.m_fElementBuffer[j++]
            pptKey[2].x = result.m_fElementBuffer[j++]
            pptKey[2].y = result.m_fElementBuffer[j++]
            pptKey[3].x = result.m_fElementBuffer[j++]
            pptKey[3].y = result.m_fElementBuffer[j++]

            val bezier = Bezier(4, pptKey)
            pathPlanResultBean.m_vecBezierOfPathPlan.add(bezier)
            mMapView?.createPathTeach(pptKey, 2)//单项双向

            // 回收Point2d对象
            pptKey.forEach { recyclePoint2d(it) }
        }
    }

    /**
     * 解析路径数据 - 提取重复代码，减少重复创建对象
     */
    private fun parsePathData(result: PlanPathResult, pathPlanResultBean: PathPlanResultBean) {
        // 限制路段数量，防止内存溢出
        val maxSegments = minOf(result.m_iPathSum, MAX_PATH_SEGMENTS)
        // 检查数据点数量是否足够
        if (maxSegments * 8 > result.m_fElementBuffer.size) return

        // 路段个数
        var j = 0
        for (i in 0 until maxSegments) {
            // 优化：检查数据边界，避免数组越界
            if (j >= result.m_cPathTypeBuffer.size) break

            when (result.m_cPathTypeBuffer[i].toInt()) {
                PATH_LINE -> {
                    // 直线 - 检查数据边界
                    if (j + 3 >= result.m_fElementBuffer.size) break

                    // 使用对象池获取PointNew对象
                    val ptStart = obtainPointNew()
                    val ptEnd = obtainPointNew()
                    ptStart.X = result.m_fElementBuffer[j++]
                    ptStart.Y = result.m_fElementBuffer[j++]
                    ptEnd.X = result.m_fElementBuffer[j++]
                    ptEnd.Y = result.m_fElementBuffer[j++]

                    pathPlanResultBean.m_vecLineOfPathPlan.add(LineNew(ptStart, ptEnd))

                    // 回收PointNew对象
                    recyclePointNew(ptStart)
                    recyclePointNew(ptEnd)
                }
                PATH_BEZIER -> {
                    // 贝塞尔 - 检查数据边界
                    if (j + 7 >= result.m_fElementBuffer.size) break

                    // 使用对象池获取Point2d对象
                    val pptKey = arrayOf(
                        obtainPoint2d(),
                        obtainPoint2d(),
                        obtainPoint2d(),
                        obtainPoint2d()
                    )

                    pptKey[0].x = result.m_fElementBuffer[j++]
                    pptKey[0].y = result.m_fElementBuffer[j++]
                    pptKey[1].x = result.m_fElementBuffer[j++]
                    pptKey[1].y = result.m_fElementBuffer[j++]
                    pptKey[2].x = result.m_fElementBuffer[j++]
                    pptKey[2].y = result.m_fElementBuffer[j++]
                    pptKey[3].x = result.m_fElementBuffer[j++]
                    pptKey[3].y = result.m_fElementBuffer[j++]

                    pathPlanResultBean.m_vecBezierOfPathPlan.add(Bezier(4, pptKey))

                    // 回收Point2d对象
                    pptKey.forEach { recyclePoint2d(it) }
                }
            }
        }
    }
}