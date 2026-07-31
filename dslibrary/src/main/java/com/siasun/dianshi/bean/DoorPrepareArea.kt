package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent
import java.util.Vector

/**
 * 过门准备区
 *
 * JSON 格式：
 * {
 *   "regId": 1,
 *   "sub_name": "tt",
 *   "routeType": 7,
 *   "m_VertexPnt": [
 *     {"X": 30.929014, "Y": 34.80917},
 *     ...
 *   ],
 *   "data": {
 *     "param_value": 1
 *   }
 * }
 */
class DoorPrepareArea : LiveEvent {
    var sub_name = "" // 区域名称，同一层内不可重复
    var regId = -1 // 区域编号
    var routeType = 7 // 区域类型 7-过门准备区
    var m_VertexPnt = Vector<PointNew>()
    var data: DoorPrepareAreaData? = null

    override fun toString(): String {
        return "DoorPrepareArea(sub_name='$sub_name', regId=$regId, routeType=$routeType, m_VertexPnt=$m_VertexPnt, data=$data)"
    }
}

/**
 * 过门准备区参数数据
 */
data class DoorPrepareAreaData(
    var param_value: Int = 0 // 参数值
) : LiveEvent
