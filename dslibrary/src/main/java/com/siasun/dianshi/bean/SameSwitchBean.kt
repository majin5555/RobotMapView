package com.siasun.dianshi.bean

import org.apache.commons.math3.stat.inference.TestUtils.t

/******************************************
 * 类描述：
 *
 * @author: why
 * @time: 2026/3/12 13:51
 ******************************************/

data class SameSwitchBean(
    var point_id: String,
    var point_name: String? = null,
    var target_map_id: Int? = null,
    var target_point_id: String? = null,
    var coordinate: StationCoordinate? = null,
    var is_bound: Boolean = false,
)