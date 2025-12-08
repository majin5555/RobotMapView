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

/**
 * @Author: CheFuX1n9
 * @Date: 2024/6/3 16:39
 * @Description: 路径规划工具类
 */
/**
 * 路径规划工具类
 * 优化：移除静态变量，避免内存泄漏
 */
object PathPlanningUtil {

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
        val pathPlanResultBean = PathPlanResultBean()
        val mRes = result.m_iPlanResult.toByte()
        
        if ((mRes.toInt() shr 0) and 1 == 1) {
            pathPlanResultBean.m_bIsPlanOk = true
            pathPlanResultBean.m_iPathPlanId = result.m_iRegionNumber

            when (result.m_iPathPlanType) {
                GLOBAL_PATH_PLAN -> handleGlobalPathPlan(result, pathPlanResultBean)
                CLEAN_PATH_PLAN -> handleCleanPathPlan(result, pathPlanResultBean)
                TEACH_PATH_PLAN -> handleTeachPathPlan(result, pathPlanResultBean, mMapView)
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
        if (result.m_strTo == "CMS" && result.m_iPlanResultMode == PATH_MODE) {
            pathPlanResultBean.m_iPathPlanType = GLOBAL_PATH_PLAN
            
            // 设置起点坐标
            result.m_fGloalPathPlanStartPosBuffer.take(3).forEachIndexed { index, fl ->
                pathPlanResultBean.startPoint[index] = fl
            }
            
            // 设置终点坐标
            result.m_fGloalPathPlanGoalPosBuffer.take(3).forEachIndexed { index, fl ->
                pathPlanResultBean.endPoint[index] = fl
            }

            LogUtil.i("PAD解析PP数据 全局路径路段个数：${result.m_iPathSum}")
            LogUtil.i("PAD解析PP数据 全局路径点位个数：${result.m_fElementBuffer.size}")
            
            // 解析路径数据
            parsePathData(result, pathPlanResultBean)
        }
    }
    
    /**
     * 处理清扫路径规划
     */
    private fun handleCleanPathPlan(result: PlanPathResult, pathPlanResultBean: PathPlanResultBean) {
        // 清扫路径规划 - 接收PP返回pad结果
        if ((result.m_strTo == "CMS" || result.m_strTo == "pad") && result.m_iPlanResultMode == PATH_MODE) {
            pathPlanResultBean.m_iPathPlanType = CLEAN_PATH_PLAN
            
            LogUtil.i("PAD解析PP数据 清扫路径路段个数：${result.m_iPathSum}")
            LogUtil.i("PAD解析PP数据 清扫路径点位个数：${result.m_fElementBuffer.size}")
            
            // 解析路径数据
            parsePathData(result, pathPlanResultBean)
        }
    }
    
    /**
     * 处理示教路线规划
     */
    private fun handleTeachPathPlan(result: PlanPathResult, pathPlanResultBean: PathPlanResultBean, mMapView: MapView?) {
        if (result.m_iPlanResultMode == PATH_MODE) {
            pathPlanResultBean.m_iPathPlanType = TEACH_PATH_PLAN
            
            LogUtil.i("PAD解析PP数据 示教路径路段个数：${result.m_iPathSum}")
            LogUtil.i("PAD解析PP数据 示教路径点位个数：${result.m_fElementBuffer.size}")
            
            // 路段个数
            var j = 0
            for (i in 0 until result.m_iPathSum) {
                // 优化：检查数据边界，避免数组越界
                if (j + 7 >= result.m_fElementBuffer.size) break
                
                val pptKey = arrayOf(Point2d(), Point2d(), Point2d(), Point2d())
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
                mMapView?.createPathTeach(pptKey, 1)
            }
        }
    }
    
    /**
     * 解析路径数据 - 提取重复代码，减少重复创建对象
     */
    private fun parsePathData(result: PlanPathResult, pathPlanResultBean: PathPlanResultBean) {
        // 路段个数
        var j = 0
        for (i in 0 until result.m_iPathSum) {
            // 优化：检查数据边界，避免数组越界
            if (j >= result.m_cPathTypeBuffer.size) break
            
            when (result.m_cPathTypeBuffer[i].toInt()) {
                PATH_LINE -> {
                    // 直线 - 检查数据边界
                    if (j + 3 >= result.m_fElementBuffer.size) break
                    
                    val ptStart = PointNew()
                    val ptEnd = PointNew()
                    ptStart.X = result.m_fElementBuffer[j++]
                    ptStart.Y = result.m_fElementBuffer[j++]
                    ptEnd.X = result.m_fElementBuffer[j++]
                    ptEnd.Y = result.m_fElementBuffer[j++]
                    pathPlanResultBean.m_vecLineOfPathPlan.add(LineNew(ptStart, ptEnd))
                }
                PATH_BEZIER -> {
                    // 贝塞尔 - 检查数据边界
                    if (j + 7 >= result.m_fElementBuffer.size) break
                    
                    val pptKey = arrayOf(Point2d(), Point2d(), Point2d(), Point2d())
                    pptKey[0].x = result.m_fElementBuffer[j++]
                    pptKey[0].y = result.m_fElementBuffer[j++]
                    pptKey[1].x = result.m_fElementBuffer[j++]
                    pptKey[1].y = result.m_fElementBuffer[j++]
                    pptKey[2].x = result.m_fElementBuffer[j++]
                    pptKey[2].y = result.m_fElementBuffer[j++]
                    pptKey[3].x = result.m_fElementBuffer[j++]
                    pptKey[3].y = result.m_fElementBuffer[j++]
                    pathPlanResultBean.m_vecBezierOfPathPlan.add(Bezier(4, pptKey))
                }
            }
        }
    }
}