package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent

/**
 * 机器人坐标信息 3D
 */
data class RobotCoordinateBean(
    var x: Double = 0.0,
    var y: Double = 0.0,
    var t: Double = 0.0,
    var z: Double = 0.0,
    var roll: Double = 0.0,
    var pitch: Double = 0.0,
) : LiveEvent
