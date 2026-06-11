package com.siasun.dianshi.bean

/**
 * 关键帧数据（内存优化版）
 * 用原始类型数组存储点云，避免百万级对象导致的 GC 卡顿。
 *
 * @param cloudPoints 原始激光点坐标（交错存储：cloudX,cloudY,cloudX,cloudY…），可为空
 * @param worldPoints 转换后的世界坐标（交错存储），与 cloudPoints 长度相同
 * @param robotPos 机器人位姿 [x, y, theta]（弧度）
 */
class KeyFrame(
    val cloudPoints: FloatArray?,
    var worldPoints: FloatArray?,
    val robotPos: FloatArray
) {
    /** 点数量（每对坐标算一个点） */
    val pointCount: Int
        get() = if (cloudPoints != null) cloudPoints.size / 2 else 0
}