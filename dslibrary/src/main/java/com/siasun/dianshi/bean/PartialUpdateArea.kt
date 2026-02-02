package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent


//局部更新
data class PartialUpdateArea(
    var start: Start,
    var end: End,
) : LiveEvent
