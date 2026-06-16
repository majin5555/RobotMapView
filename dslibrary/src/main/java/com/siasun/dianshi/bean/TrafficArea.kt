package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent

data class TrafficArea(
    var id: Int = 0,
    var name: String = "",
    var areaVertexPnt: MutableList<PointNew> = mutableListOf(),
) : LiveEvent

data class TrafficAreaRoot(
    var trafficAreasList: MutableList<TrafficArea> = mutableListOf()
) : LiveEvent
