package com.siasun.dianshi.bean


/**
 * 切换地图
 */
/**
 * 切换地图
 */
data class SwitchMapBean(
    val mapId: Int,
    var x: Double,
    var y: Double,
    var theta: Double,
    var z: Double = 0.00,
    var roll: Double = 0.00,
    var pitch: Double = 0.00,
    //3d 模式 10：普通定位 11：拖拽定位
    //2d 为0
    var source: Int = 0,
)