package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent


//局部更新
data class PartialUpdateArea(
    var id: Long,
    var mapId: Int,
    var start: Start,
    var end: End,
    var isEdit: Boolean,
) : LiveEvent
