package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent
import java.util.Vector

/**
 * 特殊区域
 */
class SpArea : LiveEvent {
    var sub_name = "" // 区域名称，同一层内不可重复
    var regId = -1 // 区域编号
    var layer_id = -1 // 层号（混行-1，非混行从0开始）
    var m_VertexPnt = Vector<PointNew>()

    /**
     * 区域类型
     * 5-避障屏蔽区
     * 6-相机屏蔽区
     * 7-货位区
     * 8-感知特殊功能屏蔽与参数调整区
     * 9-声呐屏蔽区
     * 10-限制区
     *
     *
     * 暂时拿routeType 当作区域类型
     */
    var routeType = 0
    override fun toString(): String {
        return "SpArea(sub_name='$sub_name', regId=$regId, layer_id=$layer_id, m_VertexPnt=$m_VertexPnt, routeType=$routeType)"
    }

}
