package com.siasun.dianshi.bean

/******************************************
 * 类描述：
 *
 * @author: why
 * @time: 2025/8/18 15:45
 ******************************************/


/**
 * 一体机
 */
data class MachineStation(
    var id: String? = null,
    var mapId: Int = 0,
    @JvmField
    var locate: Locate? = null, //一体机对接点
    @JvmField
    var gate: Gate? = null, //对接一体机 准备点
    @JvmField
    var wait: MachineWait? = null, //等待点
    var finish: StationCoordinate? = null, //充电结束停放点
    var charge: Int = 2, // 充电类型 1:仅充电 2:充电加水
    var loraID: Int = 0
)

/**
 * 避障点
 */
data class CmsStation(
    var id: String? = null,
    var evName: String? = null,
    var mapId: Int = 0,
    var type: Int = 1,
    var coordinate: StationCoordinate? = null,
    var isRotate: Boolean = false
)

/**
 * 一体机等待点
 */
data class MachineWait(
    var time: Int = 0,
    var coordinate: StationCoordinate? = null
)

/**
 * 站点坐标通用
 */
data class StationCoordinate(
    var x: Float = 0F,
    var y: Float = 0F,
    var theta: Float = 0F,
)



