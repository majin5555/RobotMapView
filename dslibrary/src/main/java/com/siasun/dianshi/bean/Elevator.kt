package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent


data class ElevatorRoot(var elevators: MutableList<ElevatorPoint>) : LiveEvent

/**
 * 乘梯点
 *     String name
 *     PstParkBean pstPark //梯内
 *     GatePointBean gatePoint//梯外
 *     WaitPointBean waitPoint //等待
 *     int startFloor//开始楼层
 *     int endFloor;//结束楼层
 */
data class ElevatorPoint(
    var name: String? = null,
    var pstPark: PstParkBean? = null,
    var gatePoint: GatePointBean? = null,
    var waitPoint: WaitPointBean? = null,
    var startFloor: Int = 0,
    var endFloor: Int = 0,
) : LiveEvent

//梯内
data class PstParkBean(
    var x: Float,
    var y: Float,
    var theta: Float,
    var z: Float,
    var roll: Float,
    var pitch: Float,
) : LiveEvent

//梯外
data class GatePointBean(
    var x: Float,
    var y: Float,
    var theta: Float,
    var z: Float,
    var roll: Float,
    var pitch: Float,
) : LiveEvent

//等待
data class WaitPointBean(
    var x: Float,
    var y: Float,
    var theta: Float,
    var z: Float,
    var roll: Float,
    var pitch: Float,
) : LiveEvent