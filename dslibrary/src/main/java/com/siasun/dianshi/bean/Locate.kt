package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent

data class Locate(var x: Float = 0f, var y: Float = 0f, var theta: Float = 0f) : LiveEvent
