package com.siasun.dianshi.bean

import android.graphics.PointF
import com.jeremyliao.liveeventbus.core.LiveEvent


//扩展区域 回调的数是世界坐标
data class ExpandArea(var start: PointF, var end: PointF) : LiveEvent


