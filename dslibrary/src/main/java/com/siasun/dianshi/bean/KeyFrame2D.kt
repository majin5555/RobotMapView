package com.siasun.dianshi.bean

/**
 * 关键帧数据 points 点数据  robotPos 机器人位姿
 *
 */
data class KeyFrame2D(val robotPos: FloatArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyFrame2D

        if (!robotPos.contentEquals(other.robotPos)) return false

        return true
    }

    override fun hashCode(): Int {
        return robotPos.contentHashCode()
    }
}


