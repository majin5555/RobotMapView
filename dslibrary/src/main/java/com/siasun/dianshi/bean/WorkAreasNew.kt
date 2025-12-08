package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent

/**
 * 混行区配置
 */
data class WorkAreasNew(
    var id: String = "", //混行区id
    var floor: Int = 0, //楼层号
    var ip: String = "",//混行区ip
    var port: Int = 0,//混行区端口
    var name: String = "", //混行区名称
    var tc: String = "",//混行区tc
    var areaVertexPnt: MutableList<PointNew> = mutableListOf(), //混行区点
    var passPointsList: MutableList<PassPoints> = ArrayList(), //混行区过渡点
) : LiveEvent




