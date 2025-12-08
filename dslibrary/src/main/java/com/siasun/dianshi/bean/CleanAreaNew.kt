package com.siasun.dianshi.bean

import android.graphics.PointF
import com.alibaba.fastjson.annotation.JSONField
import com.jeremyliao.liveeventbus.core.LiveEvent
import java.util.Vector

/**
 * 清扫子区域
 */
class CleanAreaNew : LiveEvent {
    var sub_name = "" // 区域名称，同一层内不可重复
    var regId = -1 // 区域编号
    var layer_id = -1 // 层号（混行-1，非混行从0开始）
    var endPoint = -1
    var m_VertexPnt = Vector<PointNew>()
    var areaStartPoint = PointF()
    var areaPathType = 0 //0-普通清扫区域  1-狭窄区域
    var pathPlanInfo: PathPlanInfo? = null //不知道啥意思 不传还不对
    var cleanShape = 4 //4-回字型 3-弓字形  6：混合型

    /**
     * 区域类型
     * 0 -自动生成
     * 1-(自定义)手动生成
     * 2-示教路线
     * 3-过门 （配置loraID）
     * 4-工作区
     * 11-过门（不配置loraID）
     *
     *
     * 暂时拿routeType 当作区域类型
     */
    var routeType = 0
    var areaType = 1



    override fun toString(): String {
        return "CleanArea{sub_name='$sub_name', regId=$regId, layer_id=$layer_id, endPoint=$endPoint, m_VertexPnt=$m_VertexPnt, areaStartPoint=$areaStartPoint, areaPathType=$areaPathType, pathPlanInfo=$pathPlanInfo, cleanShape=$cleanShape, routeType=$routeType, areaType=$areaType}"
    }
}
