package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent


//定位区域
data class PositingArea(
    var id: Long,
    var slamMode: Int,
    var longCorridorMode: Int,
    var topViewFusion: Int,
    var mapId: Int,
    var start: Start,
    var end: End,
    var isEdit: Boolean,
) : LiveEvent

data class Start(
    var x: Float,
    var y: Float,
) : LiveEvent

data class End(
    var x: Float,
    var y: Float,
) : LiveEvent