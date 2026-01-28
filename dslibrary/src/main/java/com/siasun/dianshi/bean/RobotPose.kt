package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent

/**
 * 机器人位姿
 */
data class RobotPose(
    var x: Float,
    var y: Float,
    var t: Float,
    var z: Float = 0f,
    var roll: Float = 0f,
    var pitch: Float = 0f
) : LiveEvent
