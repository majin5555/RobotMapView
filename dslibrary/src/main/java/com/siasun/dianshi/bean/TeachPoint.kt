package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent

data class TeachPoint(
    var x: Double = 0.0, var y: Double = 0.0, var theta: Double = 0.0
) : LiveEvent