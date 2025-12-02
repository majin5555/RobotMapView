package com.siasun.dianshi.utils;

import android.graphics.PointF;

import com.siasun.dianshi.bean.MapData;


/**
 * 坐标转化工具类
 */
public class CoordinateConversion {
    //View视图的宽
    public short m_uWidth;
    //View视图的高
    public short m_uHeight;

    public MapData mapData = new MapData();


    public CoordinateConversion() {

    }

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
    public PointF worldToScreen(Float wx, Float wy) {
        float px = (wx - mapData.originX) / mapData.resolution;
        float py = mapData.height - (wy - mapData.originY) / mapData.resolution;
        return new PointF(px, py);

    }

    /**
     * 屏幕坐标转世界坐标
     *
     * @param sx 屏幕坐标X
     * @param sy 屏幕坐标Y
     * @return 世界坐标PointF
     */
    public PointF screenToWorld(Float sx, Float sy) {
        float wx = sx * mapData.resolution + mapData.originX;
        float wy = (mapData.height - sy) * mapData.resolution + mapData.originY;
        return new PointF(wx, wy);
    }


    @Override
    public String toString() {
        return "CoordinateConversion{" + "m_uWidth=" + m_uWidth + ", m_uHeight=" + m_uHeight + ", mapData=" + mapData + '}';
    }
}
