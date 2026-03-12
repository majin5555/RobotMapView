package com.siasun.dianshi.bean

/******************************************
 * 类描述：
 *
 * @author: why
 * @time: 2026/3/12 13:51
 ******************************************/

data class SameSwitchBean(
    var id: Int,
    var name: String ? = null,
    var coordinate: StationCoordinate ? = null,
)