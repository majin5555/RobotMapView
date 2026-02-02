package com.siasun.dianshi.bean

/**
 * 关键帧数据 points 点数据  robotPos 机器人位姿
 *
 */
data class KeyFrame(val points: MutableList<KeyframePoint>?, val robotPos: FloatArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as KeyFrame

        if (points != other.points) return false
        if (!robotPos.contentEquals(other.robotPos)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = points?.hashCode() ?: 0
        result = 31 * result + robotPos.contentHashCode()
        return result
    }
}
