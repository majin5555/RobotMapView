package com.siasun.dianshi.bean

import com.jeremyliao.liveeventbus.core.LiveEvent

/***
 * 上线点
 */

data class InitPoseRoot(var Initposes: MutableList<InitPose> = ArrayList()) : LiveEvent

data class InitPose(
    var name: String = "",
    var Layer: Int = 0,
    var initPos: List<Float> = ArrayList()
) : LiveEvent