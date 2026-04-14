package com.siasun.dianshi.bean

import android.graphics.PointF
import com.jeremyliao.liveeventbus.core.LiveEvent

/**
 * 过门
[
{
"name": "lora模块",
"type": "door_lora"
},
{
"name": "新松过门模块",
"type": "door_siasun"
},
{
"name": "海康过门模块",
"type": "door_hk"
}
]
 */

data class CrossDoor(
    val id: Int,
    val map_id: Int,
    var door_msg: DoorMsg,
    var start_point: PointF = PointF(),
    var end_point: PointF = PointF()
) : LiveEvent

data class DoorMsg(
    var type: String,
    var door_lora: DoorLora? = DoorLora(),
    var door_hk: DoorHk? = DoorHk(),
    var door_siasun: DoorSiaSun? = DoorSiaSun(),
) : LiveEvent

data class DoorLora(
    var lora_id: Int = 0 //loraID
) : LiveEvent

data class DoorHk(
    var device_num: String = "",//设备编号
    var ip: String = "",//IP
    var port: Int = 0//端口
) : LiveEvent

data class DoorSiaSun(
    var door_sn: String = ""
) : LiveEvent

