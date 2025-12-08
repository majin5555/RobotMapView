package com.siasun.dianshi

/**
 * 区域类型
 */
object AreaType {
    const val AREA_AUTO = 0//自动
    const val AREA_MANUAL = 1//手动
    const val AREA_TEACH = 2//示教路线
    const val AREA_DOOR_LORIA_ID = 3//过门(配置loriaid)
    const val AREA_WORK = 4//工作区

    const val AREA_OBSTACLE_AVOIDANCE = 5//避障屏蔽区
    const val AREA_CAMERA_OFF = 6//相机屏蔽区
    const val AREA_CARGO_LOCATION = 7//货位区
    const val AREA_MEMORY_BLOCKING = 8//记忆屏蔽区
    const val AREA_SONAR_SHIELDING = 9//声呐屏蔽区
    const val AREA_RESTRICTED = 10//限制区
    const val AREA_DOOR_NO_LORIA_ID = 11//过门(不配置loriaid)

}
