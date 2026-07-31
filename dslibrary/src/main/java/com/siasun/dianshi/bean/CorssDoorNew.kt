package com.siasun.dianshi.bean

import android.graphics.PointF
import com.jeremyliao.liveeventbus.core.LiveEvent
import com.alibaba.fastjson.annotation.JSONField

/**
 * 过门
 *
 *       "door_type": "frame",	#屏蔽门/风淋门：screen, 电梯门/闸机门：twin
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
    @JSONField(ordinal = 1)
    val map_id: Int,

    @JSONField(ordinal = 2)
    val id: Int,

    @JSONField(ordinal = 3)
    var door_type: String,

    @JSONField(ordinal = 4)
    var start_point: PointF = PointF(),

    @JSONField(ordinal = 5)
    var end_point: PointF = PointF(),

    @JSONField(ordinal = 6)
    var door_msg: DoorMsg? = null,

    @JSONField(ordinal = 7)
    var turnstile: Int? = null,

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

