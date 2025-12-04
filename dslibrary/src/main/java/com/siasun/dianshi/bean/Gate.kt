package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent

/**
 * 准备点
 */

data class Gate(var x: Float = 0f, var y: Float = 0f, var theta: Float = 0f) : LiveEvent


