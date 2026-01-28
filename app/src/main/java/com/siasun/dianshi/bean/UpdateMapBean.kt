package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent

/**
 * 更新地图
 * 1环境预览  2 扫描新环境 3 环境扩展 4 去除噪点
 */
data class UpdateMapBean(var isSuccess: Boolean, var type: Int) : LiveEvent
