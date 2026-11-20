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
    // 原始激光坐标：仅"回环重算位姿"时需要，长时间建图时会被释放（置 null）以省一半堆内存
    var cloudPoints: FloatArray?,
    var worldPoints: FloatArray?,
    val robotPos: FloatArray
) {
    /**
     * 点数量（每对坐标算一个点）。
     * 必须用 worldPoints 计算：cloudPoints 会在建图变长后被释放，用它会让 pointCount 变成 0，
     * 表现为"老的点云突然消失"。
     */
    val pointCount: Int
        get() = worldPoints?.size?.div(2) ?: 0
}