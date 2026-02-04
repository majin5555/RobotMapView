package com.siasun.dianshi.view.createMap

import android.graphics.PointF
import com.siasun.dianshi.view.WorkMode

/**
 * 地图视图接口，定义了mapView、 2D 和 3D 地图视图共有的方法
 */
interface MapViewInterface {
    /**
     * 世界坐标转屏幕坐标
     */
    fun worldToScreen(x: Float, y: Float): PointF

    /**
     * 屏幕坐标转世界坐标
     */
    fun screenToWorld(x: Float, y: Float): PointF

    /**
     * 获取当前工作模式
     */
    fun getCurrentWorkMode(): WorkMode

    /**
     * 机器人位姿 [x, y, theta(rad)，z roll pitch]
     */
    val robotPose: FloatArray
}