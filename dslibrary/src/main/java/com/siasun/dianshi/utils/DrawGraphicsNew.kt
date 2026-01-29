package com.siasun.dianshi.utils

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import com.siasun.dianshi.bean.KeyFrame
import com.siasun.dianshi.bean.PartialUpdateArea
import com.siasun.dianshi.bean.createMap2d.SubMapData
import java.util.concurrent.ConcurrentHashMap

/**
 * 绘制图形
 */
class DrawGraphicsNew {
    var mCanvas: Canvas = Canvas()
    private val drawMatrix = Matrix() // 复用矩阵对象提升性能
    private var mSrf = CoordinateConversion()//坐标转化工具类

    val mPaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        strokeWidth = 2f
        color = Color.BLACK
        isFilterBitmap = true
        isDither = true
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }

    // 绘图工具
    private val vimWallPaint = Paint().apply {
        isAntiAlias = true
        strokeWidth = 2f
    }


    private val greenPaint = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL
        strokeWidth = 5f
    }

    val redPaint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        strokeWidth = 3f
    }
    private val yellowPaint = Paint().apply {
        color = Color.YELLOW
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    fun setCoordinateConversion(mSrf: CoordinateConversion) {
        this.mSrf = mSrf
    }

    /**
     * 绘制3D建图关键帧
     */
    fun drawKeyFrames(
        keyFrames: ConcurrentHashMap<Int, KeyFrame>, mRotateAngle: Float, width: Int, height: Int
    ) {
        if (keyFrames.isNotEmpty()) {
            mCanvas.save()
            // 应用全局旋转（如果有）
            if (mRotateAngle != 0f) {
                mCanvas.rotate(-mRotateAngle, width / 2f, height / 2f)
            }
            keyFrames.values.forEach { frame ->
                val robotScreen = mSrf.worldToScreen(frame.robotPos.x, frame.robotPos.y)
                mCanvas.drawPoint(robotScreen.x, robotScreen.y, greenPaint) // 绘制关键帧位置

                frame.points?.let { points ->
                    val contour = points.map { mSrf.worldToScreen(it.x, it.y) }

                    // 创建用于绘制点的FloatArray
                    val pointArray = FloatArray(contour.size * 2)
                    contour.forEachIndexed { index, point ->
                        pointArray[index * 2] = point.x
                        pointArray[index * 2 + 1] = point.y
                    }

                    mCanvas.drawPoints(pointArray, mPaint.apply {
                        strokeWidth = 3f
                        style = Paint.Style.FILL
                    }) // 绘制轮廓点
                }
            }
        }
    }



    /**
     * 绘制上激光点云信息
     */
    fun drawCurrentPointCloud(upPointsCloud: MutableList<PointF>) {
        if (upPointsCloud.isNotEmpty()) {
            mCanvas.drawPoints(
                upPointsCloud.flatMap { point ->
                    val worldToScreen = mSrf.worldToScreen(point.x, point.y)
                    listOf(worldToScreen.x, worldToScreen.y)
                }.toFloatArray(), redPaint
            )
        }
    }

    /**
     * 绘制下激光点云信息
     */
    fun drawDownCurrentPointCloud(cloud: MutableList<PointF>) {
        if (cloud.isNotEmpty()) {
            mCanvas.drawPoints(
                cloud.flatMap { point ->
                    val worldToScreen = mSrf.worldToScreen(point.x, point.y)
                    listOf(worldToScreen.x, worldToScreen.y)
                }.toFloatArray(), yellowPaint
            )
        }
    }


    /**
     * 绘制机器人图标
     */
    fun drawRobot(robotBitmap: Bitmap?, pose: FloatArray) {
        robotBitmap?.let {
            val screenPos = mSrf.worldToScreen(pose[0], pose[1])

            val matrix = Matrix().apply {
                postTranslate(-it.width / 2f, -it.height / 2f) // 以中心为锚点
                postRotate(-Math.toDegrees(pose[2].toDouble()).toFloat()) // 旋转方向调整
                postTranslate(screenPos.x, screenPos.y)
            }
            mCanvas.drawBitmap(it, matrix, mPaint)
        }
    }

    /**
     * 绘制2D子图
      */
//    fun drawKeyFrames2d(keyFrames2d: ConcurrentHashMap<Int, SubMapData>) {
//        for ((matrixKey, mSubMapData) in keyFrames2d.entries) {
//            // 快速检查无效数据
//            val bitmap = mSubMapData.mBitmap ?: continue
//            // 绘制子图
//            mCanvas.drawBitmap(
//                bitmap, mSubMapData.matrix!!, mPaint
//            )
//        }
//    }

    /**
     * 绘制子图的边框、中心点和ID信息 测试专用
     */
    fun drawSubMapInfo(
        matrixKey: Int,
        screenLeftTop: PointF,
        screenRightBottom: PointF,
        subMapId: Int
    ) {
        // 设置边框样式
        mPaint.apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            color = getSubMapColor(matrixKey)
            strokeWidth = 2f
        }

        // 绘制边框
        mCanvas.drawRect(
            screenLeftTop.x, screenLeftTop.y, screenRightBottom.x, screenRightBottom.y, mPaint
        )

        // 绘制中心点
        val centerX = screenLeftTop.x + (screenRightBottom.x - screenLeftTop.x) / 2
        val centerY = screenLeftTop.y + (screenRightBottom.y - screenLeftTop.y) / 2
        mCanvas.drawCircle(centerX, centerY, 5f, mPaint)

        // 绘制子图ID
        mCanvas.drawText(
            "子图 $subMapId", screenRightBottom.x - 50, screenRightBottom.y + 20, mPaint
        )
    }

    /**
     * 根据子图索引获取对应的颜色 测试专用
     */
    private fun getSubMapColor(index: Int): Int {
        return when (index) {
            0 -> Color.MAGENTA
            1 -> Color.GREEN
            2 -> Color.BLUE
            3 -> Color.YELLOW
            4 -> Color.CYAN
            else -> Color.RED
        }
    }

    /**
     * 绘制局部更新区域
     */
    fun drawPartialUpdateArea(mPartialUpdateArea: PartialUpdateArea, width: Int) {
        mPaint.isAntiAlias = true
        mPaint.setColor(Color.YELLOW)
        mPaint.setAlpha(240)
        mPaint.style = Paint.Style.FILL_AND_STROKE
        mPaint.strokeWidth = width.toFloat()
        val pStart: PointF =
            mSrf.worldToScreen(mPartialUpdateArea.start.x, mPartialUpdateArea.start.y)
        val pEnd: PointF = mSrf.worldToScreen(mPartialUpdateArea.end.x, mPartialUpdateArea.end.y)
        drawRect(pStart, pEnd)
    }


    /**
     * ------------------------------------------------------------------------
     */
    /**
     * 绘制文本
     */
    private fun drawText(pnt: PointF, mNme: String) {
        mCanvas.drawText(mNme, (pnt.x + 15), (pnt.y + 15), mPaint)
    }

    /**
     * 绘制定点
     */
    private fun drawPoint(pnt: PointF) {
        mCanvas.drawPoint(pnt.x, pnt.y, mPaint)
    }

    /**
     * 绘制线
     */
    private fun drawLine(mStart: PointF, mEnd: PointF) {
        mCanvas.drawLine(
            mStart.x, mStart.y, mEnd.x, mEnd.y, mPaint
        )
    }

    /**
     * 绘制路径
     */
    private fun drawPath(path: Path) {
        mCanvas.drawPath(path, mPaint)
    }

    /**
     * 绘制圆
     */
    private fun drawCircle(mPoint: PointF, radius: Float) {
        mCanvas.drawCircle(mPoint.x, mPoint.y, radius, mPaint)
    }

    /**
     * 绘制等边三角形（朝上的）
     */
    private fun drawTriangle(mPoint: PointF, size: Float = 10f) {
        val path = Path().apply {
            moveTo(mPoint.x, mPoint.y - size) // 顶点（上）
            lineTo(mPoint.x - size, mPoint.y + size) // 左下
            lineTo(mPoint.x + size, mPoint.y + size) // 右下
            close()
        }
        mCanvas.drawPath(path, mPaint)
    }

    /**
     * 绘制菱形
     */
    private fun drawDiamond(mPoint: PointF, size: Float = 10f) {
        val path = Path().apply {
            moveTo(mPoint.x, mPoint.y - size) // 上
            lineTo(mPoint.x + size, mPoint.y) // 右
            lineTo(mPoint.x, mPoint.y + size) // 下
            lineTo(mPoint.x - size, mPoint.y) // 左
            close()
        }
        mCanvas.drawPath(path, mPaint)
    }

    /**
     * 绘制矩形
     */
    private fun drawRect(mPoint: PointF, width: Float = 20f, height: Float = 20f) {
        val left = mPoint.x - width / 2
        val top = mPoint.y - height / 2
        val right = mPoint.x + width / 2
        val bottom = mPoint.y + height / 2
        mCanvas.drawRect(left, top, right, bottom, mPaint)
    }

    /**
     * 绘制矩形
     */
    private fun drawRect(pStart: PointF, mEnd: PointF) {
        mCanvas.drawRect(
            pStart.x, pStart.y, mEnd.x, mEnd.y, mPaint
        )
    }

}
