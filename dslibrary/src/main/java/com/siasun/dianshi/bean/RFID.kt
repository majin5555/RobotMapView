package com.siasun.dianshi.bean

import android.R


/******************************************
 * 类描述：
 *
 * @author: why
 * @time: 2026/3/5 14:45
 ******************************************/

data class RFID(
    var robot_x: Float = 0.0F,
    var robot_y: Float = 0.0F,
    var robot_z: Float = 0.0F,
    var robot_roll: Float = 0.0F,
    var robot_pitch: Float = 0.0F,
    var robot_yaw: Float = 0.0F,
    var tag_x: Float = 0.0F,
    var tag_y: Float = 0.0F,
    var tId: String ? = null,
)