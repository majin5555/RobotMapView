package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent


data class LineNew(var ptStart: PointNew, var ptEnd: PointNew) : LiveEvent



