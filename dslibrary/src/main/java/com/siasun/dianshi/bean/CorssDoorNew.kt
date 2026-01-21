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
    val start_point: PointF,
    val end_point: PointF
) : LiveEvent


data class DoorMsg(
    val door_sn: String,
    val type: String,
) : LiveEvent
