package com.siasun.dianshi.bean

import android.graphics.PointF
import com.jeremyliao.liveeventbus.core.LiveEvent

/**
 * 过门
 */

data class CrossDoor(
    val id: Int,
    val map_id: Int,
    val door_msg: DoorMsg,
    var start_point: PointF = PointF(),
    var end_point: PointF = PointF()
) : LiveEvent


data class DoorMsg(
    val door_sn: String,
    val type: String,
) : LiveEvent
