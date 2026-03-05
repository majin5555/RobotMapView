package com.siasun.dianshi.bean


/******************************************
 * 类描述：
 *
 * @author: why
 * @time: 2026/3/5 14:45
 ******************************************/

data class RFID(
    var tId: String? = null,
    var tagX: Float = 0F,
    var tagY: Float = 0F,
)