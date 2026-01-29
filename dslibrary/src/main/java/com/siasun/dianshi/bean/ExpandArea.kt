package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent


//扩展区域
data class ExpandArea(
    var start: Start,
    var end: End,
) : LiveEvent


