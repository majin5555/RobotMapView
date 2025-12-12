package com.siasun.dianshi.utils

import android.graphics.PointF
import com.siasun.dianshi.bean.MapData

/**
 * 坐标转化工具类
 */
class CoordinateConversion {
    @JvmField
    var scale = 1f//缩放比例
    var mapData = MapData()//地图数据
    // ======================================
    // 坐标转换方法
    // ======================================
    /**
     * 世界坐标转屏幕坐标
     *
     * @param wx 世界坐标X
     * @param wy 世界坐标Y
     * @return 屏幕坐标PointF
     */
    fun worldToScreen(wx: Float, wy: Float): PointF {
        val px = (wx - mapData.originX) / mapData.resolution
        val py = mapData.height - (wy - mapData.originY) / mapData.resolution
        return PointF(px, py)
    }

    /**
     * 屏幕坐标转世界坐标
     *
     * @param sx 屏幕坐标X
     * @param sy 屏幕坐标Y
     * @return 世界坐标PointF
     */
    fun screenToWorld(sx: Float, sy: Float): PointF {
        val wx = sx * mapData.resolution + mapData.originX
        val wy = (mapData.height - sy) * mapData.resolution + mapData.originY
        return PointF(wx, wy)
    }

    override fun toString(): String {
        return "CoordinateConversion{mapData=$mapData}"
    }
}
