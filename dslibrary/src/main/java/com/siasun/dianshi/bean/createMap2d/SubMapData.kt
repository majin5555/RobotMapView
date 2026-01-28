package com.siasun.dianshi.bean.createMap2d

import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.PointF

/**
 * 子图数据
 */
class SubMapData {
    //子图ID
    var id = 0

    //分辨率
    var percent = 0.05f

    //子图 x方向格子数量 子图宽度
    var width = 0f

    //子图 y方向格子数量 子图高度
    var height = 0f

    //子图右上角世界坐标
    var originX = 0f
    var originY = 0f
    var originTheta = 0f

    //左上角物理坐标   (世界坐标系)
    var leftTop = PointF()


    //左下角物理坐标   (世界坐标系)
    var leftBottom = PointF()

    //右下角物理坐标   (世界坐标系)
    var rightBottom = PointF()

    //右上角物理坐标 (世界坐标系)
    var rightTop = PointF()

    var optMaxTempX = 0f
    var optMaxTempY = 0f
    var optMaxTempTheta = 0f

    //子图占据的栅格个数
    var indexCount = 0

    //所有概率点的集合
    var intensitiesList: MutableList<Int> = mutableListOf()

    //图片
    var mBitmap: Bitmap? = null

    var matrix: Matrix? = null
    override fun toString(): String {
        return "SubMapData(id=$id, percent=$percent, width=$width, height=$height, originX=$originX, originY=$originY, originTheta=$originTheta, leftTop=$leftTop, leftBottom=$leftBottom, rightBottom=$rightBottom, rightTop=$rightTop, optMaxTempX=$optMaxTempX, optMaxTempY=$optMaxTempY, optMaxTempXTheta=$optMaxTempTheta, indexCount=$indexCount, intensitiesList=$intensitiesList, mBitmap=$mBitmap, matrix=$matrix)"
    }

}
