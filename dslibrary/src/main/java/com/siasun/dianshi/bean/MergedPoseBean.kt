package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent

/**
 * 顶视路线解析类
 */
data class MergedPoseBean(val data: MutableList<MergedPoseItem>) : LiveEvent

data class MergedPoseItem(val x: Double, val y: Double)