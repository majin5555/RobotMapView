package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent

/**
过渡点配置
 */
data class PassPoints(
    var id: String = "", //过度点名称
    var floor: Int = 0, //层号
    var next_area_id: MutableList<String> = mutableListOf(), //前往区域id
    var locate: Locate = Locate(),//停车点
    var gate: Gate = Gate(), //准备点
    var flagId: Int = 0,//hashcode  唯一的
    var next_area_name: MutableList<String> = mutableListOf() //前往区域名称)
) : LiveEvent



