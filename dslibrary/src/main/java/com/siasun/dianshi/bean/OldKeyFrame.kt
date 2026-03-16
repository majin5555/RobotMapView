package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent

/**
 * 扩展地图 获取之前的关键帧数据
 *
 */
data class OldKeyFrame(val x: Float, val y: Float, val theta: Float = 0f, val id: Int) : LiveEvent
