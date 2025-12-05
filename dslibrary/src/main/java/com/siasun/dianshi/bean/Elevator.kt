package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent


data class ElevatorRoot(var elevators: MutableList<ElevatorPoint>) : LiveEvent

/**
 * 乘梯点
 *     String name;
 *     PstParkBean pstPark;//梯内
 *     GatePointBean gatePoint;//梯外
 *     WaitPointBean waitPoint;//等待
 *     int startFloor;//开始楼层
 *     int endFloor;//结束楼层
 */
data class ElevatorPoint(
    var name: String,
    var pstPark: PstParkBean?,
    var gatePoint: GatePointBean?,
    var waitPoint: WaitPointBean?,
    var startFloor: Int,
    var endFloor: Int,
) : LiveEvent

//梯内
data class PstParkBean(
    var x: Float,
    var y: Float,
    var theta: Float,
) : LiveEvent

//梯外
data class GatePointBean(
    var x: Float,
    var y: Float,
    var theta: Float,
) : LiveEvent

//等待
data class WaitPointBean(
    var x: Float,
    var y: Float,
    var theta: Float,
) : LiveEvent

