package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent
import com.siasun.dianshi.bean.RC.RCData

data class DragLocationBean(
    var upRCData: RCData? = null
) : LiveEvent
