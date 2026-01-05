package com.siasun.dianshi.mapviewdemo.utils

import android.graphics.PointF
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
import com.siasun.dianshi.mapviewdemo.utils.PathPlanningUtil.MAX_PATH_SEGMENTS
import com.siasun.dianshi.mapviewdemo.utils.PathPlanningUtil.obtainPoint2d
import com.siasun.dianshi.view.MapView

/**
 * @Author: CheFuX1n9
 * @Date: 2024/6/3 16:39
 * @Description: 路径规划工具类
 */
object PathPlanningUtil1 {
    var mGlobalPathPlanResultBean: PathPlanResultBean? = null
    var mCleanPathPlanResultBean: PathPlanResultBean? = null

    @RequiresApi(Build.VERSION_CODES.R)
    fun getPathPlanResultBean(result: PlanPathResult): PathPlanResultBean {
        return getPathPlanResultBean(result, null)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    fun getPathPlanResultBean(result: PlanPathResult, mMapView: MapView?): PathPlanResultBean {
        val pathPlanResultBean = PathPlanResultBean()
        val mRes = result.m_iPlanResult.toByte()
        if ((mRes.toInt() shr 0) and 1 == 1) {
            pathPlanResultBean.m_bIsPlanOk = true
            pathPlanResultBean.m_iPathPlanId = result.m_iRegionNumber

            if (result.m_iPathPlanType == GLOBAL_PATH_PLAN) {
                // 全局路径规划 - 接收PP返回CMS结果
                if (result.m_strTo == "CMS") {
                    pathPlanResultBean.m_iPathPlanType = GLOBAL_PATH_PLAN
                    // 路段模式
                    if (result.m_iPlanResultMode == PATH_MODE) {
                        // 全局路径规划起点坐标
                        if (result.m_fGloalPathPlanStartPosBuffer.isNotEmpty()) {
                            // 0:x, 1:y, 2:fthita
                            result.m_fGloalPathPlanStartPosBuffer.forEachIndexed { index, fl ->
                                pathPlanResultBean.startPoint[index] = fl
                            }
                        }
                        // 全局路径规划终点坐标
                        if (result.m_fGloalPathPlanGoalPosBuffer.isNotEmpty()) {
                            // 0:x, 1:y, 2:fthita
                            result.m_fGloalPathPlanGoalPosBuffer.forEachIndexed { index, fl ->
                                pathPlanResultBean.endPoint[index] = fl
                            }
                        }

                        LogUtil.i("PAD解析PP数据 全局路径路段个数：${result.m_iPathSum}")
                        LogUtil.i("PAD解析PP数据 全局路径点位个数：${result.m_fElementBuffer.size}")
                        // 路段个数
                        var j = 0
                        for (i in 0 until result.m_iPathSum) {
                            if (result.m_cPathTypeBuffer[i].toInt() == PATH_LINE) {
                                // 直线
                                val ptStart = PointNew()
                                val ptEnd = PointNew()
                                ptStart.X = result.m_fElementBuffer[j++]
                                ptStart.Y = result.m_fElementBuffer[j++]
                                ptEnd.X = result.m_fElementBuffer[j++]
                                ptEnd.Y = result.m_fElementBuffer[j++]
                                val line = LineNew(ptStart, ptEnd)
                                pathPlanResultBean.m_vecLineOfPathPlan.add(line)
                            } else if (result.m_cPathTypeBuffer[i].toInt() == PATH_BEZIER) {
                                // 贝塞尔
                                val pptKey = arrayOf(
                                    Point2d(), Point2d(), Point2d(), Point2d()
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
                            }
                        }
                    }
                }
            } else if (result.m_iPathPlanType == CLEAN_PATH_PLAN) {
                // 清扫路径规划 - 接收PP返回pad结果
                if (result.m_strTo == "CMS" || result.m_strTo == "pad") {
                    pathPlanResultBean.m_iPathPlanType = CLEAN_PATH_PLAN
                    // 路段模式
                    if (result.m_iPlanResultMode == PATH_MODE) {
                        LogUtil.i("PAD解析PP数据 清扫路径路段个数：${result.m_iPathSum}")
                        LogUtil.i("PAD解析PP数据 清扫路径点位个数：${result.m_fElementBuffer.size}")
                        // 路段个数
                        var j = 0
                        for (i in 0 until result.m_iPathSum) {
                            if (result.m_cPathTypeBuffer[i].toInt() == PATH_LINE) {
                                // 直线
                                val ptStart = PointNew()
                                val ptEnd = PointNew()
                                ptStart.X = result.m_fElementBuffer[j++]
                                ptStart.Y = result.m_fElementBuffer[j++]
                                ptEnd.X = result.m_fElementBuffer[j++]
                                ptEnd.Y = result.m_fElementBuffer[j++]
                                val line = LineNew(ptStart, ptEnd)
                                pathPlanResultBean.m_vecLineOfPathPlan.add(line)
                            } else if (result.m_cPathTypeBuffer[i].toInt() == PATH_BEZIER) {
                                // 贝塞尔
                                val pptKey = arrayOf(
                                    Point2d(), Point2d(), Point2d(), Point2d()
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
                            }
                        }
                    }
                }
            } else if (result.m_iPathPlanType == TEACH_PATH_PLAN) {
                // 示教路线规划
                pathPlanResultBean.m_iPathPlanType = TEACH_PATH_PLAN
                // 路段模式
                if (result.m_iPlanResultMode == PATH_MODE) {
                    // 限制路段数量，防止内存溢出
                    val maxSegments = minOf(result.m_iPathSum, MAX_PATH_SEGMENTS)
                    LogUtil.i("PAD解析PP数据 示教路径路段个数：${result.m_iPathSum}")
                    LogUtil.i("PAD解析PP数据 示教路径点位个数：${result.m_fElementBuffer.size}")
                    // 收集所有路径段的关键点数组
                    val pathSegments = mutableListOf<Array<Point2d>>()

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

                        // 移除这一行，因为会导致重复绘制路径
                        val bezier = Bezier(4, pptKey)
                        pathPlanResultBean.m_vecBezierOfPathPlan.add(bezier)

                        // 将当前路径段添加到列表中
                        pathSegments.add(pptKey)
                    }
                    mMapView?.createContinuousPathTeach(pathSegments,2)
                }
            }
        } else {
            pathPlanResultBean.m_bIsPlanOk = false
        }
        return pathPlanResultBean
    }
}