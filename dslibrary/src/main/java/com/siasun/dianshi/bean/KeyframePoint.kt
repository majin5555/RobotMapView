package com.siasun.dianshi.bean

/**
 * 关键帧数据地图
 */
data class KeyframePoint(
    //点云位于机器人的坐标  用于回环检测
    var cloudX: Float, var cloudY: Float,
    //点云位于世界坐标  用于实时显示建图轮廓
    var x: Float, var y: Float
)